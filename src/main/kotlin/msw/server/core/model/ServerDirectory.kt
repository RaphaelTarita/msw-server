package msw.server.core.model

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.streams.asSequence
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import msw.server.core.common.GlobalInjections
import msw.server.core.common.JSONFile
import msw.server.core.common.SingletonInjectionImpl.terminal
import msw.server.core.common.StringProperties
import msw.server.core.common.directory
import msw.server.core.common.distinctBy
import msw.server.core.common.existsOrNull
import msw.server.core.common.readFromPath
import msw.server.core.common.readyMsg
import msw.server.core.common.renameTo
import msw.server.core.common.sha1
import msw.server.core.common.toVersionDetails
import msw.server.core.model.props.ServerProperties
import msw.server.core.model.world.World
import msw.server.core.versions.DownloadManager
import msw.server.core.versions.ManifestCreator
import msw.server.rpc.versions.VersionDetails

// https://minecraft-de.gamepedia.com/Minecraft-Server#Serverordner
context(GlobalInjections)
class ServerDirectory internal constructor(
    val manifestCreator: ManifestCreator,
    private val manager: DownloadManager,
    val root: Path,
    private val propertiesCodec: StringProperties = StringProperties.Default
) {
    companion object {
        private val specialDirectoryNames = setOf("crash-reports", "generated", "logs", "presets")
        context(GlobalInjections)
        fun initFor(
            root: Path,
            propertiesCodec: StringProperties = StringProperties.Default
        ): ServerDirectory {
            terminal.info("booting MSW in directory ${root.absolutePathString()}")
            return ServerDirectory(ManifestCreator(), DownloadManager(), root, propertiesCodec)
        }

        context(GlobalInjections)
        private fun scanForWorlds(dir: Path): List<World> {
            terminal.info("scanning for existing worlds...")
            return Files.list(dir)
                .asSequence()
                .filter { p: Path ->
                    when {
                        !Files.isDirectory(p) -> false
                        p.name in specialDirectoryNames -> false
                        else -> Files.list(p)
                            .asSequence()
                            .map { it.name }
                            .contains("level.dat")
                    }
                }.map {
                    terminal.info("- World found: ${it.name}")
                    World(it)
                }.toList()
        }

        private fun scanForVersions(dir: Path, creator: ManifestCreator): MutableMap<String, Pair<VersionDetails, Path>> {
            terminal.info("scanning for existing server versions...")
            return Files.list(dir)
                .asSequence()
                .filter { it.extension == "jar" }
                .map { it.sha1() to it }
                .distinctBy(Pair<String, Path>::first) { Files.delete(it.second) }
                .mapNotNull { (sha1, path) ->
                    creator.createManifestOrNull(sha1 = sha1)
                        ?.toVersionDetails()
                        ?.to(path)
                }
                .associateBy { (details, _) -> details.versionID }
                .onEach { (id, pair) ->
                    pair.second.renameTo("minecraft_server.${id}.jar")
                    terminal.info("- Server Version found: $id")
                }
                .toMutableMap()
        }
    }

    private val mutableVersions = scanForVersions(root, manifestCreator)
    val serverVersions: Map<String, Pair<VersionDetails, Path>>
        get() = mutableVersions
    val crashReports = directory(root, "crash-reports", create = true)
    val logs = directory(root, "logs")
    val worlds = scanForWorlds(root)
    val presets = directory(root, "presets", create = true)
    val bannedIPs = JSONFile(root / "banned-ips.json", ListSerializer(BannedIP.serializer()), listOf())
    val bannedPlayers = JSONFile(root / "banned-players.json", ListSerializer(BannedPlayer.serializer()), listOf())
    val eula = EULA(root / "eula.txt")
    val ops = JSONFile(root / "ops.json", ListSerializer(OP.serializer()), listOf())
    val properties = root / "server.properties"
    val serverIcon = (root / "sever-icon.png").existsOrNull()
    val usercache = JSONFile(root / "usercache.json", ListSerializer(ExpirablePlayerSignature.serializer()), listOf())
    val whitelist = JSONFile(root / "whitelist.json", ListSerializer(PlayerSignature.serializer()), listOf())

    init {
        addPreset("default", ServerProperties(), true)
        terminal.readyMsg("Directory")
    }

    fun presetIDs(): List<String> {
        return Files.list(presets)
            .asSequence()
            .map { it.nameWithoutExtension }
            .toList()
    }

    fun presetByID(presetID: String): String {
        return readFromPath(presets / "$presetID.properties")
    }

    fun presetExists(presetID: String): Boolean = presetIDs().contains(presetID)

    fun writePreset(presetID: String, propertiesString: String) {
        Files.newBufferedWriter(presets / "$presetID.properties").use {
            it.write(propertiesString)
        }
    }

    fun addPreset(presetID: String, preset: ServerProperties, force: Boolean = false) {
        val existing = Files.list(presets)
            .asSequence()
            .map { it.name }
            .toSet()
        if (!force && "$presetID.properties" !in existing) {
            throw FileAlreadyExistsException(
                (presets / "$presetID.properties").toFile(),
                null,
                "Preset ID '$presetID' already exists!"
            )
        }

        writePreset(presetID, propertiesCodec.encodeToString(preset))
    }

    fun readPreset(presetID: String): ServerProperties {
        return propertiesCodec.decodeFromString(presetByID(presetID))
    }

    fun removePreset(presetID: String) {
        Files.deleteIfExists(presets / "$presetID.properties")
    }

    fun activatePreset(presetID: String) {
        Files.copy(presets / "$presetID.properties", properties, StandardCopyOption.REPLACE_EXISTING)
    }

    fun parseProperties(): ServerProperties {
        return StringProperties.decodeFromString(readFromPath(properties))
    }

    fun addVersion(id: String, listeners: List<suspend (current: Long, total: Long) -> Unit> = emptyList()): Job {
        val manifest = manifestCreator.createManifest(id)
        val target = root / "minecraft_server.$id.jar"
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