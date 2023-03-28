package msw.server.rpc.versions

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import msw.server.core.common.GlobalInjections
import msw.server.core.common.NoArg
import msw.server.core.common.ServerResponse
import msw.server.core.common.comparatorForNested
import msw.server.core.common.invertInsertionPoint
import msw.server.core.common.readyMsg
import msw.server.core.common.toVersionDetails
import msw.server.core.common.versionComparatorFor
import msw.server.core.model.ServerDirectory
import msw.server.core.versions.model.VersionType

context(GlobalInjections)
class VersionsService(private val directory: ServerDirectory) : VersionsGrpcKt.VersionsCoroutineImplBase() {
    init {
        terminal.readyMsg("gRPC Service [VersionsService]:")
    }

    override suspend fun listInstalledVersions(@Suppress("UNUSED_PARAMETER") request: NoArg): VersionIDList {
        return VersionIDList {
            ids = directory.serverVersions.keys.toList()
        }
    }

    override suspend fun listAvailableVersions(@Suppress("UNUSED_PARAMETER") request: NoArg): VersionIDList {
        return VersionIDList {
            ids = directory.manifestCreator.createManifests().map { it.versionID }
        }
    }

    override suspend fun getVersionDetails(request: VersionID): VersionDetails {
        return if (directory.serverVersions.containsKey(request.id)) {
            directory.getVersionDetails(request.id)
        } else {
            directory.manifestCreator.createManifest(request.id).toVersionDetails()
        }
    }

    override suspend fun getLatestRelease(@Suppress("UNUSED_PARAMETER") request: NoArg): LatestVersion {
        val latestRelease = directory.manifestCreator.latestRelease()
        return LatestVersion {
            installed = latestRelease.versionID in directory.serverVersions
            details = latestRelease.toVersionDetails()
        }
    }

    override suspend fun getLatestSnapshot(@Suppress("UNUSED_PARAMETER") request: NoArg): LatestVersion {
        val latestSnapshot = directory.manifestCreator.latestSnapshot()
        return LatestVersion {
            installed = latestSnapshot.versionID in directory.serverVersions
            details = latestSnapshot.toVersionDetails()
        }
    }

    override suspend fun recommendedVersionAbove(request: VersionID): RecommendedVersion {
        val (list, index) = recommendationHelper(request.id)
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

    override suspend fun recommendedVersionBelow(request: VersionID): RecommendedVersion {
        val (list, index) = recommendationHelper(request.id)
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
                    if (rootDoc.versions.first { it.id == request.id }.type != VersionType.SNAPSHOT) {
                        request.id
                    } else {
                        val releases = rootDoc.versions
                            .sortedWith(comparatorForNested(comp) { it.id })
                            .filterNot { it.type == VersionType.SNAPSHOT }

                        val releaseIndex = invertInsertionPoint(releases.map { it.id }.binarySearch(request.id, comp))

                        releases[releaseIndex - 1].id
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun installVersion(request: VersionID): Flow<Progress> {
        val channel = Channel<Progress>(Channel.UNLIMITED)
        val listener: suspend (Long, Long) -> Unit = { progress, size ->
            if (progress == size) {
                channel.trySend(
                    Progress {
                        status = Progress.Status.Response(
                            ServerResponse {
                                successful = true
                                response = "Version with ID ${request.id} was successfully downloaded"
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
                val job = directory.addVersion(request.id, listener)
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
                                response = "Failed to download version with ID '${request.id}'. Reason: ${exc.message}"
                            }
                        )
                    }
                )
            }
        }
    }

    override suspend fun uninstallVersion(request: VersionID): ServerResponse {
        return try {
            val success = directory.removeVersion(request.id)
            ServerResponse {
                successful = success
                response = if (success) {
                    "Successfully removed version '${request.id}'"
                } else {
                    "Version with ID '${request.id}' is not installed"
                }
            }
        } catch (exc: Exception) {
            ServerResponse {
                successful = false
                response = "Could not remove version with ID '${request.id}'. Reason: ${exc.message}"
            }
        }
    }

    private fun recommendationHelper(versionID: String): Pair<List<String>, Int> {
        val comp = versionComparatorFor(directory.manifestCreator.rootDocument())
        val installed = directory.serverVersions.keys.sortedWith(comp)
        return installed to installed.binarySearch(versionID, comp)
    }
}