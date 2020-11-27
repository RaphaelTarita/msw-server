package com.example.core.versions

import com.example.core.common.coerceToInt
import com.example.core.common.toHexString
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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import kotlin.math.min

class DownloadManager(
    private val bufferSize: Int = 4096
) {
    companion object {
        private fun progress(target: Path): Long {
            return Files.size(target)
        }
    }

    private val client = HttpClient(Apache)

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

    fun download(manifest: DownloadManifest, target: Path) {
        checkSize(manifest)
        val writer = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        val progress = progress(target)
        if (progress >= manifest.size) return
        val md = MessageDigest.getInstance("SHA-1")
        val channel = runBlocking {
            if (progress > 0L) resumeDownload(manifest, progress, md) else initDownload(manifest)
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
        checkHash(md, manifest, target)
        checkEndSize(manifest, target)
    }
}