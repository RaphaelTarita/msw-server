package msw.server.rpc.instances

import com.toasttab.protokt.Empty
import io.grpc.ServerServiceDefinition
import io.grpc.StatusRuntimeException
import io.grpc.kotlin.AbstractCoroutineServerImpl
import msw.server.core.common.ErrorTransformer
import msw.server.core.common.Port
import msw.server.core.common.ServerResponse
import msw.server.core.common.unary
import msw.server.core.watcher.InstanceConfiguration
import msw.server.core.watcher.ServerWatcher

class InstancesService(
    private val watcher: ServerWatcher,
    private val transformer: ErrorTransformer<StatusRuntimeException>
) : AbstractCoroutineServerImpl() {
    override fun bindService(): ServerServiceDefinition =
        ServerServiceDefinition.builder(InstancesGrpc.serviceDescriptor)
            .addMethod(unary(context, InstancesGrpc.getRunningInstancesMethod, transformer.pack1(::getRunningInstances)))
            .addMethod(unary(context, InstancesGrpc.startInstanceMethod, transformer.pack1suspend(::startInstance)))
            .addMethod(unary(context, InstancesGrpc.stopInstanceMethod, transformer.pack1(::stopInstance)))
            .build()

    private fun getRunningInstances(@Suppress("UNUSED_PARAMETER") empty: Empty): InstanceList {
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