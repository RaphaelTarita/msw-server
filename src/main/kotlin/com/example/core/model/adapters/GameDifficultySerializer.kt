package com.example.core.model.adapters

import com.example.core.model.props.GameDifficulty
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object GameDifficultySerializer : KSerializer<GameDifficulty> {
    override val descriptor = PrimitiveSerialDescriptor("GameDifficulty", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: GameDifficulty) {
        encoder.encodeString(value.difficultyName)
    }

    override fun deserialize(decoder: Decoder): GameDifficulty {
        val difficulty = decoder.decodeString()
        val legacyNumber = difficulty.toIntOrNull()
        if (legacyNumber != null) {
            for (d in GameDifficulty.values()) {
                if (d.id == legacyNumber) {
                    return d
                }
            }
            throw IllegalArgumentException("Game difficulty with legacy difficulty number $legacyNumber unknown")
        } else {
            for (d in GameDifficulty.values()) {
                if (d.difficultyName == difficulty) {
                    return d
                }
            }
            throw IllegalArgumentException("Game difficulty $difficulty unknown")
        }
    }
}