package com.example.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerStats(
    @SerialName("DataVersion")
    val dataVersion: Int,
    val stats: Stats
)

@Serializable
data class Stats(
    @SerialName("minecraft:broken")
    val broken: Map<String, Long> = emptyMap(),
    @SerialName("minecraft:crafted")
    val crafted: Map<String, Long> = emptyMap(),
    @SerialName("minecraft:custom")
    val custom: Map<String, Long> = emptyMap(),
    @SerialName("minecraft:dropped")
    val dropped: Map<String, Long> = emptyMap(),
    @SerialName("minecraft:killed")
    val killed: Map<String, Long> = emptyMap(),
    @SerialName("minecraft:killed_by")
    val killedBy: Map<String, Long> = emptyMap(),
    @SerialName("minecraft:mined")
    val mined: Map<String, Long> = emptyMap(),
    @SerialName("minecraft:picked_up")
    val pickedUp: Map<String, Long> = emptyMap(),
    @SerialName("minecraft:used")
    val used: Map<String, Long> = emptyMap()
)