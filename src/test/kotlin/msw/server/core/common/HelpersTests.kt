package msw.server.core.common

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.nio.file.Path
import kotlin.io.path.Path

class HelpersTests : FunSpec({
    context("Path.existsOrNull() Tests: ") {
        test("File path '$exists' exists, therefore existsOrNull() should return Path") {
            Path(exists).existsOrNull().shouldBeInstanceOf<Path>()
        }

        test("File path '$existsNot' does not exist, therefore existsOrNull() should return null") {
            Path(existsNot).existsOrNull().shouldBeNull()
        }
    }

    context("String.replaceMultiple(Map<Pair<Char, Char>>, Boolean) Tests") {
        test("String.replaceMultiple() should be capabable of transforming 'I love Kotlin' to leetspeak") {
            "I love Kotlin".replaceMultiple(
                mapOf(
                    "I" to "1",
                    "o" to "0",
                    "e" to "3",
                    "t" to "7"
                ),
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
    }
})