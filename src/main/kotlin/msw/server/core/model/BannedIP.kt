package msw.server.core.model

import java.net.InetAddress
import java.time.OffsetDateTime
import kotlinx.serialization.Serializable
import msw.server.core.model.adapters.DateSerializer
import msw.server.core.model.adapters.IPSerializer

@Serializable
data class BannedIP(
    @Serializable(with = IPSerializer::class)
    val ip: InetAddress,
    @Serializable(with = DateSerializer::class)
    override val created: OffsetDateTime,
    override val source: String,
    override val expires: String,
    override val reason: String
) : Banned