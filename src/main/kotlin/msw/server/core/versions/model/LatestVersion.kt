package msw.server.core.versions.model

import kotlinx.serialization.Serializable

@Serializable
data class LatestVersion(
    val release: String,
    val snapshot: String
)