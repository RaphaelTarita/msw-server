package msw.server.core.model

import kotlinx.serialization.Serializable
import msw.server.core.model.adapters.DateSerializer
import msw.server.core.model.adapters.LevelSerializer
import msw.server.core.model.adapters.UUIDSerializer
import java.time.OffsetDateTime
import java.util.*

@Serializable
sealed class PlayerSignatureBase {
    abstract val uuid: UUID
    abstract val name: String
}

@Serializable
data class PlayerSignature(
    @Serializable(with = UUIDSerializer::class)
    override val uuid: UUID,
    override val name: String
) : PlayerSignatureBase()

@Serializable
data class ExpirablePlayerSignature(
    @Serializable(with = UUIDSerializer::class)
    override val uuid: UUID,
    override val name: String,
    @Serializable(with = DateSerializer::class)
    val expiresOn: OffsetDateTime
) : PlayerSignatureBase()

// https://minecraft-de.gamepedia.com/Server.properties#ops.json
@Serializable
data class OP(
    @Serializable(with = UUIDSerializer::class)
    override val uuid: UUID,
    override val name: String,
    @Serializable(with = LevelSerializer::class)
    val level: OPLevel,
    val bypassesPlayerLimit: Boolean
) : PlayerSignatureBase()

@Serializable
data class BannedPlayer(
    @Serializable(with = UUIDSerializer::class)
    override val uuid: UUID,
    override val name: String,
    @Serializable(with = DateSerializer::class)
    override val created: OffsetDateTime,
    override val source: String,
    override val expires: String,
    override val reason: String
) : PlayerSignatureBase(), Banned

enum class OPLevel(val num: Int) {
    MODERATOR(1),
    GAME_MASTER(2),
    ADMINISTRATOR(3),
    OWNER(4)
}

