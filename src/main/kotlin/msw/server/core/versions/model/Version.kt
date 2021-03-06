package msw.server.core.versions.model

import kotlinx.serialization.Serializable
import msw.server.core.versions.model.adapters.DateSerializer
import msw.server.core.versions.model.adapters.URLSerializer
import msw.server.core.versions.model.adapters.VersionTypeSerializer
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