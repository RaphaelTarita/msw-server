package com.example.core.versions.model

import kotlinx.serialization.Serializable

@Serializable
data class Versions(
    val latest: LatestVersion,
    val versions: List<Version>
)