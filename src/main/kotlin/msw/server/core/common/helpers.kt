package msw.server.core.common

import com.toasttab.protokt.Timestamp
import io.grpc.MethodDescriptor
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.ServerCalls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.StringFormat
import msw.server.core.versions.DownloadManifest
import msw.server.core.versions.model.VersionType
import msw.server.core.versions.model.Versions
import msw.server.core.watcher.MemoryAmount
import msw.server.core.watcher.MemoryUnit
import msw.server.rpc.versions.VersionDetails
import msw.server.rpc.versions.VersionLabel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.time.OffsetDateTime
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

val HEX = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

fun Long.coerceToInt(): Int {
    return min(this, Int.MAX_VALUE.toLong()).toInt()
}

fun sign(i: Int): Int {
    return if (i > 0) 1 else if (i < 0) -1 else 0
}

fun sign(l: Long): Int {
    return if (l > 0) 1 else if (l < 0) -1 else 0
}

fun invertInsertionPoint(inverted: Int): Int {
    return -(inverted + 1)
}

fun String.isNumeric(): Boolean {
    return toIntOrNull() != null
}

fun String.truncate(maxlen: Int, suffix: String = "..."): String {
    val internalMaxlen = maxlen - suffix.length
    return if (length < internalMaxlen) this else substring(0, internalMaxlen) + suffix
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

fun readFromPath(path: Path): String {
    return String(Files.readAllBytes(path))
}

fun readFromPath(path: String): String {
    return readFromPath(Paths.get(path))
}

fun readFromFile(location: File): String {
    return readFromPath(location.toPath())
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

@OptIn(ExperimentalSerializationApi::class)
fun semanticEquivalence(
    lop: String,
    rop: String,
    format: StringFormat,
    semantics: DeserializationStrategy<*>
): Boolean {
    return format.decodeFromString(semantics, lop) == format.decodeFromString(semantics, rop)
}

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

fun <K, V> Iterable<Pair<K, V>>.toMap(onDuplicates: (Pair<K, V>) -> Unit): Map<K, V> {
    val res = LinkedHashMap<K, V>()
    for ((k, v) in this) {
        if (res.containsKey(k)) {
            onDuplicates(k to v)
        } else {
            res[k] = v
        }
    }
    return res
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
    ObjectOutputStream(baos).apply { writeObject(this@toByteArray) }.close()
    return baos.toByteArray()
}

fun DownloadManifest.toVersionDetails() = VersionDetails {
    versionID = this@toVersionDetails.versionID
    label = this@toVersionDetails.type.toVersionLabel()
    time = this@toVersionDetails.time.toTimestamp()
    releaseTime = this@toVersionDetails.releaseTime.toTimestamp()
    size = this@toVersionDetails.size
    sha1 = this@toVersionDetails.sha1
}

fun VersionType.toVersionLabel(): VersionLabel {
    return when (this) {
        VersionType.OLD_ALPHA -> VersionLabel.ALPHA
        VersionType.OLD_BETA -> VersionLabel.BETA
        VersionType.RELEASE -> VersionLabel.RELEASE
        VersionType.SNAPSHOT -> VersionLabel.SNAPSHOT
        VersionType.ALL -> throw IllegalStateException("VersionType was '$this', which is an illegal value (only used for filtering)")
    }
}

fun OffsetDateTime.toTimestamp() = Timestamp {
    seconds = this@toTimestamp.toEpochSecond()
    nanos = this@toTimestamp.nano
}

fun <T, U> comparatorForNested(innerComparator: Comparator<U>, selector: (T) -> U): Comparator<T> {
    return Comparator { o1, o2 ->
        when (val ncomp = compareNullable(o1, o2)) {
            is NullableCompare.RESULT -> ncomp.res
            is NullableCompare.CONTINUE -> innerComparator.compare(selector(ncomp.o1), selector(ncomp.o2))
        }
    }
}

fun versionComparatorFor(rootDocument: Versions): Comparator<String> {
    val ordered = rootDocument.versions.sortedWith(comparatorForNested(OffsetDateTime.timeLineOrder()) { it.releaseTime })
    return Comparator { o1, o2 ->
        when (val ncomp = compareNullable(o1.ifEmpty { null }, o2.ifEmpty { null })) {
            is NullableCompare.RESULT -> ncomp.res
            is NullableCompare.CONTINUE -> {
                val i1 = ordered.indexOfFirst { it.id == o1 }
                val i2 = ordered.indexOfFirst { it.id == o2 }
                require(i1 != -1) { "Unknown version id '$o1'" }
                require(i2 != -1) { "Unknown version id '$o2'" }
                i1 - i2
            }
        }
    }
}

fun <T> compareNullable(n1: T?, n2: T?): NullableCompare<T> {
    return if (n1 == null) {
        if (n2 == null) NullableCompare.RESULT(0)
        else NullableCompare.RESULT(-1)
    } else {
        if (n2 == null) NullableCompare.RESULT(1)
        else NullableCompare.CONTINUE(n1, n2)
    }
}

sealed class NullableCompare<T> {
    data class RESULT<T>(val res: Int) : NullableCompare<T>()
    data class CONTINUE<T>(val o1: T, val o2: T) : NullableCompare<T>()
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

fun Process.addTerminationCallback(scope: CoroutineScope, callback: Process.() -> Unit): Process {
    try {
        exitValue()
        callback()
    } catch (exc: IllegalThreadStateException) {
        scope.launch {
            ignoreError<InterruptedException> {
                withContext(Dispatchers.IO) { waitFor() }
            }
            callback()
        }
    }
    return this
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

val Long.bytes: MemoryAmount
    get() = MemoryAmount {
        amount = this@bytes
        unit = MemoryUnit.BYTES
    }
val Long.kibibytes: MemoryAmount
    get() = MemoryAmount {
        amount = this@kibibytes
        unit = MemoryUnit.KIBIBYTES
    }
val Long.mebibytes: MemoryAmount
    get() = MemoryAmount {
        amount = this@mebibytes
        unit = MemoryUnit.MEBIBYTES
    }
val Long.gibibytes: MemoryAmount
    get() = MemoryAmount {
        amount = this@gibibytes
        unit = MemoryUnit.GIBIBYTES
    }

fun MemoryAmount.toCommandString(): String {
    return "$amount${
        when (unit) {
            is MemoryUnit.BYTES -> ""
            is MemoryUnit.KIBIBYTES -> "k"
            is MemoryUnit.MEBIBYTES -> "m"
            is MemoryUnit.GIBIBYTES -> "g"
            is MemoryUnit.UNRECOGNIZED -> throw IllegalArgumentException("Unrecognized memory unit: '$unit'")
        }
    }"
}

fun <T, U> AbstractCoroutineServerImpl.unary(
    context: CoroutineContext,
    descriptor: MethodDescriptor<T, U>,
    implementation: suspend (request: T) -> U
) = ServerCalls.unaryServerMethodDefinition(context, descriptor, implementation)

fun <T, U> AbstractCoroutineServerImpl.clientStream(
    context: CoroutineContext,
    descriptor: MethodDescriptor<T, U>,
    implementation: suspend (requests: Flow<T>) -> U
) = ServerCalls.clientStreamingServerMethodDefinition(context, descriptor, implementation)

fun <T, U> AbstractCoroutineServerImpl.serverStream(
    context: CoroutineContext,
    descriptor: MethodDescriptor<T, U>,
    implementation: (request: T) -> Flow<U>
) = ServerCalls.serverStreamingServerMethodDefinition(context, descriptor, implementation)

fun <T, U> AbstractCoroutineServerImpl.bidiStream(
    context: CoroutineContext,
    descriptor: MethodDescriptor<T, U>,
    implementation: (requests: Flow<T>) -> Flow<U>
) = ServerCalls.bidiStreamingServerMethodDefinition(context, descriptor, implementation)








