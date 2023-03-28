package msw.server.rpc.presets

import msw.server.core.common.GlobalInjections
import msw.server.core.common.StringProperties
import msw.server.core.common.readyMsg
import msw.server.core.common.semanticEquivalence
import msw.server.core.model.ServerDirectory
import msw.server.core.model.props.ServerProperties

context(GlobalInjections)
class PresetsService(private val directory: ServerDirectory) : PresetsGrpcKt.PresetsCoroutineImplBase() {
    init {
        terminal.readyMsg("gRPC Service [PresetsService]:")
    }

    override suspend fun getPresetIDs(request: PresetIDRegex): PresetIDList {
        val ids = directory.presetIDs()
        return PresetIDList {
            this.ids = if (request.regex.isEmpty()) {
                ids
            } else {
                val regex = request.regex.toRegex()
                ids.filter { regex.matches(it) }
            }
        }
    }

    override suspend fun getPreset(request: PresetID): PropertiesString {
        return PropertiesString { props = directory.presetByID(request.id) }
    }

    override suspend fun setPreset(request: IdentifiablePreset): PresetCRUDResponse {
        try {
            val response = if (!directory.presetExists(request.id)) {
                PresetCRUDResponse {
                    code = ResponseCode.CREATED
                    message = "New preset was created with id '${request.id}'"
                }
            } else {
                val prev = directory.presetByID(request.id)
                if (semanticEquivalence(
                        prev,
                        request.props,
                        StringProperties.Default,
                        ServerProperties.serializer()
                    )
                ) {
                    PresetCRUDResponse {
                        code = ResponseCode.UNCHANGED
                        message = "Preset with ID ${request.id} was unchanged (semantic match)"
                    }
                } else {
                    PresetCRUDResponse {
                        code = ResponseCode.MODIFIED
                        message = "Preset with ID ${request.id} was successfully modified"
                    }
                }
            }
            directory.writePreset(request.id, request.props)
            return response
        } catch (exc: Exception) {
            return PresetCRUDResponse {
                code = ResponseCode.ERROR
                message = "Set Preset request for ID ${request.id} failed, reason: ${exc.message}"
            }
        }
    }

    override suspend fun deletePreset(request: PresetID): PresetCRUDResponse {
        if (request.id == "default") {
            return PresetCRUDResponse {
                code = ResponseCode.ERROR
                message = "Attempt to delete 'default' preset (denied)"
            }
        }
        return try {
            if (directory.presetExists(request.id)) {
                directory.removePreset(request.id)
                PresetCRUDResponse {
                    code = ResponseCode.DELETED
                    message = "Preset with ID ${request.id} was successfully deleted"
                }
            } else {
                PresetCRUDResponse {
                    code = ResponseCode.UNCHANGED
                    message = "No Preset with ID ${request.id} found, state unchanged"
                }
            }
        } catch (exc: Exception) {
            PresetCRUDResponse {
                code = ResponseCode.ERROR
                message = "Delete Preset request for ID ${request.id} failed, reason: ${exc.message}"
            }
        }
    }
}