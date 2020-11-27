package com.example.core.versions.model.adapters

import com.example.core.versions.model.VersionType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object VersionTypeSerializer : KSerializer<VersionType> {
    override val descriptor = PrimitiveSerialDescriptor("VersionType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: VersionType) {
        encoder.encodeString(value.typeName)
    }

    override fun deserialize(decoder: Decoder): VersionType {
        val type = decoder.decodeString()
        for (t in VersionType.values()) {
            if (t.typeName.equals(type, true)) {
                return t
            }
        }
        throw IllegalArgumentException("Version type $type unknown")
    }
}