package com.example.core.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class JSONFile<T>(val location: File, private val serializer: KSerializer<T>, writeDefaults: Boolean = false) {
    private val json = Json {
        encodeDefaults = writeDefaults
        prettyPrint = true
    }
    private var stringContent: String
    private var objCache: T? = null

    init {
        stringContent = readFromFile(location)
    }

    constructor(location: String, serializer: KSerializer<T>, writeDefaults: Boolean = false)
            : this(File(location), serializer, writeDefaults)

    private fun cache(): T {
        return json.decodeFromString(serializer, stringContent).apply { objCache = this }
    }

    fun reload() {
        stringContent = readFromFile(location)
        objCache = null
    }

    fun get(): T {
        return objCache ?: cache()
    }

    fun set(content: T) {
        objCache = content
    }

    fun write() {
        if (objCache == null) return
        BufferedWriter(FileWriter(location, false)).apply {
            write(json.encodeToString(serializer, objCache!!).also { stringContent = it })
        }.close()
    }

    fun setAndCommit(content: T) {
        set(content)
        write()
    }

    fun performAndCommit(block: T.() -> T) {
        set(block(objCache ?: cache()))
        write()
    }
}