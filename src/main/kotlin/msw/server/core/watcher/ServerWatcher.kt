package msw.server.core.watcher

import com.google.common.collect.HashBiMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import msw.server.core.common.MemoryAmount
import msw.server.core.common.m
import msw.server.core.common.runCommand
import msw.server.core.model.ServerDirectory
import msw.server.core.model.World

class ServerWatcher(private val directory: ServerDirectory) {
    companion object {
        const val ERR_NO_INSTANCE_ON_PORT = "no tracked instance is running on port "
        const val ERR_NO_INSTANCE_ON_WORLD = "no tracked instance is running on world "
        const val STOP_COMMAND = "stop"
    }

    private val reserveMutex = Mutex()
    private val launchMutex = Mutex()
    private val instances = mutableMapOf<Int, Pair<Process, InstanceConfiguration>>()
    private val portMappings = HashBiMap.create<Int, World>()

    suspend fun launchInstance(port: Int, world: World, config: InstanceConfiguration) {
        reserveMutex.withLock {
            require(!portMappings.containsKey(port)) {
                "Port $port is already in use by instance running on world '${portMappings[port]}'"
            }
            require(!portMappings.containsValue(world)) {
                "World $world is already in use by instance running on port ${portMappings.inverse()[world]}"
            }
            portMappings[port] = world
        }

        val command = mutableListOf<String>()
        command.add("java")
        command.add("-Xms${config.heapInit}")
        command.add("-Xmx${config.heapMax}")
        command.add("-jar")
        command.add("\"${directory.root.toPath().relativize(config.version)}\"")
        command.add("--port")
        command.add(port.toString())
        command.add("--world")
        command.add("\"${directory.root.toPath().relativize(world.root.toPath())}\"")
        if (!config.guiEnabled) {
            command.add("nogui")
        }

        launchMutex.withLock {
            directory.activatePreset(config.presetID)
            instances[port] = directory.root.runCommand(command) to config
        }
    }

    suspend fun launchInstance(
        port: Int,
        worldName: String,
        versionID: String,
        presetID: String,
        heapInit: MemoryAmount = 1024L.m,
        heapMax: MemoryAmount = 1024L.m,
        guiEnabled: Boolean = false
    ) = launchInstance(
        port,
        directory.getWorld(worldName),
        InstanceConfiguration(
            directory.getVersion(versionID),
            presetID,
            heapInit,
            heapMax,
            guiEnabled
        )
    )

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
        instance.outputStream.apply { write(command.toByteArray()) }.flush()
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