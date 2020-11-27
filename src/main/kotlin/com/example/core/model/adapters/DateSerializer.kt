package com.example.core.model.adapters

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object DateSerializer : KSerializer<OffsetDateTime> {
    private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")
    override val descriptor = PrimitiveSerialDescriptor("java.time.OffsetDateTime#1", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: OffsetDateTime) {
        encoder.encodeString(value.format(format))
    }

    override fun deserialize(decoder: Decoder): OffsetDateTime {
        return OffsetDateTime.parse(decoder.decodeString(), format)
    }
}