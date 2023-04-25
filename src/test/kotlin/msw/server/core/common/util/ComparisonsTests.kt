package msw.server.core.common.util

import com.google.common.net.InetAddresses
import io.kotest.assertions.Actual
import io.kotest.assertions.Expected
import io.kotest.assertions.failure
import io.kotest.assertions.print.print
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.ints.negative
import io.kotest.matchers.ints.positive
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import msw.server.core.common.KVSeparator
import msw.server.core.common.Model01
import msw.server.core.common.Model02
import msw.server.core.common.Model03
import msw.server.core.common.Model04
import msw.server.core.common.Model05
import msw.server.core.common.Model06
import msw.server.core.common.ModelMarker
import msw.server.core.common.StringProperties
import msw.server.core.common.fixedSeedRandom
import msw.server.core.common.randomString

class ComparisonsTests : FunSpec({
    include(semanticEquivalenceTests("Model01", m1) {
        it.copy(i = 43)
    })
    include(semanticEquivalenceTests("Model02", m2) {
        it.copy(ip = InetAddresses.forString("0.0.0.0"))
    })
    include(semanticEquivalenceTests("Model03", m3) {
        it.copy(
            l = Long.MAX_VALUE,
            nested2 = it.nested2.copy(
                dateTime = OffsetDateTime.ofInstant(
                    Instant.EPOCH,
                    ZoneId.systemDefault()
                )
            )
        )
    })
    include(semanticEquivalenceTests("Model04", m4) {
        val newMap = it.map.toMutableMap()
        newMap[newMap.keys.random(fixedSeedRandom)] = 42
        it.copy(map = newMap)
    })
    include(semanticEquivalenceTests("Model05", m5) {
        it.copy(
            path2 = it.path2.copy(
                path2 = it.path2.path2.copy(
                    path2 = it.path2.path2.path2.copy(
                        endpoint1 = it.path2.path2.path2.endpoint1 + ' '
                    )
                )
            )
        )
    })
    include(semanticEquivalenceTests("Model06", m6) {
        it.copy(double = 3.14159)
    })

    context("compareNullable(T?, T?) Tests") {
        test("should return CONTINUE for non-null values") {
            val a = randomString()
            val b = randomString()

            compareNullable(a, b) shouldBe NullableCompare.CONTINUE(a, b)
        }

        test("should return RESULT if one value is null") {
            val a = randomString()
            val b = null

            when (val comp = compareNullable(a, b)) {
                is NullableCompare.RESULT -> comp.res shouldBe positive()
                else -> throw failure(Expected(NullableCompare.RESULT(1).print()), Actual(comp.print()))
            }

            when (val comp = compareNullable(b, a)) {
                is NullableCompare.RESULT -> comp.res shouldBe negative()
                else -> throw failure(Expected(NullableCompare.RESULT(-1).print()), Actual(comp.print()))
            }
        }

        test("should return RESULT if both values are null") {
            val a = null
            val b = null

            compareNullable(a, b) shouldBe NullableCompare.RESULT(0)
        }
    }

    context("comparatorForNested(Comparator<U>, (T) -> U) Tests") {
        val innerComparator = Comparator<Int> { i1, i2 -> i1 - i2 }
        val subject = comparatorForNested(innerComparator) { s: String -> s.length }

        test("should return 0 if both input objects are null") {
            val a = null
            val b = null

            subject.compare(a, b) shouldBe 0
        }

        test("should compare correctly if one side is null") {
            val a = randomString()
            val b = null

            subject.compare(a, b) shouldBe positive()
            subject.compare(b, a) shouldBe negative()
        }

        test("should compare nested objects using the inner comparator") {
            val strings = List(10) { randomString(fixedSeedRandom.nextInt(1, 100)) }
            val pairs = strings.flatMap { left ->
                strings.map { right ->
                    left to right
                }
            }

            for ((left, right) in pairs) {
                val expected = left.length - right.length
                val actual = subject.compare(left, right)
                when {
                    expected > 0 -> actual shouldBe positive()
                    expected < 0 -> actual shouldBe negative()
                    else -> actual shouldBe 0
                }
            }
        }
    }
}) {
    companion object {
        private val m1 = Model01() to Model01.serializer()
        private val m2 = Model02() to Model02.serializer()
        private val m3 = Model03() to Model03.serializer()
        private val m4 = Model04() to Model04.serializer()
        private val m5 = Model05() to Model05.serializer()
        private val m6 = Model06() to Model06.serializer()

        private fun <T : ModelMarker> semanticEquivalenceTests(
            name: String,
            modelAndSerializer: Pair<T, KSerializer<T>>,
            modelMutation: (T) -> T
        ) = funSpec {
            val (model, serializer) = modelAndSerializer
            val mutated = modelMutation(model)
            val json = Json
            val altJson = Json(json) {
                prettyPrint = true
            }
            val properties = StringProperties
            val altProperties = StringProperties(properties) {
                kvSeparator = KVSeparator.COLON
                spacesBeforeSeparator = 4
                spacesAfterSeparator = 4
            }

            test("should return true for equal $name serializations (JSON)") {
                val lop = json.encodeToString(serializer, model)
                val rop = json.encodeToString(serializer, model)

                semanticEquivalence(lop, rop, json, serializer) shouldBe true
            }

            test("should return true for equal $name serializations (Properties)") {
                val lop = properties.encodeToString(serializer, model)
                val rop = properties.encodeToString(serializer, model)

                semanticEquivalence(lop, rop, properties, serializer) shouldBe true
            }

            test("should return true for semantically equivalent $name serializations (JSON)") {
                val lop = json.encodeToString(serializer, model)
                val rop = altJson.encodeToString(serializer, model)

                semanticEquivalence(lop, rop, json, serializer) shouldBe true
            }

            test("should return true for semantically equivalent $name serializations (Properties)") {
                val lop = properties.encodeToString(serializer, model)
                val rop = altProperties.encodeToString(serializer, model)

                semanticEquivalence(lop, rop, properties, serializer) shouldBe true
            }

            test("should return false for semantically inequivalent $name serializations (JSON)") {
                val lop = json.encodeToString(serializer, model)
                val rop1 = json.encodeToString(serializer, mutated)
                val rop2 = altJson.encodeToString(serializer, mutated)

                semanticEquivalence(lop, rop1, json, serializer) shouldBe false
                semanticEquivalence(lop, rop2, json, serializer) shouldBe false
            }

            test("should return false for semantically inequivalent $name serializations (Properties)") {
                val lop = properties.encodeToString(serializer, model)
                val rop1 = properties.encodeToString(serializer, mutated)
                val rop2 = altProperties.encodeToString(serializer, mutated)

                semanticEquivalence(lop, rop1, properties, serializer) shouldBe false
                semanticEquivalence(lop, rop2, properties, serializer) shouldBe false
            }
        }
    }
}