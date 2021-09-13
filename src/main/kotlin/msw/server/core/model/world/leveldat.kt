package msw.server.core.model.world

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.benwoodworth.knbt.ExperimentalNbtApi
import net.benwoodworth.knbt.NbtRoot

@OptIn(ExperimentalNbtApi::class)
@NbtRoot(name = "")
@Serializable
data class LevelRoot(
    @SerialName("Data")
    val data: LevelData
)


@SerialName("Data")
@Serializable
data class LevelData(
    @SerialName("LevelName")
    val levelName: String
)