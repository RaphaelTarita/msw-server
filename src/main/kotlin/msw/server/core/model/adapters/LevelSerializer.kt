package msw.server.core.model.adapters

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import msw.server.core.model.OPLevel

object LevelSerializer : KSerializer<OPLevel> {
    override val descriptor = PrimitiveSerialDescriptor("msw.server.core.model.OPLevel", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: OPLevel) {
        encoder.encodeInt(value.num)
    }

    override fun deserialize(decoder: Decoder): OPLevel {
        val level = decoder.decodeInt()
        for (l in OPLevel.values()) {
            if (l.num == level) {
                return l
            }
        }
        throw IllegalArgumentException("OP level $level unknown")
    }
}