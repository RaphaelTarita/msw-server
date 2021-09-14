package msw.server.core.versions.model.adapters

import java.net.URL
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object URLSerializer : KSerializer<URL> {
    override val descriptor = PrimitiveSerialDescriptor("java.net.URL", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: URL) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): URL {
        return URL(decoder.decodeString())
    }
}