package com.example.core.common

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import java.io.File

class DirectoryTests : FunSpec({
    test("A directory called '$exists' should not fail the instantiation of a Directory class") {
        shouldNotThrowAny {
            Directory(exists)
        }
    }

    test("A directory called '$existsNot' should fail the instantiation of a Directory class") {
        shouldThrow<IllegalArgumentException> {
            Directory(existsNot)
        }
    }

    test("A directory called '$create' should be created when instantiating Directory(path, true)") {
        shouldNotThrowAny {
            Directory(create, true)
        }
        File(create).exists().shouldBeTrue()
    }

    test("A directory called '$existsNot' should not fail the instantiation Directory(path, require = false)") {
        shouldNotThrowAny {
            Directory(existsNot, require = false)
        }
    }

    test("A directory called '$exists' should contain some files obtainable via Directory(path).list()") {
        val files: Array<String>? = shouldNotThrowAny {
            Directory(exists).list()
        }
        files.shouldNotBeNull()
        files.shouldNotBeEmpty()
    }
})