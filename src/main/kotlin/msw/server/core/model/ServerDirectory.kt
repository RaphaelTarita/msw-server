package msw.server.core.model

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import msw.server.core.common.*
import msw.server.core.model.*
import msw.server.core.model.props.ServerProperties
import msw.server.core.versions.DownloadException
import msw.server.core.versions.DownloadManager
import msw.server.core.versions.DownloadManifest
import msw.server.core.versions.ManifestCreator
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

// https://minecraft-de.gamepedia.com/Minecraft-Server#Serverordner
@OptIn(ExperimentalSerializationApi::class)
class ServerDirectory(val root: Directory, private val propertiesCodec: StringProperties = StringProperties.Default) {
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

        @OptIn(ExperimentalPathApi::class)
        private fun Path.serverVersionID(): String {
            return nameWithoutExtension
                .substringAfter('.', "")
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

    init {
        addPreset("default", ServerProperties(), true)
    }

    fun addPreset(presetID: String, preset: ServerProperties, force: Boolean = false) {
        if (!force) {
            require("$presetID.properties" !in presets.list()!!) {
                "Preset ID '$presetID' already exists!"
            }
        }

        Files.newBufferedWriter(composePath(presets, "$presetID.properties")).apply {
            write(propertiesCodec.encodeToString(preset))
        }.close()
    }

    fun readPreset(presetID: String): ServerProperties {
        return propertiesCodec.decodeFromString(readFromPath(composePath(presets, "$presetID.properties")))
    }

    fun removePreset(presetID: String) {
        Files.deleteIfExists(composePath(presets, "$presetID.properties"))
    }

    fun activatePreset(presetID: String) {
        Files.copy(composePath(presets, "$presetID.properties"), properties, StandardCopyOption.REPLACE_EXISTING)
    }

    fun parseProperties(): ServerProperties {
        return StringProperties.decodeFromString(readFromPath(properties))
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

    fun getWorld(name: String): World {
        val matches = worlds.filter { it.name == name }
        require(matches.isNotEmpty()) {
            "No world with name $name found"
        }
        check(matches.size <= 1) {
            "Multiple worlds with name $name registered"
        }

        return matches.single()
    }

    fun getVersion(id: String): Path {
        val matches = serverVersions.filter { it.serverVersionID() == id }
        require(matches.isNotEmpty()) {
            "No version with id $id found"
        }
        check(matches.size <= 1) {
            "Multiple versions with id $id registered"
        }

        return matches.single()
    }
}