package com.example.core.model.adapters

import com.example.core.model.props.GameMode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object GameModeSerializer : KSerializer<GameMode> {
    override val descriptor = PrimitiveSerialDescriptor("GameMode", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: GameMode) {
        encoder.encodeString(value.modeName)
    }

    override fun deserialize(decoder: Decoder): GameMode {
        val mode = decoder.decodeString()
        val legacyNumber = mode.toIntOrNull()
        if (legacyNumber != null) {
            for (m in GameMode.values()) {
                if (m.id == legacyNumber) {
                    return m
                }
            }
            throw IllegalArgumentException("Game mode with legacy gamemode number $legacyNumber unknown")
        } else {
            for (m in GameMode.values()) {
                if (m.modeName == mode) {
                    return m
                }
            }
            throw IllegalArgumentException("Game mode $mode unknown")
        }
    }
}