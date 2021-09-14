package msw.server.core.model.adapters

import com.google.common.net.InetAddresses
import java.net.InetAddress
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object IPSerializer : KSerializer<InetAddress> {
    override val descriptor = PrimitiveSerialDescriptor("java.net.InetAddress", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: InetAddress) {
        encoder.encodeString(value.toString().split('/')[1])
    }

    @Suppress("UnstableApiUsage")
    override fun deserialize(decoder: Decoder): InetAddress {
        return InetAddresses.forString(decoder.decodeString())
    }
}