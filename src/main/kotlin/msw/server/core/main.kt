package msw.server.core

import com.github.ajalt.mordant.rendering.TextColors
import io.grpc.ServerBuilder
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import msw.server.core.common.SingletonInjectionImpl
import msw.server.core.common.directory
import msw.server.core.common.existsOrNull
import msw.server.core.model.ServerDirectory
import msw.server.core.model.props.ServerProperties
import msw.server.core.watcher.ServerWatcher
import msw.server.rpc.instancecontrol.InstanceControlService
import msw.server.rpc.instances.InstancesService
import msw.server.rpc.presets.PresetsService
import msw.server.rpc.versions.VersionsService

fun main() = with(SingletonInjectionImpl) {
    val root = directory(Path("./minecraft_server"), create = true)
    val watcher = if (root.existsOrNull()
            ?.listDirectoryEntries()
            ?.none { "server" in it.name && it.extension == "jar" } != false
    ) {
        ServerWatcher.initNew(root, 25565)
    } else {
        val directory = ServerDirectory.initFor(root)
        ServerWatcher(directory)
    }

    watcher.directory.addPreset("deleteTest", ServerProperties(), true)

    terminal.info("starting gRPC server...")
    val server = ServerBuilder.forPort(50051)
        .addService(InstanceControlService(watcher))
        .addService(InstancesService(watcher))
        .addService(PresetsService(watcher.directory))
        .addService(VersionsService(watcher.directory))
        .build()
        .start()

    terminal.println()
    terminal.println(TextColors.brightCyan("=== ALL COMPONENTS INITIALIZED. GRPC SERVER READY AND LISTENING ==="))
    terminal.println()

    server.awaitTermination()

    close()
}