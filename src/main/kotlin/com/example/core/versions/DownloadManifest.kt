package com.example.core.versions

import com.example.core.versions.model.VersionType
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