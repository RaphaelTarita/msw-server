package msw.server.core.common.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import msw.server.core.common.fixedSeedRandom
import msw.server.core.common.randomString

class CollectionsTests : FunSpec({
    context("Map<K, V>.ifContainsKey(K, (Pair<K, V>) -> R) Tests") {
        test("should execute if contains key") {
            val subject = buildMap {
                repeat(20) {
                    put(randomString(), fixedSeedRandom.nextInt())
                }
            }
            val existingKey = subject.keys.random(fixedSeedRandom)
            var spy = false

            subject.ifContainsKey(existingKey) {
                spy = true
            }

            spy shouldBe true
        }

        test("should return non-null if contains key") {
            val subject = buildMap {
                repeat(20) {
                    put(randomString(), fixedSeedRandom.nextInt())
                }
            }
            val existingKey = subject.keys.random(fixedSeedRandom)
            val existingValue = subject.getValue(existingKey)

            subject.ifContainsKey(existingKey) { (k, v) ->
                k + v
            } shouldBe existingKey + existingValue
        }

        test("should not execute and return null if not contains key") {
            val subject = buildMap {
                repeat(20) {
                    put(randomString(), fixedSeedRandom.nextInt())
                }
            }
            val nonExistentKey = randomString(11)
            var spy = false

            subject.ifContainsKey(nonExistentKey) {
                spy = true
            } should beNull()
            spy shouldBe false
        }
    }

    context("Sequence<T>.distinctBy((T) -> K, (T) -> Unit) Tests") {
        test("should only retain distinct elements (in order)") {
            val subject = listOf(1, 2, 2, 3, 4, 4, 4, 6, 7, 9, 9)

            subject.asSequence()
                .distinctBy({ it }) { /*no-op*/ }
                .toList() shouldBe subject.asSequence()
                .distinctBy { it }
                .toList()
        }

        test("should utilize selector") {
            val subject = List(10) { fixedSeedRandom.nextInt() }

            subject.asSequence()
                .distinctBy({ 0 }) { /*no-op*/ }
                .toList() shouldBe subject.asSequence()
                .distinctBy { 0 }
                .toList()
        }

        test("should execute onDuplicates block") {
            val subject = listOf(1, 2, 2, 3, 4, 4, 4, 6, 7, 9, 9)
            var counter = 0

            subject.asSequence()
                .distinctBy({ it }) { ++counter }
                .toList() should beInstanceOf<List<Int>>()

            counter shouldBe subject.size - subject.toSet().size
        }
    }
})