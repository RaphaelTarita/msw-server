package msw.server.core.versions

import msw.server.core.versions.model.VersionType
import java.net.URL
import java.time.OffsetDateTime

data class DownloadManifest(
    val versionID: String,
    val downloadUrl: URL,
    val type: VersionType,
    val time: OffsetDateTime,
    val releaseTime: OffsetDateTime,
    val size: Long,
    val sha1: String
)