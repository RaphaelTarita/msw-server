package msw.server.core.versions

import com.github.ajalt.mordant.animation.progressAnimation
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.JsonPathException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.request.get
import java.net.URL
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import msw.server.core.common.ExpirableCache
import msw.server.core.common.GlobalInjections
import msw.server.core.common.nullIfError
import msw.server.core.common.readyMsg
import msw.server.core.versions.model.VersionType
import msw.server.core.versions.model.Versions

context(GlobalInjections)
class ManifestCreator(
    private val initUrl: URL = URL("https://launchermeta.mojang.com/mc/game/version_manifest.json"),
    cacheRefresh: Long = 12 * 60 * 60 * 1000
) {
    companion object {
        private const val SPAM_PROTECTION_MILLIS = 100L
        private val updateRate = (SPAM_PROTECTION_MILLIS / 10)..SPAM_PROTECTION_MILLIS
        private var spamProtectionTimestamp = System.currentTimeMillis()

        private val dlUrlPath = JsonPath.compile("$.downloads.server.url")
        private val sizePath = JsonPath.compile("$.downloads.server.size")
        private val sha1Path = JsonPath.compile("$.downloads.server.sha1")

        private const val errorMsg = "Could not create requested download manifest:"

        private fun stringify(range: ClosedRange<OffsetDateTime>) = "${range.start} - ${range.endInclusive}"

        private fun parse(contents: String): Versions {
            return Json.decodeFromString(contents)
        }
    }

    private val client = HttpClient(CIO) {
        engine {
            endpoint {
                socketTimeout = 20_000
                connectTimeout = 20_000
            }
        }
    }

    private val root = bootstrap()
    private val cache = ExpirableCache<URL, DownloadManifest?>(cacheRefresh)

    init {
        terminal.readyMsg("Manifest Service")
    }

    private fun bootstrap(): Versions {
        terminal.info("fetching versions root document")
        val rootDoc = runBlocking {
            parse(readContent(initUrl))
        }
        terminal.info("root versions document fetched, ${rootDoc.versions.size} versions found")
        return rootDoc
    }

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

    private suspend fun readContent(url: URL): String {
        while (System.currentTimeMillis() - spamProtectionTimestamp < SPAM_PROTECTION_MILLIS) {
            delay(updateRate.random())
        }
        spamProtectionTimestamp = System.currentTimeMillis()
        return client.get(url).body()
    }

    private suspend fun findUrlInDocument(
        documentURL: URL,
        id: String,
        sha1: String,
        type: VersionType,
        time: OffsetDateTime,
        releaseTime: OffsetDateTime
    ): DownloadManifest? {
        val res = cache.runOrGet(documentURL) {
            nullIfError<JsonPathException, DownloadManifest> {
                val ctx = JsonPath.parse(readContent(it))
                DownloadManifest(
                    id,
                    withContext(Dispatchers.IO) { URL(ctx.read(dlUrlPath)) },
                    type,
                    time,
                    releaseTime,
                    ctx.read(sizePath),
                    ctx.read(sha1Path)
                )
            }
        }

        return if (sha1.isBlank() || res?.sha1 == sha1) res else null
    }

    private fun internalCreate(
        parsed: Versions,
        id: String = "",
        type: VersionType = VersionType.ALL,
        sha1: String = "",
        timeRange: ClosedRange<OffsetDateTime> = OffsetDateTime.MIN..OffsetDateTime.MAX,
        releaseTimeRange: ClosedRange<OffsetDateTime> = OffsetDateTime.MIN..OffsetDateTime.MAX,
    ): List<DownloadManifest> {
        val filtered = parsed.versions
            .asSequence()
            .filter { id.isBlank() || it.id == id }
            .filter { type == VersionType.ALL || type == it.type }
            .filter { it.time in timeRange }
            .filter { it.releaseTime in releaseTimeRange }
            .toList()

        val anim = terminal.progressAnimation {
            text("fetching ${filtered.size} manifests (${cache.validCount()} in cache)")
            percentage()
            progressBar(
                pendingChar = "-",
                separatorChar = ">",
                completeChar = "-"
            )
            completed()
            timeRemaining()
        }
        anim.updateTotal(filtered.size.toLong())
        anim.start()

        val manifests = filtered.map {
            netScope.async {
                findUrlInDocument(
                    it.url,
                    it.id,
                    sha1,
                    it.type,
                    it.time,
                    it.releaseTime
                ).also { anim.advance() }
            }
        }

        return runBlocking {
            manifests.mapNotNull { it.await() }.also {
                anim.update()
                anim.stop()
                anim.clear()
            }
        }
    }

    fun createManifests(
        id: String = "",
        type: VersionType = VersionType.ALL,
        sha1: String = "",
        timeRange: ClosedRange<OffsetDateTime> = OffsetDateTime.MIN..OffsetDateTime.MAX,
        releaseTimeRange: ClosedRange<OffsetDateTime> = OffsetDateTime.MIN..OffsetDateTime.MAX
    ): List<DownloadManifest> = internalCreate(
        root, id, type, sha1, timeRange, releaseTimeRange
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
        return internalCreate(root, root.latest.release, VersionType.RELEASE).single()
    }

    fun latestSnapshot(): DownloadManifest {
        return internalCreate(root, root.latest.snapshot, VersionType.SNAPSHOT).single()
    }

    fun rootDocument() = root
}