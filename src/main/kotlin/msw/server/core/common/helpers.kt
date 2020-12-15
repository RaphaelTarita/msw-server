package msw.server.core.common

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.math.min

val HEX = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

fun Long.coerceToInt(): Int {
    return min(this, Int.MAX_VALUE.toLong()).toInt()
}

fun String.isNumeric(): Boolean {
    return toIntOrNull() != null
}

fun composePath(root: Path, child: Path): Path {
    return root.resolve(child)
}

fun composePath(root: Path, child: String): Path {
    return root.resolve(child)
}

fun composePath(root: Directory, child: Path): Path {
    return composePath(root.toPath(), child)
}

fun composePath(root: Directory, child: String): Path {
    return composePath(root.toPath(), child)
}

fun readFromFile(location: File): String {
    return String(Files.readAllBytes(location.toPath()))
}

fun readFromPath(path: Path): String {
    return readFromFile(path.toFile())
}

fun readFromPath(path: String): String {
    return readFromPath(Paths.get(path))
}

fun Path.renameTo(new: String): Path {
    ignoreError<FileAlreadyExistsException> {
        Files.move(this, resolveSibling(new))
    }
    return this
}

fun Path.sha1(bufferSize: Int = 4096): String {
    if (!Files.exists(this)) return ""
    val md = MessageDigest.getInstance("SHA-1")
    val stream = Files.newInputStream(this)
    val buf = ByteArray(bufferSize)
    do {
        val amount = stream.read(buf, 0, bufferSize)
        if (amount > 0) {
            md.update(buf.sliceArray(0 until amount))
        }
    } while (amount >= 0)
    return md.digest().toHexString()
}

fun File.sha1(bufferSize: Int = 4096) = toPath().sha1(bufferSize)

inline fun <reified E> ignoreError(block: () -> Unit) {
    try {
        block()
    } catch (exc: Throwable) {
        if (!E::class.isInstance(exc)) throw exc
    }
}

inline fun <reified E : Throwable, R> nullIfError(block: () -> R): R? {
    return try {
        block()
    } catch (exc: Throwable) {
        if (E::class.isInstance(exc)) null else throw exc
    }
}

fun existsOrNull(file: File): File? {
    return if (file.exists()) file else null
}

fun existsOrNull(file: Path): Path? {
    return if (Files.exists(file)) file else null
}

fun String.replaceMultiple(map: Map<String, String>, ignoreCase: Boolean = false): String {
    var ret = this
    for ((old, new) in map) {
        ret = ret.replace(old, new, ignoreCase)
    }
    return ret
}

fun String.replaceMultiple(vararg entries: Pair<String, String>, ignoreCase: Boolean = false): String {
    return replaceMultiple(mapOf(*entries), ignoreCase)
}

fun String.escape(vararg charsToEscape: Char, ignoreCase: Boolean = false): String {
    return replaceMultiple(charsToEscape.associate { it.toString() to "\\" + it }, ignoreCase)
}

fun String.unescape(vararg charsToUnescape: Char, ignoreCase: Boolean = false): String {
    return replaceMultiple(charsToUnescape.associate { "\\" + it to it.toString() }, ignoreCase)
}

fun <K, V, U> errorJoin(first: Map<K, V>, second: Map<K, U>): Map<K, Pair<V, U>> {
    require(first.keys == second.keys) {
        "attempted to merge two maps with different key sets"
    }
    return innerJoin(first, second)
}

fun <K, V, U> innerJoin(first: Map<K, V>, second: Map<K, U>): Map<K, Pair<V, U>> {
    val result = mutableMapOf<K, Pair<V, U>>()
    for ((key, firstValue) in first) {
        val secondValue = second[key]
        if (secondValue != null) {
            result[key] = firstValue to secondValue
        }
    }
    return result
}

