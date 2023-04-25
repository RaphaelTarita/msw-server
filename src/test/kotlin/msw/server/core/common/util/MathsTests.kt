package msw.server.core.common.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.negative
import io.kotest.matchers.ints.positive
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import msw.server.core.common.fixedSeedRandom

class MathsTests : FunSpec({
    context("Long.coerceToInt() Tests") {
        test("should coerce Long that's bigger than Int.MAX_VALUE to Int.MAX_VALUE") {
            val testSubjects = List(10) { fixedSeedRandom.nextLong(Int.MAX_VALUE.toLong() + 1, Long.MAX_VALUE) }
            for (long in testSubjects) {
                long.coerceToInt() shouldBe Int.MAX_VALUE
            }
        }

        test("should coerce Long that's smaller than Int.MIN_VALUE to Int.MIN_VALUE") {
            val testSubjects = List(10) { fixedSeedRandom.nextLong(Long.MIN_VALUE, Int.MIN_VALUE.toLong() - 1) }
            for (long in testSubjects) {
                long.coerceToInt() shouldBe Int.MIN_VALUE
            }
        }

        test("should not change Long that's within Int bounds") {
            val testSubjects = List(10) { fixedSeedRandom.nextLong(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()) }
            for (long in testSubjects) {
                long.coerceToInt() shouldBe long.toInt()
            }
        }
    }

    context("invertInsertionPoint(Int) Tests") {
        test("should invert the integer for all values != -1") {
            val positives = List(8) { fixedSeedRandom.nextInt(2, Int.MAX_VALUE) } + 0 + 1
            val negatives = List(10) { fixedSeedRandom.nextInt(Int.MIN_VALUE, -2) }

            for (pos in positives) {
                invertInsertionPoint(pos) shouldBe negative()
            }

            for (neg in negatives) {
                invertInsertionPoint(neg) shouldBe positive()
            }

            invertInsertionPoint(-1) shouldBeExactly 0
        }
    }
})