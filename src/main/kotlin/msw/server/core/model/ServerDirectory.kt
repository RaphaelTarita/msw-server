package msw.server.core.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import msw.server.core.common.*
import msw.server.core.model.props.ServerProperties
import msw.server.core.model.world.World
import msw.server.core.versions.DownloadException
import msw.server.core.versions.DownloadManager
import msw.server.core.versions.DownloadManifest
import msw.server.core.versions.ManifestCreator
import msw.server.rpc.versions.VersionDetails
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.nameWithoutExtension

// https://minecraft-de.gamepedia.com/Minecraft-Server#Serverordner
@OptIn(ExperimentalSerializationApi::class)
class ServerDirectory(
    val root: Directory,
    private val toplevelScope: CoroutineScope,
    private val netScope: CoroutineScope,
    private val propertiesCodec: StringProperties = StringProperties.Default
) {
    companion object {
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

        private fun scanForVersions(dir: Directory, creator: ManifestCreator): MutableMap<String, Pair<VersionDetails, Path>> {
            return dir.listFiles { file: File -> file.extension == "jar" }!!
                .map { it.toPath() }
                .map { it.sha1() to it }
                .toMap { (_, path) ->
                    Files.delete(path)
                }
                .mapValues { (sha1, path) ->
                    val manifest = nullIfError<DownloadException, DownloadManifest> { creator.createManifest(sha1 = sha1) }
                    if (manifest != null) {
                        path.renameTo("minecraft_server.${manifest.versionID}.jar")
                    }
                    return@mapValues manifest to path
                }
                .filterValues { (manifest, _) ->
                    manifest != null
                }
                .mapValues { (_, pair) ->
                    pair.first!!.toVersionDetails() to pair.second
                }
                .mapKeys { (_, pair) ->
                    pair.first.versionID
                }
                .toMutableMap()
        }

        @OptIn(ExperimentalPathApi::class)
        private fun Path.serverVersionID(): String {
            return nameWithoutExtension
                .substringAfter('.', "")
        }
    }

    val manifestCreator = ManifestCreator(netScope)
    private val manager = DownloadManager(netScope)
    private val mutableVersions = scanForVersions(root, manifestCreator)
    val serverVersions: Map<String, Pair<VersionDetails, Path>>
        get() = mutableVersions
    val crashReports = Directory(root, "crash-reports", create = true)
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

    fun presetIDs(): List<String> {
        return presets.listFiles()!!.map { it.nameWithoutExtension }
    }

    fun presetByID(presetID: String): String {
        return readFromPath(composePath(presets, "$presetID.properties"))
    }

    fun presetExists(presetID: String): Boolean = presetIDs().contains(presetID)

    fun writePreset(presetID: String, propertiesString: String) {
        Files.newBufferedWriter(composePath(presets, "$presetID.properties")).apply {
            write(propertiesString)
        }.close()
    }

    fun addPreset(presetID: String, preset: ServerProperties, force: Boolean = false) {
        if (!force) {
            if ("$presetID.properties" !in presets.list()!!) {
                throw FileAlreadyExistsException(
                    File(presets, "$presetID.properties"),
                    null,
                    "Preset ID '$presetID' already exists!"
                )
            }
        }

        writePreset(presetID, propertiesCodec.encodeToString(preset))
    }

    fun readPreset(presetID: String): ServerProperties {
        return propertiesCodec.decodeFromString(presetByID(presetID))
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

    fun addVersion(id: String, listeners: List<suspend (current: Long, total: Long) -> Unit> = emptyList()): Job {
        val manifest = manifestCreator.createManifest(id)
        val target = composePath(root, "minecraft_server.$id.jar")
        return toplevelScope.launch {
            manager.download(manifest, target, listeners)
            mutableVersions[id] = manifest.toVersionDetails() to target
        }
    }

    fun addVersion(id: String, listener: suspend (Long, Long) -> Unit): Job = addVersion(id, listOf(listener))

    fun removeVersion(id: String): Boolean {
        val version = serverVersions[id]
        return if (version != null) {
            Files.delete(version.second)
            mutableVersions.remove(id)
            true
        } else false
    }

    private fun getVersionFull(id: String): Pair<VersionDetails, Path> {
        return serverVersions[id] ?: throw IllegalArgumentException("No version with id $id found")
    }

    fun getVersion(id: String): Path {
        return getVersionFull(id).second
    }

    fun getVersionDetails(id: String): VersionDetails {
        return getVersionFull(id).first
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
}