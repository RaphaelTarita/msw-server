package msw.server.core.common.util

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import msw.server.core.common.randomString

class GenericTests : FunSpec({
    context("ignoreError(() -> Unit) Tests") {
        test("should ignore specified exception") {
            shouldNotThrow<IllegalArgumentException> {
                ignoreError<IllegalArgumentException> {
                    throw IllegalArgumentException(randomString(20))
                }
            }
        }

        test("should ignore subclass of specified exception") {
            shouldNotThrow<RuntimeException> {
                ignoreError<RuntimeException> {
                    throw IllegalArgumentException(randomString(20))
                }
            }
        }

        test("should not ignore non-specified exception") {
            shouldThrow<IllegalArgumentException> {
                ignoreError<NoSuchElementException> {
                    throw IllegalArgumentException(randomString(20))
                }
            }
        }

        test("should execute code that does not throw an exception") {
            var spy = "testBefore"
            val after = "testAfter"
            ignoreError<IllegalArgumentException> {
                spy = after
            }
            spy shouldBe after
        }
    }

    context("nullIfError(() -> R) Tests") {
        test("should return non-null result") {
            val expected = randomString()

            nullIfError<IllegalArgumentException, String> { expected } shouldBe expected
        }

        test("should return null when specified exception is thrown") {
            nullIfError<IllegalArgumentException, String> {
                throw IllegalArgumentException(randomString(20))
            } shouldBe null
        }

        test("should not return null when non-specified exception is thrown") {
            shouldThrow<IllegalArgumentException> {
                nullIfError<NoSuchElementException, String> {
                    throw IllegalArgumentException(randomString(20))
                }
            }
        }
    }
})