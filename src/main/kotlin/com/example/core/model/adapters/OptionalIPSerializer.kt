package com.example.core.model.adapters

import com.google.common.net.InetAddresses
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.InetAddress

object OptionalIPSerializer : KSerializer<InetAddress?> {
    override val descriptor = PrimitiveSerialDescriptor("java.net.InetAddress?", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: InetAddress?) {
        encoder.encodeString(value?.toString()?.split('/')?.get(1) ?: "")
    }

    @Suppress("UnstableApiUsage")
    override fun deserialize(decoder: Decoder): InetAddress? {
        val str = decoder.decodeString()
        if (str.isBlank()) return null
        return InetAddresses.forString(str)
    }
}