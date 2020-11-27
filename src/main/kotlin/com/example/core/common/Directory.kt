package com.example.core.common

import java.io.File
import java.net.URI

class Directory : File {
    constructor(parent: File, child: String, create: Boolean = false, require: Boolean = true) : super(parent, child) {
        check(create, require)
    }

    constructor(uri: URI, create: Boolean = false, require: Boolean = true) : super(uri) {
        check(create, require)
    }

    constructor(pathname: String, create: Boolean = false, require: Boolean = true) : super(pathname) {
        check(create, require)
    }

    constructor(parent: String, child: String, create: Boolean = false, require: Boolean = true) : super(
        parent,
        child
    ) {
        check(create, require)
    }

    private fun condMkdir() {
        if (!exists()) {
            mkdir()
        }
    }

    private fun check(create: Boolean = false, require: Boolean = true) {
        if (require) {
            if (create) condMkdir()
            require(isDirectory) {
                "File $path is not a directory"
            }
        }
    }
}

fun File.toDirectory(): Directory {
    return Directory(path)
}