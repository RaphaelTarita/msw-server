package msw.server.rpc.instances

import msw.server.core.common.InstanceConfiguration
import msw.server.core.common.NoArg
import msw.server.core.common.Port
import msw.server.core.common.ServerResponse
import msw.server.core.watcher.ServerWatcher

class InstancesService(private val watcher: ServerWatcher) : InstancesGrpcKt.InstancesCoroutineImplBase() {
    override suspend fun getRunningInstances(@Suppress("UNUSED_PARAMETER") request: NoArg): InstanceList {
        return watcher.getInstances()
    }

    override suspend fun startInstance(request: StartInstanceRequest): ServerResponse {
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

    override suspend fun stopInstance(request: Port): ServerResponse {
        var world: String? = null
        return try {
            world = watcher.worldOnPort(request.num).name
            watcher.stopInstance(request.num)
            ServerResponse {
                successful = true
                response = "Instance $world:${request.num} stopped successfuly"
            }
        } catch (exc: Exception) {
            ServerResponse {
                successful = false
                response = "Failed to stop instance ${world ?: '?'}:${request.num}. Reason: ${exc.message}"
            }
        }
    }
}