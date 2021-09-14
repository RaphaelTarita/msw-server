package msw.server.core.versions

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.statement.HttpResponse
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import msw.server.core.common.coerceToInt
import msw.server.core.common.toHexString

class DownloadManager(
    private val scope: CoroutineScope,
    private val bufferSize: Int = 4096
) {
    companion object {
        private fun progress(target: Path): Long {
            return Files.size(target)
        }
    }

    private val client = HttpClient(Apache)

    private fun List<suspend (Long, Long) -> Unit>.notifyProgress(current: Long, total: Long) {
        for (listener in this) {
            scope.launch { listener(current, total) }
        }
    }

    private fun checkSize(manifest: DownloadManifest) {
        val actualSize = runBlocking {
            client.head<HttpResponse>(manifest.downloadUrl).contentLength()
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

    private suspend fun initDownload(manifest: DownloadManifest): ByteReadChannel {
        return client.get(manifest.downloadUrl)
    }

    private suspend fun resumeDownload(manifest: DownloadManifest, progress: Long, md: MessageDigest): ByteReadChannel {
        val channel = initDownload(manifest)
        val hashBuf = ByteArray(bufferSize)
        var remaining = progress
        do {
            val available = channel.readAvailable(hashBuf, 0, min(bufferSize, remaining.coerceToInt()))
            if (available > 0) {
                md.update(hashBuf.sliceArray(0 until available))
                remaining -= available
            }
        } while (available >= 0 && remaining > 0)
        return channel
    }

    fun download(
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
        val channel = runBlocking {
            if (progress > 0L) resumeDownload(manifest, progress, md) else initDownload(manifest)
        }
        listeners.notifyProgress(progress, manifest.size)
        val internalUpdateRate = (manifest.size / bufferSize) / updateRate

        var count = 0L
        var hashJob: Job? = null
        val buf = ByteArray(bufferSize)
        do {
            val available = runBlocking { channel.readAvailable(buf, 0, bufferSize) }
            if (available > 0) {
                val significant = buf.sliceArray(0 until available)
                runBlocking { hashJob?.join() }
                hashJob = scope.launch { md.update(significant) }
                writer.write(significant)
                progress += available
                if (count++ % internalUpdateRate == 0L) {
                    listeners.notifyProgress(progress, manifest.size)
                }
            }
        } while (available >= 0)
        writer.close()
        checkHash(md, manifest, target)
        checkEndSize(manifest, target)
        listeners.notifyProgress(manifest.size, manifest.size)
    }
}