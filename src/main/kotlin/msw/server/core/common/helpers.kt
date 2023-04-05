package msw.server.core.common

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import com.toasttab.protokt.Timestamp
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.OffsetDateTime
import kotlin.io.path.div
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.StringFormat
import msw.server.core.versions.DownloadManifest
import msw.server.core.versions.model.VersionType
import msw.server.core.versions.model.Versions
import msw.server.rpc.versions.VersionDetails
import msw.server.rpc.versions.VersionLabel

val HEX = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

fun Long.coerceToInt(): Int {
    return max(min(this, Int.MAX_VALUE.toLong()), Int.MIN_VALUE.toLong()).toInt()
}

fun invertInsertionPoint(inverted: Int): Int {
    return -(inverted + 1)
}

fun String.truncate(maxlen: Int, suffix: String = "..."): String {
    val internalMaxlen = maxlen - suffix.length
    return if (length <= maxlen) this else substring(0, internalMaxlen) + suffix
}

fun directory(path: Path, create: Boolean = false, require: Boolean = true): Path {
    if (require) {
        if (!Files.exists(path)) {
            if (create) {
                Files.createDirectory(path)
            } else {
                throw IllegalArgumentException("$path does not exist")
            }
        }
        require(Files.isDirectory(path)) { "$path is not a directory" }
    }
    return path
}

fun directory(
    parent: Path,
    child: String,
    create: Boolean = false,
    require: Boolean = true
) = directory(parent / child, create, require)

fun readFromPath(path: Path): String {
    return String(Files.readAllBytes(path))
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

fun Path.existsOrNull(): Path? {
    return if (Files.exists(this)) this else null
}

fun String.replaceMultiple(map: Map<String, String>, ignoreCase: Boolean = false): String {
    var ret = this
    for ((old, new) in map) {
        ret = ret.replace(old, new, ignoreCase)
    }
    return ret
}

fun String.escape(vararg charsToEscape: Char, ignoreCase: Boolean = false): String {
    return replaceMultiple(charsToEscape.associate { it.toString() to "\\" + it }, ignoreCase)
}

fun <K, V, R> Map<K, V>.ifContainsKey(key: K, action: (Pair<K, V>) -> R): R? {
    return if (containsKey(key)) {
        action(key to this.getValue(key))
    } else {
        null
    }
}

fun <T, K> Sequence<T>.distinctBy(selector: (T) -> K, onDuplicates: (T) -> Unit): Sequence<T> {
    val observed = mutableSetOf<K>()
    return sequence {
        forEach {
            val select = selector(it)
            if (select in observed) {
                onDuplicates(it)
            } else {
                observed += select
                yield(it)
            }
        }
        observed.clear()
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

fun Path.runCommand(command: List<String>): Process {
    try {
        return ProcessBuilder(command)
            .directory(toFile())
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
    } catch (ex: IOException) {
        throw IllegalArgumentException("command $command is invalid")
    }
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


fun Terminal.readyMsg(component: String) {
    println(TextColors.green("✓ $component initialized and ready."))
}







