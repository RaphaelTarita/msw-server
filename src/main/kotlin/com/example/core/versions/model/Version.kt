package com.example.core.versions.model

import com.example.core.versions.model.adapters.DateSerializer
import com.example.core.versions.model.adapters.URLSerializer
import com.example.core.versions.model.adapters.VersionTypeSerializer
import kotlinx.serialization.Serializable
import java.net.URL
import java.time.OffsetDateTime

@Serializable
data class Version(
    val id: String,
    @Serializable(with = VersionTypeSerializer::class)
    val type: VersionType,
    @Serializable(with = URLSerializer::class)
    val url: URL,
    @Serializable(with = DateSerializer::class)
    val time: OffsetDateTime,
    @Serializable(with = DateSerializer::class)
    val releaseTime: OffsetDateTime
)