package msw.server.rpc.instancecontrol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import msw.server.core.common.GlobalInjections
import msw.server.core.common.InstanceConfiguration
import msw.server.core.common.Port
import msw.server.core.common.ServerResponse
import msw.server.core.common.readyMsg
import msw.server.core.common.truncate
import msw.server.core.watcher.ServerWatcher

context(GlobalInjections)
class InstanceControlService(private val watcher: ServerWatcher) : InstanceControlGrpcKt.InstanceControlCoroutineImplBase() {
    companion object {
        private const val COMMAND_MAXLEN = 50
    }

    init {
        terminal.readyMsg("gRPC Service [InstanceControlService]:")
    }

    override suspend fun getPortForWorld(request: World): Port {
        return Port { num = watcher.portForWorld(request.name) }
    }

    override suspend fun getWorldOnPort(request: Port): World {
        return World { name = watcher.worldOnPort(request.num).name }
    }

    override suspend fun getConfig(request: Port): InstanceConfiguration {
        return watcher.configOnPort(request.num)
    }

    override fun getLog(request: Port): Flow<LogLine> {
        val reader = watcher.instanceOnPort(request.num).inputStream.bufferedReader()
        return flow {
            var line = withContext(Dispatchers.IO) { reader.readLine() }
            while (line != null) {
                emit(LogLine { this.line = line })
                line = withContext(Dispatchers.IO) { reader.readLine() }
            }
        }
    }

    override suspend fun sendCommand(request: CommandRequest): ServerResponse {
        var world: String? = null
        val truncated = request.cmd.truncate(COMMAND_MAXLEN)
        return try {
            world = watcher.worldOnPort(request.port).name
            watcher.sendCommand(request.port, request.cmd)
            ServerResponse {
                successful = true
                response = "Command '$truncated' was executed on $world:${request.port}"
            }
        } catch (exc: Exception) {
            ServerResponse {
                successful = false
                response =
                    "Failed to execute commmand '$truncated' on instance ${world ?: '?'}:${request.port}. Reason: ${exc.message}"
            }
        }
    }
}