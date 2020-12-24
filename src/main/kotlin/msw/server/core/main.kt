package msw.server.core

import io.grpc.ServerBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import msw.server.core.common.Directory
import msw.server.core.common.ErrorTransformer
import msw.server.core.model.ServerDirectory
import msw.server.core.model.props.ServerProperties
import msw.server.core.versions.DownloadException
import msw.server.core.watcher.ServerWatcher
import msw.server.rpc.instancecontrol.InstanceControlService
import msw.server.rpc.instances.InstancesService
import msw.server.rpc.presets.PresetsService
import msw.server.rpc.versions.VersionsService
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
fun main() {
    val toplevelDispatcher = newSingleThreadContext("toplevel")
    val toplevelScope = CoroutineScope(toplevelDispatcher)
    val netScope = CoroutineScope(EmptyCoroutineContext)

    val directory = ServerDirectory(Directory("C:\\Users\\rapha\\Desktop\\minecraft_server"), toplevelScope, netScope)
    val watcher = ServerWatcher(directory, toplevelScope)
    directory.addPreset("deleteTest", ServerProperties(), true)

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
        .addService(PresetsService(directory, transformer))
        .addService(VersionsService(directory, transformer))
        .build()
        .start()
        .awaitTermination()

    toplevelDispatcher.close()
}