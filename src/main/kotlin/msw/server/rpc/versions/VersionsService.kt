package msw.server.rpc.versions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import msw.server.core.common.GlobalInjections
import msw.server.core.common.NoArg
import msw.server.core.common.ServerResponse
import msw.server.core.common.util.readyMsg
import msw.server.core.common.util.toVersionDetails
import msw.server.core.model.ServerDirectory
import msw.server.core.versions.model.VersionType

context(GlobalInjections)
class VersionsService(private val directory: ServerDirectory) : VersionsGrpcKt.VersionsCoroutineImplBase() {
    init {
        terminal.readyMsg("gRPC Service [VersionsService]:")
    }

    override suspend fun listInstalledVersions(request: NoArg): VersionIDList {
        return VersionIDList {
            ids = directory.serverVersions
                .keys
                .toList()
        }
    }

    override suspend fun listAvailableVersions(request: NoArg): VersionIDList {
        return VersionIDList {
            ids = directory.manifestCreator
                .rootDocument()
                .versions
                .map { it.id }
        }
    }

    override suspend fun getVersionDetails(request: VersionID): VersionDetails {
        return if (directory.serverVersions.containsKey(request.id)) {
            directory.getVersionDetails(request.id)
        } else {
            directory.manifestCreator.createManifest(request.id).toVersionDetails()
        }
    }

    override suspend fun getLatestRelease(request: NoArg): LatestVersion {
        val latestRelease = directory.manifestCreator.latestRelease()
        return LatestVersion {
            installed = latestRelease.versionID in directory.serverVersions
            details = latestRelease.toVersionDetails()
        }
    }

    override suspend fun getLatestSnapshot(request: NoArg): LatestVersion {
        val latestSnapshot = directory.manifestCreator.latestSnapshot()
        return LatestVersion {
            installed = latestSnapshot.versionID in directory.serverVersions
            details = latestSnapshot.toVersionDetails()
        }
    }

    override suspend fun recommendedVersionAbove(request: VersionID): RecommendedVersion {
        val above = installedVersions(request.id, true)

        return RecommendedVersion {
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

    override suspend fun recommendedVersionBelow(request: VersionID): RecommendedVersion {
        val below = installedVersions(request.id, false)

        return RecommendedVersion {
            recommendedID = if (below.isNotEmpty()) {
                installed = true
                below.last()
            } else {
                installed = false
                directory.manifestCreator
                    .rootDocument()
                    .versions
                    .sorted()
                    .takeWhile { it.id != request.id }
                    .last { it.type != VersionType.SNAPSHOT }
                    .id
            }
            all = below
        }
    }

    override fun installVersion(request: VersionID): Flow<Progress> {
        return flow {
            try {
                val job = directory.addVersion(request.id) { progress, size ->
                    emit(determineProgress(progress, size, request.id))
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

    private fun installedVersions(pivot: String, above: Boolean): List<String> {
        val all = directory.serverVersions
            .keys
            .sortedBy { directory.manifestCreator.versionsMap[it] }

        return if (above) all.takeLastWhile { it != pivot } else all.takeWhile { it != pivot }
    }

    private fun determineProgress(progress: Long, size: Long, id: String): Progress {
        return if (progress == size) {
            Progress {
                status = Progress.Status.Response(
                    ServerResponse {
                        successful = true
                        response = "Version with ID $id was successfully downloaded"
                    }
                )
            }
        } else {
            Progress {
                status = Progress.Status.RelativeProgress(progress.toDouble() * 100 / size.toDouble())
            }
        }
    }
}