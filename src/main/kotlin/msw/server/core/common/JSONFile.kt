package msw.server.core.common

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.readText
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class JSONFile<T>(val location: Path, private val serializer: KSerializer<T>, initial: T? = null, writeDefaults: Boolean = false) {
    private val json = Json {
        encodeDefaults = writeDefaults
        prettyPrint = true
    }
    private var stringContent: String
    private var objCache: T? = null

    init {
        if (initial != null) {
            setAndCommit(initial)
        }

        stringContent = location.readText()
    }

    private fun cache(): T {
        return json.decodeFromString(serializer, stringContent).apply { objCache = this }
    }

    fun reload() {
        stringContent = location.readText()
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
        Files.newBufferedWriter(location, StandardOpenOption.WRITE, StandardOpenOption.CREATE).use { writer ->
            writer.write(json.encodeToString(serializer, objCache!!).also { stringContent = it })
        }
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