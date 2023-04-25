package msw.server.core.common.util

import com.github.ajalt.mordant.terminal.Terminal
import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldMatch
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import msw.server.core.common.MemoryAmount
import msw.server.core.common.MemoryUnit
import msw.server.core.common.fixedSeedRandom
import msw.server.core.common.randomDownloadManifest
import msw.server.core.common.randomOffsetDateTime
import msw.server.core.common.randomString
import msw.server.core.versions.model.VersionType
import msw.server.rpc.versions.VersionLabel

class MswTests : FunSpec({
    context("VersionType.toVersionLabel() Tests") {
        test("should return correct version label for version type") {
            VersionType.OLD_ALPHA.toVersionLabel() shouldBe VersionLabel.ALPHA
            VersionType.OLD_BETA.toVersionLabel() shouldBe VersionLabel.BETA
            VersionType.RELEASE.toVersionLabel() shouldBe VersionLabel.RELEASE
            VersionType.SNAPSHOT.toVersionLabel() shouldBe VersionLabel.SNAPSHOT
        }

        test("should throw IllegalArgumentException when trying to convert 'ALL' version type") {
            shouldThrow<IllegalArgumentException> {
                VersionType.ALL.toVersionLabel()
            }.message shouldContain VersionType.ALL.toString()
        }
    }

    context("OffsetDateTime.toTimestamp() Tests") {
        test("should return Timestamp with correct seconds and nanoseconds mesaures") {
            val subjects = List(10) { randomOffsetDateTime() }

            for (dateTime in subjects) {
                val timestamp = dateTime.toTimestamp()
                timestamp.seconds shouldBe dateTime.toEpochSecond()
                timestamp.nanos shouldBe dateTime.nano
            }
        }
    }

    context("DownloadManifest.toVersionDetails() Tests") {
        test("should return VersionDetails with correct values") {
            val subjects = List(10) { randomDownloadManifest() }

            for (manifest in subjects) {
                val details = manifest.toVersionDetails()
                details.versionID shouldBe manifest.versionID
                details.label shouldBe manifest.type.toVersionLabel()
                details.time shouldBe manifest.time.toTimestamp()
                details.releaseTime shouldBe manifest.releaseTime.toTimestamp()
                details.size shouldBe manifest.size
                details.sha1 shouldBe manifest.sha1
            }
        }
    }

    context("Long.(kibi/mebi/gibi)bytes Tests") {
        test("should return a MemoryAmount with the correct unit") {
            fixedSeedRandom.nextLong().bytes.unit shouldBe MemoryUnit.BYTES
            fixedSeedRandom.nextLong().kibibytes.unit shouldBe MemoryUnit.KIBIBYTES
            fixedSeedRandom.nextLong().mebibytes.unit shouldBe MemoryUnit.MEBIBYTES
            fixedSeedRandom.nextLong().gibibytes.unit shouldBe MemoryUnit.GIBIBYTES
        }

        test("should return a MemoryAmout with the correct amount") {
            val subjects = List(100) { fixedSeedRandom.nextLong() }

            for (long in subjects) {
                long.bytes.amount shouldBe long
                long.kibibytes.amount shouldBe long
                long.mebibytes.amount shouldBe long
                long.gibibytes.amount shouldBe long
            }
        }
    }

    context("MemoryAmount.toCommandString() Tests") {
        val subjects = List(10) {
            MemoryAmount {
                amount = fixedSeedRandom.nextLong(0, Long.MAX_VALUE)
                unit = MemoryUnit.from(fixedSeedRandom.nextInt(0, 4))
            }
        }

        test("should return string with amount") {
            for (memoryAmount in subjects) {
                memoryAmount.toCommandString() shouldContain memoryAmount.amount.toString()
            }
        }

        test("should return string with suffix") {
            for (memoryAmount in subjects) {
                val str = memoryAmount.toCommandString()
                when (memoryAmount.unit) {
                    MemoryUnit.BYTES -> str shouldMatch Regex("[0-9]+")
                    MemoryUnit.KIBIBYTES -> str shouldEndWith "k"
                    MemoryUnit.MEBIBYTES -> str shouldEndWith "m"
                    MemoryUnit.GIBIBYTES -> str shouldEndWith "g"
                    else -> fail("subjects should only contain valid memory units")
                }
            }
        }

        test("should throw for unrecognized memory units") {
            val errorSubjects = List(10) {
                MemoryAmount {
                    amount = fixedSeedRandom.nextLong(0, Long.MAX_VALUE)
                    unit = MemoryUnit.from(fixedSeedRandom.nextInt(4, Int.MAX_VALUE))
                }
            }

            for (memoryAmount in errorSubjects) {
                shouldThrow<IllegalArgumentException> {
                    memoryAmount.toCommandString()
                }
            }
        }
    }

    context("Terminal.readyMsg(String) Tests") {
        val terminalMock = mockk<Terminal>()
        val slot = slot<String>()
        every { terminalMock.println(message = capture(slot)) } just Runs

        test("should print ready message") {
            repeat(10) {
                val componentString = randomString()
                terminalMock.readyMsg(componentString)
                verify { terminalMock.println(message = any()) }
                slot.captured shouldContain componentString
                slot.captured shouldContain "ready"
            }
        }
    }
})