package com.example.core.model.adapters

import com.example.core.model.OPLevel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object LevelSerializer : KSerializer<OPLevel> {
    override val descriptor = PrimitiveSerialDescriptor("OPLevel", PrimitiveKind.INT)

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