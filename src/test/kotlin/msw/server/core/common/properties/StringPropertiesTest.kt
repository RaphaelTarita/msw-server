package msw.server.core.common.properties

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import msw.server.core.common.*

@OptIn(ExperimentalSerializationApi::class)
class StringPropertiesTest : FunSpec({
    test("Properties serialization should escape KV separators in keys") {
        val model = PropertiesEscapeModel()
        val encoded = StringProperties.encodeToString(PropertiesEscapeModel.serializer(), model)
        encoded.substringBeforeLast('=') shouldContain "\\\\[=:\\s\\\\#!]".toRegex()
        StringProperties.decodeFromString(PropertiesEscapeModel.serializer(), encoded) shouldBe model
    }

    test("Properties serialization should escape line breaks in values") {
        val model = Model01("Hello,\nWorld!")
        val encoded = StringProperties.encodeToString(Model01.serializer(), model)
        encoded shouldContain "\\"
        StringProperties.decodeFromString(Model01.serializer(), encoded) shouldBe model
    }

    include(modelTests(Model01(), Model01.serializer()))
    include(modelTests(Model02(), Model02.serializer()))
    include(modelTests(Model03(), Model03.serializer()))
    include(modelTests(Model04(), Model04.serializer()))
    include(modelTests(Model05(), Model05.serializer()))
    include(modelTests(Model06(), Model06.serializer()))
}) {
    companion object {
        fun <T : ModelMarker> modelTests(m: T, serializer: KSerializer<T>) = funSpec {
            val custom = StringProperties {
                spacesBeforeSeparator = 1
                spacesAfterSeparator = 1
                kvSeparator = KVSeparator.COLON
            }
            val name = m::class.simpleName!!

            test("$name: Serializing and deserializing should result in the same object (using default config)") {
                val string = StringProperties.encodeToString(serializer, m)
                StringProperties.decodeFromString(serializer, string) shouldBe m
            }

            test("$name: Serializing and deserializing should result in the same object (using custom config)") {
                val string = custom.encodeToString(serializer, m)
                custom.decodeFromString(serializer, string) shouldBe m
            }

            test("$name: Serializing and deserializing should result in the same object (using mixed config)") {
                val string = custom.encodeToString(serializer, m)
                StringProperties.decodeFromString(serializer, string) shouldBe m
            }
        }
    }
}
