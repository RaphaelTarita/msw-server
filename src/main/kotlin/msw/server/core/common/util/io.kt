package msw.server.core.common.util

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.div

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

fun Path.renameTo(new: String): Path {
    return Files.move(this, resolveSibling(new))
}

fun Path.existsOrNull(): Path? {
    return if (Files.exists(this)) this else null
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

fun Path.runCommand(command: List<String>): Process {
    try {
        return ProcessBuilder(command)
            .directory(toFile())
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
    } catch (_: IOException) {
        throw IllegalArgumentException("command ${command.joinToString(" ")} is invalid")
    }
}

fun Process.addTerminationCallback(callback: Process.() -> Unit): Process {
    onExit().thenAcceptAsync(callback)
    return this
}