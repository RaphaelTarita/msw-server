package msw.server.core.common

import com.google.common.net.InetAddresses
import java.net.InetAddress
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import msw.server.core.model.OPLevel
import msw.server.core.model.adapters.DateSerializer
import msw.server.core.model.adapters.IPSerializer
import msw.server.core.model.adapters.LevelSerializer
import msw.server.core.model.adapters.UUIDSerializer

interface ModelMarker

@Serializable
data class Model01(
    val s: String = "Hello, World!",
    val i: Int = 42,
    val b: Boolean = false
) : ModelMarker

@Suppress("UnstableApiUsage")
@Serializable
data class Model02(
    @Serializable(with = DateSerializer::class)
    val dateTime: OffsetDateTime = OffsetDateTime.now()
        .withNano(0), // com.example.core.model.adapters.DateSerializer.kt
    @Serializable(with = IPSerializer::class)
    val ip: InetAddress = InetAddresses.forString("127.0.0.1"), // com.example.core.model.adapters.IPSerializer.kt
    @Serializable(with = LevelSerializer::class)
    val level: OPLevel = OPLevel.MODERATOR, // com.example.core.model.adapters.LevelSerializer.kt
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID = UUID.randomUUID() // com.example.core.model.adapters.UUIDSerializer.kt
) : ModelMarker

@Serializable
data class Model03(
    val s: String = "Hello, World!",
    val i: Int = 42,
    val nested1: Model03X01 = Model03X01(),
    val l: Long = 999999999999999999L,
    val nested2: Model03X02 = Model03X02()
) : ModelMarker

@Serializable
data class Model03X01(
    val stringField: String = "stringField",
    val doubleField: Double = 3.1415,
    val byteField: Byte = 2
) : ModelMarker

@Serializable
data class Model03X02(
    @Serializable(with = DateSerializer::class)
    val dateTime: OffsetDateTime = OffsetDateTime.now().withNano(0).minusDays(1),
    val int: Int = 100
) : ModelMarker

@Serializable
data class Model04(
    val list: List<Int> = listOf(1, 1, 2, 3, 5, 8, 13, 21, 34),
    val map: Map<String, Int> = mapOf("one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5),
    val set: Set<String> = setOf("firstElem", "secondElem", "thirdElem", "fourthElem", "fifthElem"),
    val string: String = "Hello!"
) : ModelMarker

@Serializable
data class Model05(
    val path1: Model05X01 = Model05X01(),
    val path2: Model05X02 = Model05X02(),
    val path3: Model05X03 = Model05X03()
) : ModelMarker

@Serializable
data class Model05X01(
    val path1: Model05X01X01 = Model05X01X01(),
    val path2: Model05X01X02 = Model05X01X02(),
    val endpoint1: String = "Model05 -> path 1 -> endpoint 1"
) : ModelMarker

@Serializable
data class Model05X02(
    val path1: Model05X02X01 = Model05X02X01(),
    val path2: Model05X02X02 = Model05X02X02()
) : ModelMarker

@Serializable
data class Model05X03(
    val path1: Model05X03X01 = Model05X03X01(),
    val path2: Model05X03X02 = Model05X03X02(),
    val path3: Model05X03X03 = Model05X03X03(),
    val path4: Model05X03X04 = Model05X03X04(),
    val endpoint1: String = "Model05 -> path 3 -> endpoint 1",
    val endpoint2: String = "Model05 -> path 3 -> endpoint 2"
) : ModelMarker

@Serializable
data class Model05X01X01(
    val endpoint1: String = "Model05 -> path 1 -> path 1 -> endpoint 1"
) : ModelMarker

@Serializable
data class Model05X01X02(
    val endpoint1: String = "Model05 -> path 1 -> path 2 -> endpoint 1",
    val endpoint2: String = "Model05 -> path 1 -> path 2 -> endpoint 2"
) : ModelMarker

@Serializable
data class Model05X02X01(
    val endpoint1: String = "Model05 -> path 2 -> path 1 -> endpoint 1",
    val endpoint2: String = "Model05 -> path 2 -> path 1 -> endpoint 2",
    val endpoint3: String = "Model05 -> path 2 -> path 1 -> endpoint 3"
) : ModelMarker

@Serializable
data class Model05X02X02(
    val path1: Model05X02X02X01 = Model05X02X02X01(),
    val path2: Model05X02X02X02 = Model05X02X02X02(),
    val endpoint1: String = "Model05 -> path 2 -> path 2 -> endpoint 1"
) : ModelMarker

@Serializable
data class Model05X03X01(
    val endpoint1: String = "Model05 -> path 3 -> path 1 -> endpoint 1"
) : ModelMarker

@Serializable
data class Model05X03X02(
    val endpoint1: String = "Model05 -> path 3 -> path 2 -> endpoint 1",
) : ModelMarker

@Serializable
data class Model05X03X03(
    val endpoint1: String = "Model05 -> path 3 -> path 3 -> endpoint 1",
    val endpoint2: String = "Model05 -> path 3 -> path 3 -> endpoint 2"
) : ModelMarker

@Serializable
data class Model05X03X04(
    val endpoint1: String = "Model05 -> path 3 -> path 4 -> endpoint 1"
) : ModelMarker

@Serializable
data class Model05X02X02X01(
    val endpoint1: String = "Model05 -> path 2 -> path 2 -> path 1 -> endpoint 1",
    val endpoint2: String = "Model05 -> path 2 -> path 2 -> path 1 -> endpoint 2"
) : ModelMarker

@Serializable
data class Model05X02X02X02(
    val endpoint1: String = "Model05 -> path 2 -> path 2 -> path 2 -> endpoint 1"
) : ModelMarker

@Serializable
data class Model06(
    val byte: Byte = 1,
    val short: Short = 2,
    val int: Int = 3,
    val long: Long = 4L,
    val float: Float = 3.14f,
    val double: Double = 3.1415,
    val boolean: Boolean = true,
    val char: Char = 'x',
    val string: String = "Hello, World!"
) : ModelMarker

@Serializable
data class PropertiesEscapeModel(
    @SerialName("e=e:e e\\e#e!e")
    val test: Int = 42
) : ModelMarker