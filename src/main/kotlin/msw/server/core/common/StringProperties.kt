package msw.server.core.common

import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.properties.Properties
import java.security.MessageDigest
import kotlin.math.min

private val nonBlankSeparators = setOf('=', ':')
private val separators = nonBlankSeparators + ' '
private val commentFilter = "[ \\t\\f]*[#!].*".toRegex()

@ExperimentalSerializationApi
open class OpenProperties(val props: Properties) : SerialFormat by props

@ExperimentalSerializationApi
sealed class StringProperties(internal val config: PropertiesConf) :
    OpenProperties(Properties(config.module)),
    StringFormat {

    companion object Default : StringProperties(PropertiesConf())

    private fun put(k: String, v: Any, builder: StringBuilder) {
        builder.append(k.escapeProperties())
        repeat(config.spacesBeforeSeparator) {
            builder.append(' ')
        }
        builder.append(config.kvSeparator.c)
        repeat(config.spacesAfterSeparator) {
            builder.append(' ')
        }
        builder.append(v.toString().escape(*config.lineSeparator.chars()))
            .append(config.lineSeparator.s)
    }

    private fun putComment(str: String, builder: StringBuilder) {
        str.lines().forEach {
            repeat(config.spacesBeforeCommentChar) {
                builder.append(' ')
            }
            builder.append(config.commentChar.c)
            repeat(config.spacesAfterCommentChar) {
                builder.append(' ')
            }
            builder.append(it)
                .append(config.lineSeparator.s)
        }
    }

    @OptIn(InternalSerializationApi::class)
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val result = mutableMapOf<String, String>()
        for (line in string.logicalLines()) {
            var skipNext = false
            var kend = line.length
            for (i in line.indices) {
                if (skipNext) continue
                if (line[i] == '\\') skipNext = true
                if (line[i] in separators) {
                    kend = i
                    break
                }
            }

            var vbegin = line.length
            var separatorFound = false
            for (i in kend..line.lastIndex) {
                if (separatorFound && line[i] != ' ') {
                    vbegin = i
                    break
                }
                if (line[i] in nonBlankSeparators) {
                    separatorFound = true
                }
                if (line[i] !in separators) {
                    vbegin = i
                    break
                }
            }

            result[line.substring(0, kend)] = line.substring(vbegin)
        }
        val engine = ConvertEngine(result, deserializer.descriptor)
        return engine.decodeSerializableValue(deserializer)
    }

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val map = props.encodeToMap(serializer, value)
        val builder = StringBuilder()
        for ((k, v) in map) {
            put(k, v, builder)
        }
        return builder.toString()
    }

    fun <T> encodeWithComments(serializer: SerializationStrategy<T>, value: T, comments: Map<Int, String>): String {
        val map = props.encodeToMap(serializer, value)
        val builder = StringBuilder()
        var lline = 0
        for ((k, v) in map) {
            comments.ifContainsKey(lline) { (_, str) ->
                putComment(str, builder)
            }
            put(k, v, builder)
            lline++
        }
        comments.filterKeys { it >= lline }
            .toSortedMap()
            .forEach { (_, str) ->
                putComment(str, builder)
            }
        return builder.toString()
    }

    fun <T> decodeWithComments(deserializer: DeserializationStrategy<T>, string: String): Pair<T, Map<Int, String>> {
        return decodeFromString(deserializer, string) to string.indexedComments(config)
    }

    fun <T> semanticHash(
        deserializer: DeserializationStrategy<T>,
        string: String,
        considerComments: Boolean = false,
        algorithm: String = "SHA-1"
    ): String {
        val md = MessageDigest.getInstance(algorithm)

        val comments: Map<Int, String>?
        val obj = if (considerComments) {
            val pair = decodeWithComments(deserializer, string)
            comments = pair.second
            pair.first
        } else {
            comments = null
            decodeFromString(deserializer, string)
        }

        md.update(obj.toByteArray())
        if (considerComments) {
            md.update(comments.toByteArray())
        }
        return md.digest().toHexString()
    }
}

