package msw.server.core.common

import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.properties.Properties

@ExperimentalSerializationApi
open class OpenProperties(val props: Properties) : SerialFormat by props

@ExperimentalSerializationApi
sealed class StringProperties(internal val config: PropertiesConf) :
    OpenProperties(Properties(config.module)),
    StringFormat {

    companion object Default : StringProperties(PropertiesConf())

    @OptIn(InternalSerializationApi::class)
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val result = mutableMapOf<String, String>()
        val separators = setOf('=', ':', ' ')
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
            for (i in kend..line.lastIndex) {
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
        return builder.toString()
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
    var commentChar: CommentChar = from.commentChar
    var spacesBeforeSeparator: Int = from.spacesBeforeSeparator
    var spacesAfterSeparator: Int = from.spacesAfterSeparator
    var module: SerializersModule = from.module

    internal fun build(): PropertiesConf {
        return PropertiesConf(
            lineSeparator,
            kvSeparator,
            commentChar,
            spacesBeforeSeparator,
            spacesAfterSeparator,
            module
        )
    }
}

@ExperimentalSerializationApi
internal data class PropertiesConf(
    val lineSeparator: LineSeparator = LineSeparator.LF,
    val kvSeparator: KVSeparator = KVSeparator.EQUALS,
    val commentChar: CommentChar = CommentChar.HASHTAG,
    val spacesBeforeSeparator: Int = 0,
    val spacesAfterSeparator: Int = 0,
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

private fun String.logicalLines(): List<String> {
    val commentFilter = "[ \\t\\f]*[#!].*".toRegex()
    val result = lines().filterNot {
        it.isBlank() || commentFilter.matches(it)
    }.toMutableList()

    var i = 0
    while (i < result.size) {
        if (result[i].endsWith('\\')) {
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