package msw.server.core.common.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainOnlyOnce
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import msw.server.core.common.countSubstrings
import msw.server.core.common.extraChars
import msw.server.core.common.fixedSeedRandom
import msw.server.core.common.randomString

class StringsTests : FunSpec({
    context("String.truncate(Int, String) Tests") {
        test("should truncate strings longer than maxlen") {
            val testSubjects = List(10) {
                val maxlen = fixedSeedRandom.nextInt(3, 200)
                maxlen to randomString(maxlen + fixedSeedRandom.nextInt(1, 11))
            }

            for ((maxlen, str) in testSubjects) {
                val res = str.truncate(maxlen, "")
                res shouldHaveLength maxlen
                str shouldStartWith res
            }
        }

        test("should append suffix if truncating") {
            val testSubjects = List(10) {
                val maxlen = fixedSeedRandom.nextInt(3, 200)
                Triple(
                    maxlen,
                    randomString(maxlen + fixedSeedRandom.nextInt(1, 11)),
                    randomString(fixedSeedRandom.nextInt(1, maxlen / 10 + 2))
                )
            }

            for ((maxlen, str, suffix) in testSubjects) {
                val res = str.truncate(maxlen, suffix)
                res shouldHaveLength maxlen
                res shouldEndWith suffix
                str shouldStartWith res.removeSuffix(suffix)
            }
        }

        test("should not truncate or append suffix if shorter than maxlen") {
            val testSubjects = List(10) {
                val maxlen = fixedSeedRandom.nextInt(3, 200)
                maxlen to randomString(
                    (maxlen - fixedSeedRandom.nextInt(1, 11)).coerceAtLeast(1)
                )
            }

            for ((maxlen, str) in testSubjects) {
                val res = str.truncate(maxlen)
                res shouldBe str
            }
        }

        test("should not truncate or append suffix if exactly maxlen") {
            val testSubjects = List(10) {
                val maxlen = fixedSeedRandom.nextInt(3, 200)
                maxlen to randomString(maxlen)
            }

            for ((maxlen, str) in testSubjects) {
                val res = str.truncate(maxlen)
                res shouldBe str
            }
        }
    }

    context("String.replaceMultiple(Map<String, String>, Boolean) Tests") {
        test("should return same string for empty map") {
            val subjects = List(10) { randomString(100) }

            for (str in subjects) {
                str.replaceMultiple(emptyMap()) shouldBe str
            }
        }

        test("should replace single entry") {
            val subjects = List(10) { randomString(100) }
            val replacements = subjects.associateWith {
                val start = fixedSeedRandom.nextInt(0, it.length - 1)
                val end = fixedSeedRandom.nextInt(start + 1, it.length)
                mapOf(it.substring(start, end) to "#")
            }

            for ((orig, replMap) in replacements) {
                val result = orig.replaceMultiple(replMap)
                result shouldNotContain replMap.keys.single()
                result shouldContain replMap.values.single()
            }
        }

        test("should replace multiple entries") {
            val totalLength = 100
            val section1 = totalLength / 3
            val section2 = (totalLength / 3) * 2
            val subjects = List(10) { randomString(totalLength) }
            val replacements = subjects.associateWith {
                val start1 = fixedSeedRandom.nextInt(0, section1 - 1)
                val end1 = fixedSeedRandom.nextInt(start1 + 1, section1)
                val start2 = fixedSeedRandom.nextInt(section1, section2 - 1)
                val end2 = fixedSeedRandom.nextInt(start2 + 1, section2)
                val start3 = fixedSeedRandom.nextInt(section2, totalLength - 1)
                val end3 = fixedSeedRandom.nextInt(start3 + 1, totalLength)
                mapOf(
                    it.substring(start1, end1) to "()",
                    it.substring(start2, end2) to "[]",
                    it.substring(start3, end3) to "{}"
                )
            }

            for ((orig, replMap) in replacements) {
                val result = orig.replaceMultiple(replMap)
                for ((old, new) in replMap) {
                    result shouldNotContain old
                    result shouldContain new
                }
            }
        }
    }

    context("String.escape(CharArray) Tests") {
        test("should not change string for empty array") {
            val subjects = List(10) { randomString(100) }
            val emptyArray = CharArray(0)

            for (str in subjects) {
                str.escape(emptyArray) shouldBe str
            }
        }

        test("should escape single character") {
            val subjects = List(10) {
                val toEscape = extraChars.random(fixedSeedRandom)
                (randomString(fixedSeedRandom.nextInt(1, 11)) +
                        toEscape +
                        randomString(fixedSeedRandom.nextInt(1, 11))) to charArrayOf(toEscape)
            }

            for ((str, toEscape) in subjects) {
                val result = str.escape(toEscape)
                result shouldContainOnlyOnce toEscape.single().toString()
                result shouldContainOnlyOnce "\\${toEscape.single()}"
            }
        }

        test("should escape single character multiple times") {
            val subjects = List(10) {
                val toEscape = extraChars.random(fixedSeedRandom)
                val count = fixedSeedRandom.nextInt(1, 11)
                Triple(
                    List(count + 1) { randomString(fixedSeedRandom.nextInt(1, 11)) }.joinToString(toEscape.toString()),
                    charArrayOf(toEscape),
                    count
                )
            }

            for ((str, toEscape, count) in subjects) {
                val result = str.escape(toEscape)
                result.count { it == toEscape.single() } shouldBe count
                result.countSubstrings("\\${toEscape.single()}") shouldBe count
            }
        }

        test("should escape backslashes") {
            val subjects = List(10) {
                randomString(fixedSeedRandom.nextInt(1, 11)) +
                        "\\" +
                        randomString(fixedSeedRandom.nextInt(1, 11))
            }

            for (str in subjects) {
                str.escape(charArrayOf('\\')) shouldContainOnlyOnce "\\\\"
            }
        }
    }
})