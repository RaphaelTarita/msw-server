package com.example.core.model.adapters

import com.example.core.model.props.WorldType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object WorldTypeSerializer : KSerializer<WorldType> {
    override val descriptor = PrimitiveSerialDescriptor("WorldType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: WorldType) {
        encoder.encodeString(value.typeName)
    }

    override fun deserialize(decoder: Decoder): WorldType {
        val type = decoder.decodeString()
        for (t in WorldType.values()) {
            if (t.typeName.equals(type, true)) {
                return t
            }
        }
        throw IllegalArgumentException("World type $type unknown")
    }
}