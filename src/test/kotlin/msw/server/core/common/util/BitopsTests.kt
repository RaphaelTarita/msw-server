package msw.server.core.common.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.beEmpty
import io.kotest.matchers.string.shouldMatch
import msw.server.core.common.fixedSeedRandom

class BitopsTests : FunSpec({
    context("ByteArray.toHexString() Tests") {
        test("should return only hex chars") {
            val subject = fixedSeedRandom.nextBytes(256)

            subject.toHexString() shouldMatch Regex("[0-9a-fA-F]+")
        }

        test("should return empty string for empty ByteArray") {
            val subject = byteArrayOf()

            subject.toHexString() should beEmpty()
        }

        test("should return correct hexadecimal representation") {
            val subject = fixedSeedRandom.nextBytes(256)

            subject.toHexString()
                .chunked(2) { it.toString().toUByte(16).toByte() }
                .toByteArray() shouldBe subject
        }
    }
})