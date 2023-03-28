package msw.server.core

import io.grpc.ServerBuilder
import java.util.concurrent.Executors
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import msw.server.core.common.Directory
import msw.server.core.common.existsOrNull
import msw.server.core.model.ServerDirectory
import msw.server.core.model.props.ServerProperties
import msw.server.core.watcher.ServerWatcher
import msw.server.rpc.instancecontrol.InstanceControlService
import msw.server.rpc.instances.InstancesService
import msw.server.rpc.presets.PresetsService
import msw.server.rpc.versions.VersionsService

fun main() {
    val toplevelDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    val toplevelScope = CoroutineScope(toplevelDispatcher)
    val netScope = CoroutineScope(EmptyCoroutineContext)

    val root = Directory("./minecraft_server", create = true)
    val watcher = if (root.existsOrNull()?.list()?.none { "server" in it && it.endsWith(".jar") } != false) {
        ServerWatcher.initNew(toplevelScope, netScope, root, 25565)
    } else {
        val directory = ServerDirectory(root, toplevelScope, netScope)
        ServerWatcher(directory, toplevelScope)
    }

    println(watcher.directory.worlds.map { it.name })
    watcher.directory.addPreset("deleteTest", ServerProperties(), true)

    ServerBuilder.forPort(50051)
        .addService(InstanceControlService(watcher))
        .addService(InstancesService(watcher))
        .addService(PresetsService(watcher.directory))
        .addService(VersionsService(watcher.directory))
        .build()
        .start()
        .awaitTermination()

    toplevelDispatcher.close()
}