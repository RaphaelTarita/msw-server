package msw.server.rpc.versions

import com.toasttab.protokt.Empty
import io.grpc.ServerServiceDefinition
import io.grpc.StatusRuntimeException
import io.grpc.kotlin.AbstractCoroutineServerImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import msw.server.core.common.*
import msw.server.core.model.ServerDirectory
import msw.server.core.versions.model.VersionType

class VersionsService(
    private val directory: ServerDirectory,
    private val transformer: ErrorTransformer<StatusRuntimeException>
) : AbstractCoroutineServerImpl() {
    override fun bindService(): ServerServiceDefinition =
        ServerServiceDefinition.builder(VersionsGrpc.serviceDescriptor)
            .addMethod(unary(context, VersionsGrpc.listInstalledVersionsMethod, transformer.pack1suspend(::listInstalledVersions)))
            .addMethod(unary(context, VersionsGrpc.listAvailableVersionsMethod, transformer.pack1suspend(::listAvailableVersions)))
            .addMethod(unary(context, VersionsGrpc.getVersionDetailsMethod, transformer.pack1suspend(::getVersionDetails)))
            .addMethod(unary(context, VersionsGrpc.recommendedVersionAboveMethod, transformer.pack1suspend(::recommendedVersionAbove)))
            .addMethod(unary(context, VersionsGrpc.recommendedVersionBelowMethod, transformer.pack1suspend(::recommendedVersionBelow)))
            .addMethod(serverStream(context, VersionsGrpc.installVersionMethod, transformer.pack1(::installVersion)))
            .addMethod(unary(context, VersionsGrpc.uninstallVersionMethod, transformer.pack1suspend(::uninstallVersion)))
            .build()

    private fun listInstalledVersions(empty: Empty): VersionIDList {
        return VersionIDList {
            ids = directory.serverVersions.keys.toList()
        }
    }

    private fun listAvailableVersions(empty: Empty): VersionIDList {
        return VersionIDList {
            ids = directory.manifestCreator.createManifests().map { it.versionID }
        }
    }

    private fun getVersionDetails(version: VersionID): VersionDetails {
        return if (directory.serverVersions.containsKey(version.id)) {
            directory.getVersionDetails(version.id)
        } else {
            directory.manifestCreator.createManifest(version.id).toVersionDetails()
        }
    }

    private fun recommendedVersionAbove(version: VersionID): RecommendedVersion {
        val (list, index) = recommendationHelper(version.id)
        val (found, pivot) = if (index < 0) false to invertInsertionPoint(index) else true to index
        val above = list.subList(pivot, list.size)
        return if (found) {
            RecommendedVersion {
                recommendedID = list[pivot]
                installed = true
                all = above
            }
        } else {
            RecommendedVersion {
                recommendedID = if (above.isNotEmpty()) {
                    installed = true
                    above.last()
                } else {
                    installed = false
                    directory.manifestCreator.latestRelease().versionID
                }
                all = above
            }
        }
    }

    private fun recommendedVersionBelow(version: VersionID): RecommendedVersion {
        val (list, index) = recommendationHelper(version.id)
        val (found, pivot) = if (index < 0) false to invertInsertionPoint(index) else true to index
        val below = list.subList(0, pivot)
        return if (found) {
            RecommendedVersion {
                recommendedID = list[pivot]
                installed = true
                all = below
            }
        } else {
            RecommendedVersion {
                recommendedID = if (below.isNotEmpty()) {
                    installed = true
                    below.last()
                } else {
                    installed = false
                    val rootDoc = directory.manifestCreator.rootDocument()
                    val comp = versionComparatorFor(rootDoc)
                    if (rootDoc.versions.first { it.id == version.id }.type != VersionType.SNAPSHOT) {
                        version.id
                    } else {
                        val releases = rootDoc.versions
                            .sortedWith(comparatorForNested(comp) { it.id })
                            .filterNot { it.type == VersionType.SNAPSHOT }

                        val releaseIndex = invertInsertionPoint(releases.map { it.id }.binarySearch(version.id, comp))

                        releases[releaseIndex - 1].id
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun installVersion(version: VersionID): Flow<Progress> {
        val channel = Channel<Progress>(Channel.UNLIMITED)
        val listener: suspend (Long, Long) -> Unit = { progress, size ->
            if (progress == size) {
                channel.trySend(
                    Progress {
                        status = Progress.Status.Response(
                            ServerResponse {
                                successful = true
                                response = "Version with ID ${version.id} was successfully downloaded"
                            }
                        )
                    }
                )
                channel.close()
            } else {
                val relative = progress.toDouble() * 100 / size.toDouble()
                channel.send(
                    Progress {
                        status = Progress.Status.RelativeProgress(relative)
                    }
                )
            }
        }

        return flow {
            try {
                val job = directory.addVersion(version.id, listener)
                while (!channel.isClosedForReceive) {
                    emit(channel.receive())
                }
                job.join()
            } catch (exc: Exception) {
                emit(
                    Progress {
                        status = Progress.Status.Response(
                            ServerResponse {
                                successful = false
                                response = "Failed to download version with ID '${version.id}'. Reason: ${exc.message}"
                            }
                        )
                    }
                )
            }
        }
    }

    private fun uninstallVersion(version: VersionID): ServerResponse {
        return try {
            val success = directory.removeVersion(version.id)
            ServerResponse {
                successful = success
                response = if (success) {
                    "Successfully removed version '${version.id}'"
                } else {
                    "Version with ID '${version.id}' is not installed"
                }
            }
        } catch (exc: Exception) {
            ServerResponse {
                successful = false
                response = "Could not remove version with ID '${version.id}'. Reason: ${exc.message}"
            }
        }
    }

    private fun recommendationHelper(versionID: String): Pair<List<String>, Int> {
        val comp = versionComparatorFor(directory.manifestCreator.rootDocument())
        val installed = directory.serverVersions.keys.sortedWith(comp)
        return installed to installed.binarySearch(versionID, comp)
    }
}