fun <K, V, U> leftOuterJoin(first: Map<K, V>, second: Map<K, U>): Map<K, Pair<V, U?>> {
    val result = mutableMapOf<K, Pair<V, U?>>()
    for ((key, firstValue) in first) {
        result[key] = firstValue to second[key]
    }
    return result
}

fun <K, V, U> rightOuterJoin(first: Map<K, V>, second: Map<K, U>): Map<K, Pair<V?, U>> {
    val result = mutableMapOf<K, Pair<V?, U>>()
    for ((key, secondValue) in second) {
        result[key] = first[key] to secondValue
    }
    return result
}

fun <K, V, U> fullOuterJoin(first: Map<K, V>, second: Map<K, U>): Map<K, Pair<V?, U?>> {
    val result = mutableMapOf<K, Pair<V?, U?>>()
    val secondMutable = second.toMutableMap()
    for ((key, value) in first) {
        result[key] = value to secondMutable.remove(key)
    }

    for ((key, value) in secondMutable) {
        result[key] = null to value
    }

    return result
}

fun <K, V, R> Map<K, V>.ifContainsKey(key: K, action: (Pair<K, V>) -> R): R? {
    return if (containsKey(key)) {
        action(key to this.getValue(key))
    } else {
        null
    }
}

fun <K, V, R> Map<K, V>.ifContainsValue(value: V, action: (Pair<K, V>) -> R): List<R> {
    return if (containsValue(value)) {
        val list = mutableListOf<R>()
        filterValues { it == value }.forEach {
            list.add(action(it.toPair()))
        }
        list
    } else {
        emptyList()
    }
}

fun ByteArray.toHexString(): String {
    val hexChars = CharArray(size * 2)
    for (i in indices) {
        val v = this[i].toInt() and 0xFF
        hexChars[i * 2] = HEX[v ushr 4]
        hexChars[i * 2 + 1] = HEX[v and 0x0F]
    }
    return String(hexChars)
}

fun Any?.toByteArray(): ByteArray {
    if (this == null) return ByteArray(0)

    val baos = ByteArrayOutputStream()
    ObjectOutputStream(baos).apply { writeObject(this) }.close()
    return baos.toByteArray()
}

fun Directory.runCommand(command: List<String>): Process {
    try {
        return ProcessBuilder(command)
            .directory(this)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
    } catch (ex: IOException) {
        throw IllegalArgumentException("command $command is invalid")
    }
}

fun Directory.runCommand(command: String): Process = runCommand(command.commandParts())

fun String.commandParts(): List<String> {
    if (isEmpty()) return emptyList()
    var remaining = this
    val res = mutableListOf<String>()
    val space = "\\s".toRegex()
    val quote = "[\"']".toRegex()

    while (remaining.isNotEmpty()) {
        var candidate = remaining.substring(0, remaining.indexOf(space, notFound = remaining.length))
        if (candidate.contains(quote)) {
            val strippedRemaining = remaining.removePrefix(candidate)
            candidate += strippedRemaining.substring(0, strippedRemaining.indexOf(quote) + 1)
        }
        res.add(candidate)
        remaining = remaining.removePrefix(candidate).trimStart()
    }
    return res
}

fun String.indexOf(regex: Regex, startIndex: Int = 0, notFound: Int = 0): Int {
    return regex.find(this.substring(startIndex))?.range?.start ?: notFound
}

fun hashCode(vararg vals: Any?, prime: Int = 31): Int {
    var res = 0
    for (v in vals) {
        res += v.hashCode()
        res *= prime
    }
    return res
}

val Long.b: MemoryAmount
    get() = MemoryAmount(this, MemoryUnit.BYTES)
val Long.k: MemoryAmount
    get() = MemoryAmount(this, MemoryUnit.KIBIBYTES)
val Long.m
    get() = MemoryAmount(this, MemoryUnit.MEBIBYTES)
val Long.g: MemoryAmount
    get() = MemoryAmount(this, MemoryUnit.GIBIBYTES)









