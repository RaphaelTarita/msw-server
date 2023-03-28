package msw.server.core.watcher

import com.google.common.collect.HashBiMap
import java.io.OutputStream
import kotlin.io.path.Path
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import msw.server.core.common.Directory
import msw.server.core.common.GlobalInjections
import msw.server.core.common.InstanceConfiguration
import msw.server.core.common.MemoryAmount
import msw.server.core.common.addTerminationCallback
import msw.server.core.common.mebibytes
import msw.server.core.common.readyMsg
import msw.server.core.common.runCommand
import msw.server.core.common.toCommandString
import msw.server.core.model.ServerDirectory
import msw.server.core.model.world.World
import msw.server.core.versions.DownloadManager
import msw.server.core.versions.ManifestCreator
import msw.server.rpc.instances.Instance
import msw.server.rpc.instances.InstanceList

context(GlobalInjections)
class ServerWatcher(
    val directory: ServerDirectory
) {
    companion object {
        context(GlobalInjections)
        fun initNew(
            root: Directory,
            port: Int,
            initialVersionId: String? = null
        ): ServerWatcher {
            val manifestCreator = ManifestCreator()
            val downloadManager = DownloadManager()
            val manifest = if (initialVersionId != null) manifestCreator.createManifest(initialVersionId) else manifestCreator.latestRelease()
            terminal.info("initializing new server installation...")
            terminal.info("- root dir: ${root.absolutePath}")
            terminal.info("- port: $port")
            terminal.info("- initial version: ${manifest.versionID}")

            val initialVersionPath = Path("minecraft_server_${manifest.versionID}.jar")
            terminal.info("downloading: $initialVersionPath")
            downloadManager.download(manifest, root.toPath().resolve(initialVersionPath))

            val command = buildList {
                add("java")
                add("-jar")
                add("\"$initialVersionPath\"")
                add("--port")
                add(port.toString())
                add("nogui")
            }

            terminal.info("starting server to generate server files...")
            terminal.info("- command: ${command.joinToString(" ")}")
            val process = root.runCommand(command)
            process.inputStream.transferTo(OutputStream.nullOutputStream()) // discard MC Server output
            process.waitFor()
            terminal.info("done.")

            val serverDirectory = ServerDirectory(manifestCreator, downloadManager, root)
            return ServerWatcher(serverDirectory)
        }

        const val ERR_NO_INSTANCE_ON_PORT = "no tracked instance is running on port "
        const val ERR_NO_INSTANCE_ON_WORLD = "no tracked instance is running on world "
        const val STOP_COMMAND = "stop"
    }

    private val reserveMutex = Mutex()
    private val launchMutex = Mutex()
    private val instances = mutableMapOf<Int, Pair<Process, InstanceConfiguration>>()
    private val portMappings = HashBiMap.create<Int, World>()

    init {
        terminal.readyMsg("Watcher")
    }

    suspend fun launchInstance(port: Int, world: World, config: InstanceConfiguration) {
        reserveMutex.withLock {
            check(!portMappings.containsKey(port)) {
                "Port $port is already in use by instance running on world '${portMappings[port]}'"
            }
            check(!portMappings.containsValue(world)) {
                "World $world is already in use by instance running on port ${portMappings.inverse()[world]}"
            }
            portMappings[port] = world
        }

        try {
            val command = buildList {
                add("java")
                add("-Xms${(config.heapInit ?: 1024L.mebibytes).toCommandString()}")
                add("-Xmx${(config.heapMax ?: 1024L.mebibytes).toCommandString()}")
                add("-jar")
                add("\"${directory.root.toPath().relativize(directory.getVersion(config.versionID))}\"")
                add("--port")
                add(port.toString())
                add("--world")
                add("\"${directory.root.toPath().relativize(world.root.toPath())}\"")
                if (!config.guiEnabled) {
                    add("nogui")
                }
            }

            launchMutex.withLock {
                directory.activatePreset(config.presetID)
                instances[port] = directory.root
                    .runCommand(command)
                    .addTerminationCallback(toplevelScope) {
                        portMappings.remove(port)
                        instances.remove(port)
                    } to config
            }
        } catch (exc: Exception) {
            portMappings.remove(port)
            instances.remove(port)
            throw exc
        }
    }

    suspend fun launchInstance(
        port: Int,
        worldName: String,
        config: InstanceConfiguration
    ) = launchInstance(port, directory.getWorld(worldName), config)

    suspend fun launchInstance(
        port: Int,
        worldName: String,
        versionID: String,
        presetID: String,
        heapInit: MemoryAmount = 1024L.mebibytes,
        heapMax: MemoryAmount = 1024L.mebibytes,
        guiEnabled: Boolean = false
    ) = launchInstance(
        port,
        worldName,
        InstanceConfiguration {
            this.versionID = versionID
            this.presetID = presetID
            this.heapInit = heapInit
            this.heapMax = heapMax
            this.guiEnabled = guiEnabled
        }
    )

    fun getInstances(): InstanceList {
        var counter = 1
        return InstanceList {
            instances = this@ServerWatcher.instances.map {
                Instance {
                    ordinal = counter++
                    port = it.key
                    worldName = portMappings[it.key]!!.name
                }
            }
        }
    }

    fun worldOnPort(port: Int): World {
        return portMappings[port] ?: throw IllegalArgumentException(ERR_NO_INSTANCE_ON_PORT + port)
    }

    fun portForWorld(world: World): Int {
        return portMappings.inverse()[world] ?: throw IllegalArgumentException(ERR_NO_INSTANCE_ON_WORLD + world)
    }

    fun portForWorld(worldName: String): Int = portForWorld(directory.getWorld(worldName))

    fun instanceOnPort(port: Int): Process {
        return instances[port]?.first ?: throw IllegalArgumentException(ERR_NO_INSTANCE_ON_PORT + port)
    }

    fun configOnPort(port: Int): InstanceConfiguration {
        return instances[port]?.second ?: throw IllegalArgumentException(ERR_NO_INSTANCE_ON_PORT + port)
    }

    fun instanceOnWorld(world: World): Process {
        return instanceOnPort(
            portMappings.inverse()[world]
                ?: throw IllegalArgumentException(ERR_NO_INSTANCE_ON_WORLD + world.name)
        )
    }

    fun configOnWorld(world: World): InstanceConfiguration {
        return configOnPort(
            portMappings.inverse()[world]
                ?: throw IllegalArgumentException(ERR_NO_INSTANCE_ON_WORLD + world.name)
        )
    }

    fun instanceOnWorld(worldName: String): Process = instanceOnWorld(directory.getWorld(worldName))

    fun configOnWorld(worldName: String): InstanceConfiguration = configOnWorld(directory.getWorld(worldName))

    fun sendCommand(port: Int, command: String) {
        val instance = instances[port]?.first ?: throw IllegalArgumentException(ERR_NO_INSTANCE_ON_PORT + port)
        instance.outputStream.apply { write((command + '\n').toByteArray()) }.flush()
    }

    fun sendCommand(world: World, command: String) {
        sendCommand(
            portMappings.inverse()[world]
                ?: throw IllegalArgumentException(ERR_NO_INSTANCE_ON_WORLD + world.name),
            command
        )
    }

    fun sendCommand(worldName: String, command: String) = sendCommand(directory.getWorld(worldName), command)

    fun stopInstance(port: Int) = sendCommand(port, STOP_COMMAND)

    fun stopInstance(world: World) = sendCommand(world, STOP_COMMAND)

    fun stopInstance(worldName: String) = sendCommand(worldName, STOP_COMMAND)
}