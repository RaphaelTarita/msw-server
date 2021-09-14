package msw.server.core.common

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class JSONFile<T>(val location: Path, private val serializer: KSerializer<T>, writeDefaults: Boolean = false) {
    private val json = Json {
        encodeDefaults = writeDefaults
        prettyPrint = true
    }
    private var stringContent: String
    private var objCache: T? = null

    init {
        stringContent = readFromPath(location)
    }

    constructor(location: String, serializer: KSerializer<T>, writeDefaults: Boolean = false)
            : this(Paths.get(location), serializer, writeDefaults)

    private fun cache(): T {
        return json.decodeFromString(serializer, stringContent).apply { objCache = this }
    }

    fun reload() {
        stringContent = readFromPath(location)
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
        Files.newBufferedWriter(location, StandardOpenOption.WRITE).apply {
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