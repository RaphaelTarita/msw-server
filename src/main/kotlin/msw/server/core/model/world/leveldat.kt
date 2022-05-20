package msw.server.core.model.world

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("")
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