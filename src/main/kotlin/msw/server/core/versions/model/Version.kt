package msw.server.core.versions.model

import java.net.URL
import java.time.OffsetDateTime
import kotlinx.serialization.Serializable
import msw.server.core.common.util.comparatorForNested
import msw.server.core.versions.model.adapters.DateSerializer
import msw.server.core.versions.model.adapters.URLSerializer
import msw.server.core.versions.model.adapters.VersionTypeSerializer

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
) : Comparable<Version> {
    companion object {
        private val comparator = comparatorForNested<Version, OffsetDateTime>(OffsetDateTime.timeLineOrder()) { it.releaseTime }
    }

    override fun compareTo(other: Version): Int {
        return comparator.compare(this, other)
    }
}