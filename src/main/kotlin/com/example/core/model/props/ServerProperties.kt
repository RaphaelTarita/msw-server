package com.example.core.model.props

import com.example.core.model.OPLevel
import com.example.core.model.adapters.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.InetAddress

// https://minecraft-de.gamepedia.com/Server.properties#Server.properties
// https://minecraft.gamepedia.com/Server.properties
@Serializable
data class ServerProperties(
    @SerialName("view-distance")
    val viewDistance: Int = 10,
    @SerialName("max-build-height")
    val maxBuildHeight: Int = 256,
    @SerialName("server-ip")
    @Serializable(with = OptionalIPSerializer::class)
    val serverIp: InetAddress? = null,
    @SerialName("level-seed")
    val levelSeed: String = "",
    @Serializable(with = GameModeSerializer::class)
    val gamemode: GameMode = GameMode.SURVIVAL,
    @SerialName("server-port")
    val serverPort: Int = 25565,
    @SerialName("enable-command-block")
    val enableCommandBlock: Boolean = false,
    @SerialName("allow-nether")
    val allowNether: Boolean = true,
    @SerialName("enable-rcon")
    val enableRcon: Boolean = false,
    @SerialName("op-permission-level")
    @Serializable(with = LevelSerializer::class)
    val opPermissionLevel: OPLevel = OPLevel.OWNER,
    @SerialName("enable-query")
    val enableQuery: Boolean = false,
    @SerialName("prevent-proxy-connections")
    val preventProxyConnections: Boolean = false,
    @SerialName("generator-settings")
    val generatorSettings: String = "",
    @SerialName("resource-pack")
    val resourcePack: String = "",
    @SerialName("player-idle-timeout")
    val playerIdleTimeout: Int = 0,
    @SerialName("level-name")
    val levelName: String = "world",
    val motd: String = "A Minecraft Server",
    @SerialName("force-gamemode")
    val forceGamemode: Boolean = false,
    val hardcore: Boolean = false,
    @SerialName("white-list")
    val whitelist: Boolean = false,
    @SerialName("broadcast-console-to-ops")
    val broadcastConsoleToOPs: Boolean = true,
    val pvp: Boolean = true,
    @SerialName("spawn-npcs")
    val spawnNPCs: Boolean = true,
    @SerialName("generate-structures")
    val generateStructures: Boolean = true,
    @SerialName("spawn-animals")
    val spawnAnimals: Boolean = true,
    @SerialName("snooper-enabled")
    val snooperEnabled: Boolean = true,
    @Serializable(with = GameDifficultySerializer::class)
    val difficulty: GameDifficulty = GameDifficulty.EASY,
    @SerialName("network-compression-threshold")
    val networkCompressionThreshold: Int = 256,
    @SerialName("level-type")
    @Serializable(with = WorldTypeSerializer::class)
    val worldType: WorldType = WorldType.DEFAULT,
    @SerialName("spawn-monsters")
    val spawnMonsters: Boolean = true,
    @SerialName("max-tick-time")
    val maxTickTime: Long = 60000,
    @SerialName("max-players")
    val maxPlayers: Int = 20,
    @SerialName("enforce-whitelist")
    val enforceWhitelist: Boolean = false,
    @SerialName("resource-pack-sha1")
    val resourcePackSHA1: String = "",
    @SerialName("online-mode")
    val onlineMode: Boolean = true,
    @SerialName("allow-flight")
    val allowFlight: Boolean = true,
    @SerialName("max-world-size")
    val maxWorldSize: Int = 29999984,
    @SerialName("function-permission-level")
    val functionPermissionLevel: OPLevel = OPLevel.GAME_MASTER,
    @SerialName("rate-limit")
    val rateLimit: Int = 0,
    val query: Query = Query(),
    val rcon: RCon = RCon(),
    @SerialName("spawn-protection")
    val spawnProtection: Int = 16,
    @SerialName("broadcast-rcon-to-ops")
    val broadcastRConToOPs: Boolean = true,
    @SerialName("enable-jmx-monitoring")
    val enableJMXMonitoring: Boolean = false,
    @SerialName("sync-chunk-writes")
    val syncChunkWrites: Boolean = true,
    @SerialName("enable-status")
    val enableStatus: Boolean = true,
    @SerialName("entity-broadcast-range-percentage")
    val entityBroadcastRangePercentage: Int = 100,
    @SerialName("require-resource-pack")
    val requireResourcePack: Boolean = false,
    @SerialName("use-native-transport")
    val useNativeTransport: Boolean = true,
) {
    init {
        require(viewDistance in 3..15) {
            "property view-distance has to be between 3 and 15"
        }
        require(maxBuildHeight >= 0) {
            "property max-build-height cannot be negative"
        }
        require(serverPort in 0..65534) {
            "property server-port has to be between 0 and 65534"
        }
        require(playerIdleTimeout >= 0) {
            "property player-idle-timeout cannot be negative"
        }
        require(networkCompressionThreshold >= -1) {
            "property network-compression-threshold must be positive (or -1)"
        }
        require(maxTickTime >= -1) {
            "property max-tick-time must be positive (or -1)"
        }
        require(maxPlayers >= 1) {
            "property max-players has to be greater than 1"
        }
        require("[0-9a-f]*".toRegex().matches(resourcePackSHA1)) {
            "property resource-pack-sha1 has to be a valid hexadecimal number with lowercase letters"
        }
        require(maxWorldSize in 1..29999984) {
            "property max-world-size has to be between 1 and 29999984"
        }
        require(functionPermissionLevel.num >= OPLevel.GAME_MASTER.num) {
            "property function-permission-level has to be 2 or greater (game master or higher)"
        }
        require(rateLimit >= 0) {
            "property rate-limit cannot be negative"
        }
        require(query.port in 0..65534) {
            "property query.port has to be between 0 and 65534"
        }
        require(rcon.port in 0..65534) {
            "property rcon.port has to be between 0 and 65534"
        }
        require(spawnProtection >= 0) {
            "property spawn-protection cannot be negative"
        }
        require(entityBroadcastRangePercentage in 0..500) {
            "property enable-broadcast-range-percentage has to be between 0 and 500"
        }
    }
}