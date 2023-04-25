package msw.server.core.versions

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.head
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import msw.server.core.common.GlobalInjections
import msw.server.core.common.util.coerceToInt
import msw.server.core.common.util.readyMsg
import msw.server.core.common.util.toHexString

context(GlobalInjections)
class DownloadManager(
    private val bufferSize: Int = 4096
) {
    companion object {
        private fun progress(target: Path): Long {
            return Files.size(target)
        }
    }

    private val client = HttpClient(CIO)

    init {
        terminal.readyMsg("Download Service")
    }

    private suspend fun List<suspend (Long, Long) -> Unit>.notifyProgress(current: Long, total: Long) {
        for (listener in this) {
            listener(current, total)
        }
    }

    private fun checkSize(manifest: DownloadManifest) {
        val actualSize = runBlocking {
            client.head(manifest.downloadUrl).contentLength()
        }

        if (actualSize != manifest.size) {
            throw DownloadException(
                manifest.downloadUrl,
                "Expected ${manifest.size} bytes, but HEAD returned $actualSize"
            )
        }
    }

    private fun checkEndSize(manifest: DownloadManifest, target: Path) {
        val actualSize = Files.size(target)
        if (actualSize != manifest.size) {
            throw DownloadException(
                manifest.downloadUrl,
                "Expected ${manifest.size} bytes, but downloaded file has $actualSize bytes"
            )
        }
    }

    private fun checkHash(md: MessageDigest, manifest: DownloadManifest, path: Path) {
        val sha1 = md.digest().toHexString()
        if (sha1 != manifest.sha1) {
            Files.delete(path)
            throw DownloadException(
                manifest.downloadUrl,
                "Expected SHA-1 '${manifest.sha1}', but got '$sha1'"
            )
        }
    }

    private suspend fun prepare(manifest: DownloadManifest): HttpStatement {
        return client.prepareGet(manifest.downloadUrl)
    }

    private suspend fun processPrevious(res: HttpResponse, progress: Long, md: MessageDigest): ByteReadChannel {
        val hashBuf = ByteArray(bufferSize)
        var remaining = progress
        val channel = res.bodyAsChannel()
        do {
            val available = channel.readAvailable(hashBuf, 0, min(bufferSize, remaining.coerceToInt()))
            if (available > 0) {
                md.update(hashBuf.sliceArray(0 until available))
                remaining -= available
            }
        } while (available >= 0 && remaining > 0)
        return channel
    }

    suspend fun download(
        manifest: DownloadManifest,
        target: Path,
        listeners: List<suspend (current: Long, total: Long) -> Unit> = emptyList(),
        updateRate: Long = 100
    ) {
        checkSize(manifest)
        val writer = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        var progress = progress(target)
        if (progress >= manifest.size) return
        val md = MessageDigest.getInstance("SHA-1")
        val statement = prepare(manifest)

        listeners.notifyProgress(progress, manifest.size)
        val internalUpdateRate = max(1, (manifest.size / bufferSize) / updateRate)

        var count = 0L
        var hashJob: Job? = null
        val buf = ByteArray(bufferSize)

        statement.execute { res ->
            val channel = if (progress > 0) processPrevious(res, progress, md) else res.bodyAsChannel()
            do {
                val available = channel.readAvailable(buf, 0, bufferSize)
                if (available > 0) {
                    val significant = buf.sliceArray(0 until available)
                    hashJob?.join()
                    hashJob = netScope.launch { md.update(significant) }
                    writer.write(significant)
                    progress += available
                    if (count++ % internalUpdateRate == 0L) {
                        listeners.notifyProgress(progress, manifest.size)
                    }
                }
            } while (available >= 0)
        }

        writer.close()
        checkHash(md, manifest, target)
        checkEndSize(manifest, target)
        listeners.notifyProgress(manifest.size, manifest.size)
    }
}