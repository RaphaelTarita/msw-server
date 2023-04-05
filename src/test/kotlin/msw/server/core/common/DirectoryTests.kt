package msw.server.core.common

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import java.io.File

class DirectoryTests : FunSpec({
    test("A directory called '$exists' should not fail the instantiation of a Directory class") {
        shouldNotThrowAny {
            Directory(exists)
            Directory(File(parentDir), existsName)
        }
    }

    test("A directory called '$existsNot' should fail the instantiation of a Directory class") {
        shouldThrow<IllegalArgumentException> {
            Directory(existsNot)
        }
        shouldThrow<IllegalArgumentException> {
            Directory(File(parentDir), existsNotName)
        }
    }

    test("A directory called '$create' should be created when instantiating Directory(path, true)") {
        val spy = File(create)
        shouldNotThrowAny {
            Directory(create, create = true)
        }
        spy.exists().shouldBeTrue()
        spy.delete()
        shouldNotThrowAny {
            Directory(File(parentDir), createName, create = true)
        }
        spy.exists().shouldBeTrue()
    }

    test("A directory called '$existsNot' should not fail the instantiation Directory(path, require = false)") {
        shouldNotThrowAny {
            Directory(existsNot, require = false)
            Directory(File(parentDir), existsNotName, require = false)

            Directory(existsNot, create = true, require = false)
            Directory(File(parentDir), existsNotName, create = true, require = false)
        }

        File(existsNot).exists().shouldBeFalse()
    }

    test("A directory called '$exists' should contain some files obtainable via Directory(path).list()") {
        val files: Array<String>? = shouldNotThrowAny {
            Directory(exists).list()
        }
        files.shouldNotBeNull()
        files.shouldNotBeEmpty()
    }

    test("File.toDirectory() should behave like Directory constructor") {
        shouldNotThrowAny {
            File(exists).toDirectory()
        }
        shouldThrow<IllegalArgumentException> {
            File(existsNot).toDirectory()
        }
    }
})