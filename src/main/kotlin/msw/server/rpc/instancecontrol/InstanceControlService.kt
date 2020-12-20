package msw.server.rpc.instancecontrol

import io.grpc.ServerServiceDefinition
import io.grpc.StatusRuntimeException
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.ServerCalls.serverStreamingServerMethodDefinition
import io.grpc.kotlin.ServerCalls.unaryServerMethodDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import msw.server.core.common.ErrorTransformer
import msw.server.core.common.truncate
import msw.server.core.watcher.InstanceConfiguration
import msw.server.core.watcher.Port
import msw.server.core.watcher.ServerResponse
import msw.server.core.watcher.ServerWatcher

class InstanceControlService(
    private val watcher: ServerWatcher,
    private val transformer: ErrorTransformer<StatusRuntimeException>
) : AbstractCoroutineServerImpl() {
    override fun bindService(): ServerServiceDefinition = ServerServiceDefinition.builder(InstanceControlGrpc.serviceDescriptor)
        .addMethod(unaryServerMethodDefinition(context, InstanceControlGrpc.getPortForWorldMethod, transformer.pack1suspend(::getPortForWorld)))
        .addMethod(unaryServerMethodDefinition(context, InstanceControlGrpc.getWorldOnPortMethod, transformer.pack1suspend(::getWorldOnPort)))
        .addMethod(unaryServerMethodDefinition(context, InstanceControlGrpc.getConfigMethod, transformer.pack1suspend(::getConfig)))
        .addMethod(serverStreamingServerMethodDefinition(context, InstanceControlGrpc.getLogMethod, transformer.pack1(::getLog)))
        .addMethod(unaryServerMethodDefinition(context, InstanceControlGrpc.sendCommandMethod, transformer.pack1suspend(::sendCommand)))
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
        val truncated = commandRequest.cmd.truncate(50)
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
                response = "Failed to execute commmand '$truncated' on instance ${world ?: '?'}:${commandRequest.port}. Reason: ${exc.message}"
            }
        }
    }
}