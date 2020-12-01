package msw.server.core.versions

import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.JsonPathException
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import msw.server.core.common.nullIfError
import msw.server.core.versions.model.VersionType
import msw.server.core.versions.model.Versions
import java.net.URL
import java.time.OffsetDateTime

class ManifestCreator(private val initUrl: URL = URL("https://launchermeta.mojang.com/mc/game/version_manifest.json")) {
    companion object {
        private val dlUrlPath = JsonPath.compile("$.downloads.server.url")
        private val sizePath = JsonPath.compile("$.downloads.server.size")
        private val sha1Path = JsonPath.compile("$.downloads.server.sha1")
        private const val errorMsg = "Could not create requested download manifest:"

        private fun stringify(range: ClosedRange<OffsetDateTime>) = "${range.start} - ${range.endInclusive}"

        private fun parse(contents: Deferred<String>): Versions {
            return Json.decodeFromString(runBlocking { contents.await() })
        }
    }

    private val client = HttpClient(Apache) {
        engine {
            socketTimeout = 20_000
            connectTimeout = 20_000
        }
    }
    private val contents = GlobalScope.async { readContent() }

    private fun constructErrorMsg(
        id: String,
        type: VersionType,
        sha1: String,
        timeRange: ClosedRange<OffsetDateTime>,
        releaseTimeRange: ClosedRange<OffsetDateTime>
    ): String {
        val res = mutableListOf<String>()
        if (id.isNotBlank()) {
            res.add("id \'$id\'")
        }
        if (type != VersionType.ALL) {
            res.add("version type \'${type.typeName}\'")
        }
        if (sha1.isNotBlank()) {
            res.add("SHA-1 \'$sha1\'")
        }
        if (timeRange.start != OffsetDateTime.MIN && timeRange.endInclusive != OffsetDateTime.MAX) {
            res.add("time range \'${stringify(timeRange)}\'")
        }
        if (releaseTimeRange.start != OffsetDateTime.MIN && releaseTimeRange.endInclusive != OffsetDateTime.MAX) {
            res.add("release time range \'${stringify(releaseTimeRange)}\'")
        }

        return if (res.isEmpty()) {
            "(no search parameters)"
        } else {
            res.joinToString(", ")
        }
    }

    private suspend fun readContent(url: URL = initUrl): String {
        return client.get(url)
    }

    private fun findUrlInDocument(
        document: String,
        id: String,
        type: VersionType,
        sha1: String,
        time: OffsetDateTime,
        releaseTime: OffsetDateTime
    ): DownloadManifest? {
        val ctx = JsonPath.parse(document)
        return nullIfError<JsonPathException, DownloadManifest?> {
            val manifest = DownloadManifest(
                id,
                URL(ctx.read(dlUrlPath)),
                type,
                time,
                releaseTime,
                ctx.read(sizePath),
                ctx.read(sha1Path)
            )
            return if (sha1.isBlank() || manifest.sha1 == sha1) manifest else null
        }
    }

    private fun internalCreate(
        parsed: Versions,
        id: String = "",
        type: VersionType = VersionType.ALL,
        sha1: String = "",
        timeRange: ClosedRange<OffsetDateTime> = OffsetDateTime.MIN..OffsetDateTime.MAX,
        releaseTimeRange: ClosedRange<OffsetDateTime> = OffsetDateTime.MIN..OffsetDateTime.MAX
    ): List<DownloadManifest> {
        val manifests = parsed.versions
            .asSequence()
            .filter { type == VersionType.ALL || type == it.type }
            .filter { it.time in timeRange }
            .filter { it.releaseTime in releaseTimeRange }
            .filter { id.isBlank() || it.id == id }
            .toList()
            .map {
                GlobalScope.async {
                    findUrlInDocument(
                        readContent(it.url),
                        it.id,
                        it.type,
                        sha1,
                        it.time,
                        it.releaseTime
                    )
                }
            }

        return runBlocking { manifests.mapNotNull { it.await() } }
    }

    fun createManifests(
        id: String = "",
        type: VersionType = VersionType.ALL,
        sha1: String = "",
        timeRange: ClosedRange<OffsetDateTime> = OffsetDateTime.MIN..OffsetDateTime.MAX,
        releaseTimeRange: ClosedRange<OffsetDateTime> = OffsetDateTime.MIN..OffsetDateTime.MAX
    ): List<DownloadManifest> = internalCreate(
        parse(contents),
        id, type, sha1, timeRange, releaseTimeRange
    )

    fun createManifest(
        id: String = "",
        type: VersionType = VersionType.ALL,
        sha1: String = "",
        timeRange: ClosedRange<OffsetDateTime> = OffsetDateTime.MIN..OffsetDateTime.MAX,
        releaseTimeRange: ClosedRange<OffsetDateTime> = OffsetDateTime.MIN..OffsetDateTime.MAX
    ): DownloadManifest {
        val manifests = createManifests(id, type, sha1, timeRange, releaseTimeRange)
        return when {
            manifests.isEmpty() -> throw DownloadException(
                initUrl,
                "$errorMsg No URL found for: ${constructErrorMsg(id, type, sha1, timeRange, releaseTimeRange)}"
            )
            manifests.size > 1 -> throw DownloadException(
                initUrl,
                "$errorMsg Multiple URLs found for: ${constructErrorMsg(id, type, sha1, timeRange, releaseTimeRange)}"
            )
            else -> manifests.single()
        }
    }

    fun latestRelease(): DownloadManifest {
        val parsed = parse(contents)
        return internalCreate(parsed, parsed.latest.release, VersionType.RELEASE).single()
    }

    fun latestSnapshot(): DownloadManifest {
        val parsed = parse(contents)
        return internalCreate(parsed, parsed.latest.snapshot, VersionType.SNAPSHOT).single()
    }
}