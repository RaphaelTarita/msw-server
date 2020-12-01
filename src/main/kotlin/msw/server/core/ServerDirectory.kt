package msw.server.core

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import msw.server.core.common.*
import msw.server.core.model.*
import msw.server.core.model.props.ServerProperties
import msw.server.core.versions.DownloadException
import msw.server.core.versions.DownloadManager
import msw.server.core.versions.DownloadManifest
import msw.server.core.versions.ManifestCreator
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

// https://minecraft-de.gamepedia.com/Minecraft-Server#Serverordner
class ServerDirectory(val root: Directory) {
    companion object {
        private val creator = ManifestCreator()
        private val manager = DownloadManager()

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
    val bannedIPs = JSONFile(composePath(root, "banned-ips.json"), ListSerializer(BannedIP.serializer()))
    val bannedPlayers = JSONFile(composePath(root, "banned-players.json"), ListSerializer(BannedPlayer.serializer()))
    val eula = EULA(composePath(root, "eula.txt"))
    val ops = JSONFile(composePath(root, "ops.json"), ListSerializer(OP.serializer()))
    val properties = composePath(root, "server.properties")
    val serverIcon = existsOrNull(composePath(root, "sever-icon.png"))
    val usercache = JSONFile(composePath(root, "usercache.json"), ListSerializer(ExpirablePlayerSignature.serializer()))
    val whitelist = JSONFile(composePath(root, "whitelist.json"), ListSerializer(PlayerSignature.serializer()))

    private var activePresetID: String = "default"

    @OptIn(ExperimentalSerializationApi::class)
    fun readPreset(presetID: String): ServerProperties {
        val presetFile = presets.listFiles { _, name ->
            name == "$presetID.properties"
        }!!.single().toPath()

        return StringProperties.decodeFromString(ServerProperties.serializer(), readFromPath(presetFile))
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun writePreset(presetID: String, preset: ServerProperties, config: StringProperties = StringProperties.Default) {
        val target = composePath(presets, "$presetID.properties")
        Files.newBufferedWriter(target, StandardOpenOption.WRITE).apply {
            write(config.encodeToString(ServerProperties.serializer(), preset))
        }
    }

    fun swapInPreset(presetID: String) {
        val newPreset = composePath(presets, "$presetID.properties")
        val oldTarget = composePath(presets, "$activePresetID.properties")
        Files.copy(properties, oldTarget)
        Files.copy(newPreset, properties)
        activePresetID = presetID
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun parseProperties(): ServerProperties {
        return StringProperties.decodeFromString(ServerProperties.serializer(), readFromPath(properties))
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun writeProperties(props: ServerProperties, config: StringProperties = StringProperties.Default) {
        Files.newBufferedWriter(properties, StandardOpenOption.WRITE).apply {
            write(config.encodeToString(ServerProperties.serializer(), props))
        }.close()
    }

    fun addVersion(id: String): Job {
        val manifest = creator.createManifest(id)
        val target = composePath(root, "minecraft_server.$id.jar")
        return GlobalScope.launch {
            manager.download(manifest, target)
            serverVersions.add(target)
        }
    }

    fun removeVersion(id: String): Boolean {
        val target = composePath(root, "minecraft_server.$id.jar")
        return Files.deleteIfExists(target).also {
            serverVersions.remove(target)
        }
    }
}