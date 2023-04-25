package msw.server.core.common.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.paths.exist
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.shouldContain
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import kotlin.io.path.div
import kotlin.io.path.name
import kotlin.io.path.writeText
import kotlinx.coroutines.future.await
import msw.server.core.common.randomString
import msw.server.core.common.tempdir

class IoTests : FunSpec({
    val exists: Path
    val testFile1: Path
    val testFile2: Path
    val testFile3: Path
    val parentDir = tempdir {
        exists = tempdir {
            testFile1 = tempfile()
            testFile2 = tempfile()
            testFile3 = tempfile()
        }
    }

    context("directory(Path, (String), Boolean, Boolean) Tests") {
        test("should not fail for existing directory") {
            val existsName = exists.name
            directory(exists) shouldBe exists
            directory(parentDir, existsName) shouldBe exists
        }

        test("should fail for non-existent directory") {
            val nonExistentName = "nonexistent1"
            shouldThrow<IllegalArgumentException> {
                directory(parentDir / nonExistentName)
            }
            shouldThrow<IllegalArgumentException> {
                directory(parentDir, nonExistentName)
            }
        }

        test("should create non-existent directory when specifying 'create = true'") {
            val createName = "create"
            val subject = parentDir / createName

            directory(subject, create = true) shouldBe subject

            subject should exist()
            Files.delete(subject)

            directory(parentDir, createName, create = true) shouldBe subject

            subject should exist()
        }

        test("should not fail for non-existent directory when specifying 'require = false'") {
            val nonExistentName = "nonexistent2"
            val subject = parentDir / nonExistentName

            directory(subject, require = false) shouldBe subject
            directory(parentDir, nonExistentName, require = false) shouldBe subject

            directory(subject, create = true, require = false) shouldBe subject
            directory(parentDir, nonExistentName, create = true, require = false) shouldBe subject

            subject shouldNot exist()
        }
    }

    context("Path.renameTo(String) Tests") {
        test("should rename existing file") {
            val newName = "testFileRenamed"
            val expected = exists / newName

            testFile1.renameTo(newName) shouldBe expected

            testFile1 shouldNot exist()
            expected should exist()
        }

        test("should not fail when renaming to the same name") {
            val fileName = testFile2.name

            testFile2.renameTo(fileName) shouldBe testFile2

            testFile2 should exist()
        }

        test("should fail when renaming non-existent file") {
            val subject = exists / "nonexistent3"

            shouldThrow<NoSuchFileException> {
                subject.renameTo("testFileRenamed")
            }
        }
    }

    context("Path.existsOrNull() Tests") {
        test("should return path for existing files") {
            exists.existsOrNull() shouldBe exists
        }

        test("should return null for non-existent files") {
            val subject = exists / "nonexistent4"
            subject.existsOrNull() should beNull()
        }
    }

    context("Path.sha1() Tests") {
        fun testWithParameters(stringSize: Int, bufferSize: Int? = null) {
            val str = randomString(
                stringSize,
                '-', '_', ' ', '\t', '\n', '{', '}', '[', ']', ':', '"'
            )
            val md = MessageDigest.getInstance("SHA-1")
            val expected = md.digest(str.toByteArray()).toHexString()

            testFile3.writeText(str)
            if (bufferSize == null) {
                testFile3.sha1() shouldBe expected
            } else {
                testFile3.sha1(bufferSize) shouldBe expected
            }
        }

        test("should return SHA-1 of file contents") {
            testWithParameters(8192)
        }

        test("should work with small buffer sizes") {
            testWithParameters(1024, 1)
        }

        test("should work with big buffer sizes") {
            testWithParameters(2048, 8192)
        }
    }

    context("Path.runCommand(List<String>) Tests") {
        val expected = "File#toString() was called"
        val mockPath = mockk<Path>(relaxed = true)
        val mockFile = mockk<File>()

        beforeEach {
            clearMocks(mockPath, mockFile)
            every { mockPath.toFile() } returns mockFile
        }

        test("should call File#toString()") {
            every { mockFile.toString() } throws AssertionError(expected)
            val command = List(10) { randomString(5) }

            shouldThrowWithMessage<AssertionError>(expected) {
                mockPath.runCommand(command)
            }

            verify { mockPath.toFile() }
            verify { mockFile.toString() }

        }

        test("should rethrow IOExceptions as IllegalArgumentExceptions") {
            every { mockFile.toString() } throws IOException(expected)
            val command = List(10) { randomString(5) }

            shouldThrow<IllegalArgumentException> {
                mockPath.runCommand(command)
            }.message shouldContain command.joinToString(" ")

            verify { mockPath.toFile() }
            verify { mockFile.toString() }
        }
    }

    context("Process.addTerminationCallback(Process.() -> Unit) Tests") {
        val future = CompletableFuture<Process>()
        val mockProcess = mockk<Process>(relaxed = true)
        every { mockProcess.onExit() } returns future

        test("should execute callback when process finishes") {
            var spy = false
            mockProcess.addTerminationCallback {
                this shouldBe mockProcess
                spy = true
            }

            spy shouldBe false

            future.complete(mockProcess)
            future.await()

            spy shouldBe true
        }
    }
})