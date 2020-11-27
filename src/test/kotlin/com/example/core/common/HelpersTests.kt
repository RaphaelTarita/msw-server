package com.example.core.common

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.maps.shouldContainKeys
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File

class HelpersTests : FunSpec({
    context("String.isNumeric() Tests: ") {
        test("String.isNumeric() should behave according to the contents of the string") {
            "123".isNumeric().shouldBeTrue()
            "abc".isNumeric().shouldBeFalse()
        }
    }

    context("existsOrNull(File) Tests: ") {
        test("File path '$exists' exists, therefore existsOrNull(File(path)) should return File") {
            existsOrNull(File(exists)).shouldBeInstanceOf<File>()
        }

        test("File path '$existsNot' does not exist, therefore existsOrNull(File(path) should return null") {
            existsOrNull(File(existsNot)).shouldBeNull()
        }
    }

    context("String.replaceMultiple(vararg Pair<Char, Char>, Boolean) Tests") {
        test("String.replaceMultiple() should be capabable of transforming 'I love Kotlin' to leetspeak") {
            "I love Kotlin".replaceMultiple(
                "I" to "1",
                "o" to "0",
                "e" to "3",
                "t" to "7",
                ignoreCase = true
            ) shouldBe "1 l0v3 K07l1n"
        }
    }

    context("String.escape(vararg Char, Boolean) / String.unescape(vararg Char, Boolean) Tests") {
        test("String.escape() should escape a string containing the special characters '~', '/', '+', '-'") {
            val escaped = randomString(50, *escapeThese).escape(*escapeThese)

            for (i in escaped.indices) {
                (escaped[i] !in escapeThese || (i > 0 && escaped[i - 1] == '\\')).shouldBeTrue()
            }
        }

        test("String.unescape() should unescape a string containing the escaped characters '~', '/', '+', '-'") {
            val unescaped = randomString(50, *escapeThese).escape(*escapeThese).unescape(*escapeThese)

            for (i in unescaped.indices) {
                (unescaped[i] !in escapeThese || i < 1 || unescaped[i - 1] != '\\').shouldBeTrue()
            }
        }
    }

    context("xxxJoin(Map<K, V>, Map<K, U>) variations Tests") {
        test("innerJoin() should only contain KV-Pairs present in both maps") {
            innerJoin(map1, map2).shouldContainKeys("Hello", "MSW", "KXS")
        }

        test("leftOuterJoin() should contain all keys from the first map") {
            leftOuterJoin(map1, map2).shouldContainKeys(*map1.keys.toTypedArray())
        }

        test("rightOuterJoin() should contain all keys from the second map") {
            rightOuterJoin(map1, map2).shouldContainKeys(*map2.keys.toTypedArray())
        }

        test("fullOuterJoin() should contain all keys from both maps") {
            val allKeys = map1.keys.toMutableSet().apply { addAll(map2.keys) }.toTypedArray()
            fullOuterJoin(map1, map2).shouldContainKeys(*allKeys)
        }

        test("errorJoin() should not fulfill its preconditions and throw an IllegalArgumentException") {
            shouldThrow<IllegalArgumentException> {
                errorJoin(map1, map2)
            }
        }
    }
})