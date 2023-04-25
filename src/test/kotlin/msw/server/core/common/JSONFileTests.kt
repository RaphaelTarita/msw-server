package msw.server.core.common

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlinx.serialization.SerializationException

class JSONFileTests : FunSpec({
    val jsonFileEmpty: Path
    val jsonFile0: Path
    val jsonFile1: Path
    val jsonFile2: Path
    val jsonFile3: Path
    val jsonFile4: Path
    val jsonFile5: Path
    val jsonFile6: Path

    tempdir {
        jsonFileEmpty = tempfile("empty", ".json")
        jsonFile0 = tempfile("json00", ".json")
        jsonFile1 = tempfile("json01", ".json")
        jsonFile2 = tempfile("json02", ".json")
        jsonFile3 = tempfile("json03", ".json")
        jsonFile4 = tempfile("json04", ".json")
        jsonFile5 = tempfile("json05", ".json")
        jsonFile6 = tempfile("json06", ".json")
    }

    jsonFile0.writeText("{ \"s\": \"s\", \"i\": 0, \"b\": true}")
    jsonFile1.writeText("{}")
    jsonFile2.writeText("{}")
    jsonFile3.writeText("{}")
    jsonFile4.writeText("{}")
    jsonFile5.writeText("{}")
    jsonFile6.writeText("{}")

    val m1 = JSONFile(jsonFile1, Model01.serializer(), writeDefaults = true) to Model01()
    val m2 = JSONFile(jsonFile2, Model02.serializer(), writeDefaults = true) to Model02()
    val m3 = JSONFile(jsonFile3, Model03.serializer(), writeDefaults = true) to Model03()
    val m4 = JSONFile(jsonFile4, Model04.serializer(), writeDefaults = true) to Model04()
    val m5 = JSONFile(jsonFile5, Model05.serializer(), writeDefaults = true) to Model05()
    val m6 = JSONFile(jsonFile6, Model06.serializer(), writeDefaults = true) to Model06()

    test("Loading a file into the model Model01 should work") {
        shouldNotThrowAny {
            JSONFile(jsonFile0, Model01.serializer())
        }.get() shouldBe Model01("s", 0, true)
    }

    test("Loading an empty file should not fail, but deserialization should fail") {
        val file = shouldNotThrowAny {
            JSONFile(jsonFileEmpty, Model01.serializer())
        }

        shouldThrow<SerializationException> {
            file.get()
        }
    }

    test("A get() after reload() should newly deserialize the file contents") {
        val file = shouldNotThrowAny {
            JSONFile(jsonFileEmpty, Model01.serializer())
        }

        shouldNotThrowAny {
            file.set(Model01())
            file.get()
        } shouldBe Model01()

        shouldNotThrowAny {
            file.reload()
        }

        shouldThrow<SerializationException> {
            file.get()
        }
    }

    test("performAndCommit() should write the result of the lambda to the file") {
        val file = shouldNotThrowAny {
            JSONFile(jsonFile1, Model01.serializer()).apply {
                set(Model01())
                performAndCommit {
                    Model01(s, i + 1, !b)
                }
            }
        }

        shouldNotThrowAny {
            file.reload()
            file.get()
        } shouldBe Model01("Hello, World!", 43, true)
    }

    include(modelTests("Model01", m1))
    include(modelTests("Model02", m2))
    include(modelTests("Model03", m3))
    include(modelTests("Model04", m4))
    include(modelTests("Model05", m5))
    include(modelTests("Model06", m6))
}) {
    companion object {
        private fun <T : ModelMarker> modelTests(name: String, fileAndModel: Pair<JSONFile<T>, T>) = funSpec {
            val (jsonFile, model) = fileAndModel
            test("$name: Writing to and reading from file should result in the original value") {
                jsonFile.apply { setAndCommit(model) }.get() shouldBe model
                jsonFile.reload()
            }

            test("$name: Two calls to get() should result in the same object") {
                jsonFile.get() shouldBe jsonFile.get()
            }
        }
    }
}