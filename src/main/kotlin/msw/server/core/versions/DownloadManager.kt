package msw.server.core.versions

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import msw.server.core.common.coerceToInt
import msw.server.core.common.toHexString
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import kotlin.math.min

class DownloadManager(
    private val manifest: DownloadManifest,
    var target: Path,
    private val bufferSize: Int = 4096
) {
    companion object {
        private fun progress(target: Path): Long {
            return Files.size(target)
        }
    }

    private val client = HttpClient(Apache)
    private val md = MessageDigest.getInstance("SHA-1")

    private fun checkSize() {
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

    private fun checkEndSize(target: Path) {
        val actualSize = Files.size(target)
        if (actualSize != manifest.size) {
            throw DownloadException(
                manifest.downloadUrl,
                "Expected ${manifest.size} bytes, but downloaded file has $actualSize bytes"
            )
        }
    }

    private fun checkHash(path: Path) {
        val sha1 = md.digest().toHexString()
        if (sha1 != manifest.sha1) {
            Files.delete(path)
            throw DownloadException(manifest.downloadUrl, "Expected SHA-1 '${manifest.sha1}', but got '$sha1'")
        }
    }

    private suspend fun initDownload(): ByteReadChannel {
        return client.get(manifest.downloadUrl)
    }

    private suspend fun resumeDownload(progress: Long): ByteReadChannel {
        val channel = initDownload()
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

    fun download() {
        checkSize()
        val currentTarget = target
        val writer = Files.newOutputStream(currentTarget, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        val progress = progress(currentTarget)
        if (progress >= manifest.size) return
        val channel = runBlocking {
            if (progress > 0L) resumeDownload(progress) else initDownload()
        }

        var hashJob: Job? = null
        val buf = ByteArray(bufferSize)
        do {
            val available = runBlocking { channel.readAvailable(buf, 0, bufferSize) }
            if (available > 0) {
                val significant = buf.sliceArray(0 until available)
                runBlocking { hashJob?.join() }
                hashJob = GlobalScope.launch { md.update(significant) }
                writer.write(significant)
            }
        } while (available >= 0)
        writer.close()
        checkHash(currentTarget)
        checkEndSize(currentTarget)
    }
}