@ExperimentalSerializationApi
private class StringPropertiesImpl(conf: PropertiesConf) : StringProperties(conf)

@ExperimentalSerializationApi
fun StringProperties(
    from: StringProperties = StringProperties,
    builderAction: PropertiesBuilder.() -> Unit
): StringProperties {
    val builder = PropertiesBuilder(from.config)
    builder.builderAction()
    val conf = builder.build()
    return StringPropertiesImpl(conf)
}

@ExperimentalSerializationApi
class PropertiesBuilder internal constructor(from: PropertiesConf) {

    var lineSeparator: LineSeparator = from.lineSeparator
    var kvSeparator: KVSeparator = from.kvSeparator
    var spacesBeforeSeparator: Int = from.spacesBeforeSeparator
    var spacesAfterSeparator: Int = from.spacesAfterSeparator
    var commentChar: CommentChar = from.commentChar
    var spacesBeforeCommentChar = from.spacesBeforeCommentChar
    var spacesAfterCommentChar: Int = from.spacesAfterCommentChar
    var module: SerializersModule = from.module

    internal fun build(): PropertiesConf {
        return PropertiesConf(
            lineSeparator,
            kvSeparator,
            spacesBeforeSeparator,
            spacesAfterSeparator,
            commentChar,
            spacesBeforeCommentChar,
            spacesAfterCommentChar,
            module
        )
    }
}

@ExperimentalSerializationApi
internal data class PropertiesConf(
    val lineSeparator: LineSeparator = LineSeparator.LF,
    val kvSeparator: KVSeparator = KVSeparator.EQUALS,
    val spacesBeforeSeparator: Int = 0,
    val spacesAfterSeparator: Int = 0,
    val commentChar: CommentChar = CommentChar.HASHTAG,
    val spacesBeforeCommentChar: Int = 0,
    val spacesAfterCommentChar: Int = 1,
    val module: SerializersModule = EmptySerializersModule
)

enum class LineSeparator(val s: String) {
    LF("\n"),
    CR("\r"),
    CRLF("\r\n");

    fun chars(): CharArray {
        return s.toCharArray()
    }
}

enum class KVSeparator(val c: Char) {
    EQUALS('='),
    COLON(':'),
    SPACE(' ')
}

enum class CommentChar(val c: Char) {
    HASHTAG('#'),
    EXCLAMATION_MARK('!')
}

private fun String.escapeProperties() = escape('\\').escape('=', ':', ' ', '#', '!')

private fun String.logicalLines(preserveComments: Boolean = false): List<String> {
    val result = lines().filterNot {
        it.isBlank() || (!preserveComments && commentFilter.matches(it))
    }.toMutableList()

    var i = 0
    while (i < result.size) {
        if (!commentFilter.matches(result[i]) && result[i].endsWith('\\')) {
            result[i] = result[i].dropLast(1)
            if (i != result.lastIndex) {
                result[i] += '\n' + result[i + 1]
                result.removeAt(i + 1)
            }
        }
        i++
    }

    return result
}

@ExperimentalSerializationApi
private fun String.indexedComments(config: PropertiesConf): Map<Int, String> {
    val res = mutableMapOf<Int, String>()
    val llines = logicalLines(true).toMutableList()

    val accumulator = mutableListOf<String>()
    var i = 0
    while (i < llines.size) {
        if (commentFilter.matches(llines[i])) {
            accumulator.add(cleanupComment(llines.removeAt(i), config.spacesAfterCommentChar))
        } else {
            if (accumulator.isNotEmpty()) {
                res[i] = accumulator.joinToString(config.lineSeparator.s)
                accumulator.clear()
            }
            i++
        }
    }

    if (accumulator.isNotEmpty()) {
        res[llines.size] = accumulator.joinToString(config.lineSeparator.s)
    }

    return res
}

private fun cleanupComment(comment: String, spacesAfterCommentChar: Int): String {
    val trimmed = comment.trimStart()
    return trimmed.substring(
        min(
            spacesAfterCommentChar + 1,
            trimmed.drop(1).indexOfFirst { it != ' ' } + 1
        )
    )
}