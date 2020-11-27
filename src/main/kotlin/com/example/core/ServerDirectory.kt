package com.example.core

import com.example.core.common.*
import com.example.core.model.*
import com.example.core.model.props.ServerProperties
import com.example.core.versions.DownloadException
import com.example.core.versions.DownloadManifest
import com.example.core.versions.ManifestCreator
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Path

// https://minecraft-de.gamepedia.com/Minecraft-Server#Serverordner
class ServerDirectory(val root: Directory) {
    companion object {
        private val creator = ManifestCreator()

        private fun scanForWorlds(dir: Directory): List<World> {
            val worldDirectories = dir.listFiles { file: File ->
                if (!file.isDirectory) return@listFiles false

                when (file.name) {
                    "crash-reports", "generated", "logs", "presets" -> return@listFiles false
                }

                return@listFiles file.list()?.contains("level.dat") ?: false
            }!!

            val res = mutableListOf<World>()
            for (c in worldDirectories) {
                res.add(World(c.toDirectory()))
            }
            return res
        }

        private fun scanForVersions(dir: Directory): MutableList<Path> {
            return dir.listFiles { file: File -> file.extension == "jar" }!!
                .asSequence()
                .map { it.toPath() }
                .associateBy { it.sha1() }
                .mapValues { (k, v) ->
                    val manifest = nullIfError<DownloadException, DownloadManifest> { creator.createManifest(sha1 = k) }
                    if (manifest == null) v to false
                    else v.renameTo("minecraft_server.${manifest.versionID}.jar") to true
                }
                .filterValues { it.second }
                .values
                .map { it.first }
                .toMutableList()
        }
    }

    val serverVersions = scanForVersions(root)
    val crashReports = Directory(root, "crash-reports")
    val logs = Directory(root, "logs")
    val worlds = scanForWorlds(root)
    val presets = Directory(root, "presets", create = true)
    val bannedIPs = JSONFile(File(root, "banned-ips.json"), ListSerializer(BannedIP.serializer()))
    val bannedPlayers = JSONFile(File(root, "banned-players.json"), ListSerializer(BannedPlayer.serializer()))
    val eula = EULA(File(root, "eula.txt"))
    val ops = JSONFile(File(root, "ops.json"), ListSerializer(OP.serializer()))
    val properties = File(root, "server.properties")
    val serverIcon = existsOrNull(File(root, "sever-icon.png"))
    val usercache = JSONFile(File(root, "usercache.json"), ListSerializer(ExpirablePlayerSignature.serializer()))
    val whitelist = JSONFile(File(root, "whitelist.json"), ListSerializer(PlayerSignature.serializer()))

    @OptIn(ExperimentalSerializationApi::class)
    fun parseProperties(): ServerProperties {
        return StringProperties.decodeFromString(ServerProperties.serializer(), readFromFile(properties))
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun writeProperties(props: ServerProperties, config: StringProperties = StringProperties.Default) {
        BufferedWriter(FileWriter(properties, false)).apply {
            write(config.encodeToString(ServerProperties.serializer(), props))
        }.close()
    }
}