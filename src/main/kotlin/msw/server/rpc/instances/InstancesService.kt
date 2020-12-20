package msw.server.rpc.instances

import com.toasttab.protokt.Empty
import io.grpc.ServerServiceDefinition
import io.grpc.StatusRuntimeException
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.ServerCalls.unaryServerMethodDefinition
import msw.server.core.common.ErrorTransformer
import msw.server.core.watcher.InstanceConfiguration
import msw.server.core.watcher.Port
import msw.server.core.watcher.ServerResponse
import msw.server.core.watcher.ServerWatcher

class InstancesService(
    private val watcher: ServerWatcher,
    private val transformer: ErrorTransformer<StatusRuntimeException>
) : AbstractCoroutineServerImpl() {
    override fun bindService(): ServerServiceDefinition = ServerServiceDefinition.builder(InstancesGrpc.serviceDescriptor)
        .addMethod(unaryServerMethodDefinition(context, InstancesGrpc.getRunningInstancesMethod, transformer.pack1suspend(::getRunningInstances)))
        .addMethod(unaryServerMethodDefinition(context, InstancesGrpc.startInstanceMethod, transformer.pack1suspend(::startInstance)))
        .addMethod(unaryServerMethodDefinition(context, InstancesGrpc.stopInstanceMethod, transformer.pack1suspend(::stopInstance)))
        .build()

    private fun getRunningInstances(empty: Empty): InstanceList {
        return watcher.getInstances()
    }

    private suspend fun startInstance(request: StartInstanceRequest): ServerResponse {
        return try {
            watcher.launchInstance(
                request.port,
                request.worldName,
                request.config ?: InstanceConfiguration {
                    versionID = "1.16.3"
                    presetID = "default"
                }
            )
            ServerResponse {
                successful = true
                response = "Instance started successfully: ${request.worldName}:${request.port}"
            }
        } catch (exc: Exception) {
            ServerResponse {
                successful = false
                response = "Failed to start instance ${request.worldName}:${request.port}. Reason: ${exc.message}"
            }
        }
    }

    private fun stopInstance(port: Port): ServerResponse {
        var world: String? = null
        return try {
            world = watcher.worldOnPort(port.num).name
            watcher.stopInstance(port.num)
            ServerResponse {
                successful = true
                response = "Instance $world:${port.num} stopped successfuly"
            }
        } catch (exc: Exception) {
            ServerResponse {
                successful = false
                response = "Failed to stop instance ${world ?: '?'}:${port.num}. Reason: ${exc.message}"
            }
        }
    }
}