package msw.server.rpc.instancecontrol

import io.grpc.ServerServiceDefinition
import io.grpc.StatusRuntimeException
import io.grpc.kotlin.AbstractCoroutineServerImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import msw.server.core.common.ErrorTransformer
import msw.server.core.common.Port
import msw.server.core.common.ServerResponse
import msw.server.core.common.serverStream
import msw.server.core.common.truncate
import msw.server.core.common.unary
import msw.server.core.watcher.InstanceConfiguration
import msw.server.core.watcher.ServerWatcher

class InstanceControlService(
    private val watcher: ServerWatcher,
    private val transformer: ErrorTransformer<StatusRuntimeException>
) : AbstractCoroutineServerImpl() {
    companion object {
        private const val COMMAND_MAXLEN = 50
    }

    override fun bindService(): ServerServiceDefinition =
        ServerServiceDefinition.builder(InstanceControlGrpc.serviceDescriptor)
            .addMethod(unary(context, InstanceControlGrpc.getPortForWorldMethod, transformer.pack1(::getPortForWorld)))
            .addMethod(unary(context, InstanceControlGrpc.getWorldOnPortMethod, transformer.pack1(::getWorldOnPort)))
            .addMethod(unary(context, InstanceControlGrpc.getConfigMethod, transformer.pack1(::getConfig)))
            .addMethod(serverStream(context, InstanceControlGrpc.getLogMethod, transformer.pack1(::getLog)))
            .addMethod(unary(context, InstanceControlGrpc.sendCommandMethod, transformer.pack1(::sendCommand)))
            .build()

    private fun getPortForWorld(world: World): Port {
        return Port { num = watcher.portForWorld(world.name) }
    }

    private fun getWorldOnPort(port: Port): World {
        return World { name = watcher.worldOnPort(port.num).name }
    }

    private fun getConfig(port: Port): InstanceConfiguration {
        return watcher.configOnPort(port.num)
    }

    private fun getLog(port: Port): Flow<LogLine> {
        val reader = watcher.instanceOnPort(port.num).inputStream.bufferedReader()
        return flow {
            var line = withContext(Dispatchers.IO) { reader.readLine() }
            while (line != null) {
                emit(LogLine { this.line = line })
                line = withContext(Dispatchers.IO) { reader.readLine() }
            }
        }
    }

    private fun sendCommand(commandRequest: CommandRequest): ServerResponse {
        var world: String? = null
        val truncated = commandRequest.cmd.truncate(COMMAND_MAXLEN)
        return try {
            world = watcher.worldOnPort(commandRequest.port).name
            watcher.sendCommand(commandRequest.port, commandRequest.cmd)
            ServerResponse {
                successful = true
                response = "Command '$truncated' was executed on $world:${commandRequest.port}"
            }
        } catch (exc: Exception) {
            ServerResponse {
                successful = false
                response =
                    "Failed to execute commmand '$truncated' on instance ${world ?: '?'}:${commandRequest.port}. Reason: ${exc.message}"
            }
        }
    }
}