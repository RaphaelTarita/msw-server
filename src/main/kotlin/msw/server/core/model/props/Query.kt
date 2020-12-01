package msw.server.core.model.props

import kotlinx.serialization.Serializable

@Serializable
data class Query(
    val port: Int = 25565
)