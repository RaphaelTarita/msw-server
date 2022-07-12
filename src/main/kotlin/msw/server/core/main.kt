package msw.server.core

import io.grpc.ServerBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import java.util.concurrent.Executors
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import msw.server.core.common.Directory
import msw.server.core.common.ErrorTransformer
import msw.server.core.common.existsOrNull
import msw.server.core.model.ServerDirectory
import msw.server.core.model.props.ServerProperties
import msw.server.core.versions.DownloadException
import msw.server.core.watcher.ServerWatcher
import msw.server.rpc.instancecontrol.InstanceControlService
import msw.server.rpc.instances.InstancesService
import msw.server.rpc.presets.PresetsService
import msw.server.rpc.versions.VersionsService

@OptIn(ExperimentalCoroutinesApi::class)
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

    val transformer = ErrorTransformer.typedRulesetWithLambdaFallback(
        {
            StatusRuntimeException(
                Status.UNKNOWN
                    .withDescription("Unhandled Exception occurred ($it)")
                    .withCause(it)
            )
        },
        IllegalArgumentException::class to {
            StatusRuntimeException(
                Status.INVALID_ARGUMENT
                    .withDescription("Bad Request: Call had malformed arguments (${it.message})")
                    .withCause(it)
            )
        },
        IllegalStateException::class to {
            StatusRuntimeException(
                Status.FAILED_PRECONDITION
                    .withDescription("Precondition Failure: System was in wrong state for your call (${it.message})")
                    .withCause(it)
            )
        },
        IndexOutOfBoundsException::class to {
            StatusRuntimeException(
                Status.OUT_OF_RANGE
                    .withDescription("Bad Request: Call requested access to an element with out-of-range index (${it.message})")
                    .withCause(it)
            )
        },
        NoSuchElementException::class to {
            StatusRuntimeException(
                Status.NOT_FOUND
                    .withDescription("Resource not found: Call-requested resource was not inside target collection (${it.message})")
                    .withCause(it)
            )
        },
        NoSuchFileException::class to {
            StatusRuntimeException(
                Status.NOT_FOUND
                    .withDescription("Resource not found: (k.io) File System location specified by call does not exist (${it.message})")
                    .withCause(it)
            )
        },
        java.nio.file.NoSuchFileException::class to {
            StatusRuntimeException(
                Status.NOT_FOUND
                    .withDescription("Resource not found: (j.nio) File System location specified by call does not exist (${it.message})")
                    .withCause(it)
            )
        },
        FileAlreadyExistsException::class to {
            StatusRuntimeException(
                Status.ALREADY_EXISTS
                    .withDescription("Resource already exists: Call requested to create an already-existing resource (${it.message})")
                    .withCause(it)
            )
        },
        DownloadException::class to {
            StatusRuntimeException(
                Status.FAILED_PRECONDITION
                    .withDescription("Precondition Failure: Download failed (${it.message})")
                    .withCause(it)
            )
        }
    )

    ServerBuilder.forPort(50051)
        .addService(InstanceControlService(watcher, transformer))
        .addService(InstancesService(watcher, transformer))
        .addService(PresetsService(watcher.directory, transformer))
        .addService(VersionsService(watcher.directory, transformer))
        .build()
        .start()
        .awaitTermination()

    toplevelDispatcher.close()
}