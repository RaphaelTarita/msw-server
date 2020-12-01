package msw.server.core.common

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerializationException

class JSONFileTests : FunSpec({
    test("Loading a file into the model Model01 should work") {
        shouldNotThrowAny {
            JSONFile("$exists/json00.json", Model01.serializer())
        }.get() shouldBe Model01("s", 0, true)
    }

    test("Loading an empty file should not fail, but deserialization should fail") {
        val file = shouldNotThrowAny {
            JSONFile("$exists/empty.json", Model01.serializer())
        }

        shouldThrow<SerializationException> {
            file.get()
        }
    }

    test("A get() after reload() should newly deserialize the file contents") {
        val file = shouldNotThrowAny {
            JSONFile("$exists/empty.json", Model01.serializer())
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
            JSONFile("$exists/json01.json", Model01.serializer()).apply {
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
        private val m1 = JSONFile("$exists/json01.json", Model01.serializer(), true) to Model01()
        private val m2 = JSONFile("$exists/json02.json", Model02.serializer(), true) to Model02()
        private val m3 = JSONFile("$exists/json03.json", Model03.serializer(), true) to Model03()
        private val m4 = JSONFile("$exists/json04.json", Model04.serializer(), true) to Model04()
        private val m5 = JSONFile("$exists/json05.json", Model05.serializer(), true) to Model05()
        private val m6 = JSONFile("$exists/json06.json", Model06.serializer(), true) to Model06()

        private fun cleanupAll() {
            m1.first.reload()
            m2.first.reload()
            m3.first.reload()
            m4.first.reload()
            m5.first.reload()
            m6.first.reload()
        }

        private fun <T : ModelMarker> modelTests(name: String, model: Pair<JSONFile<T>, T>) = funSpec {
            test("$name: Writing to and reading from file should result in the original value") {
                model.first.apply { setAndCommit(model.second) }.get() shouldBe model.second
                model.first.reload()
            }

            test("$name: Two calls to get() should result in the same object") {
                model.first.get() shouldBe model.first.get()
            }
        }
    }
}