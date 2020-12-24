package msw.server.rpc.presets

import io.grpc.ServerServiceDefinition
import io.grpc.StatusRuntimeException
import io.grpc.kotlin.AbstractCoroutineServerImpl
import kotlinx.serialization.ExperimentalSerializationApi
import msw.server.core.common.ErrorTransformer
import msw.server.core.common.StringProperties
import msw.server.core.common.semanticEquivalence
import msw.server.core.common.unary
import msw.server.core.model.ServerDirectory
import msw.server.core.model.props.ServerProperties

class PresetsService(
    private val directory: ServerDirectory,
    private val transformer: ErrorTransformer<StatusRuntimeException>
) : AbstractCoroutineServerImpl() {
    override fun bindService(): ServerServiceDefinition = ServerServiceDefinition.builder(PresetsGrpc.serviceDescriptor)
        .addMethod(unary(context, PresetsGrpc.getPresetIDsMethod, transformer.pack1suspend(::getPresetIDs)))
        .addMethod(unary(context, PresetsGrpc.getPresetMethod, transformer.pack1suspend(::getPreset)))
        .addMethod(unary(context, PresetsGrpc.setPresetMethod, transformer.pack1suspend(::setPreset)))
        .addMethod(unary(context, PresetsGrpc.deletePresetMethod, transformer.pack1suspend(::deletePreset)))
        .build()

    private fun getPresetIDs(optionalRegex: PresetIDRegex): PresetIDList {
        val ids = directory.presetIDs()
        return PresetIDList {
            this.ids = if (optionalRegex.regex.isEmpty()) {
                ids
            } else {
                val regex = optionalRegex.regex.toRegex()
                ids.filter { regex.matches(it) }
            }
        }
    }

    private fun getPreset(id: PresetID): PropertiesString {
        return PropertiesString { props = directory.presetByID(id.id) }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun setPreset(idPreset: IdentifiablePreset): PresetCRUDResponse {
        try {
            val response = if (!directory.presetExists(idPreset.id)) {
                PresetCRUDResponse {
                    code = ResponseCode.CREATED
                    message = "New preset was created with id '${idPreset.id}'"
                }
            } else {
                val prev = directory.presetByID(idPreset.id)
                if (semanticEquivalence(
                        prev,
                        idPreset.props,
                        StringProperties.Default,
                        ServerProperties.serializer()
                    )
                ) {
                    PresetCRUDResponse {
                        code = ResponseCode.UNCHANGED
                        message = "Preset with ID ${idPreset.id} was unchanged (semantic match)"
                    }
                } else {
                    PresetCRUDResponse {
                        code = ResponseCode.MODIFIED
                        message = "Preset with ID ${idPreset.id} was successfully modified"
                    }
                }
            }
            directory.writePreset(idPreset.id, idPreset.props)
            return response
        } catch (exc: Exception) {
            return PresetCRUDResponse {
                code = ResponseCode.ERROR
                message = "Set Preset request for ID ${idPreset.id} failed, reason: ${exc.message}"
            }
        }
    }

    private fun deletePreset(id: PresetID): PresetCRUDResponse {
        if (id.id == "default") {
            return PresetCRUDResponse {
                code = ResponseCode.ERROR
                message = "Attempt to delete 'default' preset (denied)"
            }
        }
        return try {
            if (directory.presetExists(id.id)) {
                directory.removePreset(id.id)
                PresetCRUDResponse {
                    code = ResponseCode.DELETED
                    message = "Preset with ID ${id.id} was successfully deleted"
                }
            } else {
                PresetCRUDResponse {
                    code = ResponseCode.UNCHANGED
                    message = "No Preset with ID ${id.id} found, state unchanged"
                }
            }
        } catch (exc: Exception) {
            PresetCRUDResponse {
                code = ResponseCode.ERROR
                message = "Delete Preset request for ID ${id.id} failed, reason: ${exc.message}"
            }
        }
    }
}