package msw.server.core.model

import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.util.Locale
import msw.server.core.common.readFromPath

class EULA(val location: Path) {
    private val urlRegex =
        Regex("EULA\\s?\\((https?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b[-a-zA-Z0-9@:%_+.~#?&/=]*)\\)")
    private val dateTimeRegex = Regex("\\w{3}\\s\\w{3}\\s\\d{2}\\s\\d{2}(?::\\d{2}){2}\\s\\w{2,4}\\s\\d{4}")
    private val dateTimeFormat = DateTimeFormatterBuilder()
        .appendPattern("E MMM dd HH:mm:ss ")
        .appendZoneText(TextStyle.SHORT, setOf(ZoneId.of("Europe/Vienna")))
        .appendPattern(" yyyy")
        .toFormatter(Locale.ENGLISH)

    private fun change(to: String) {
        val content = readFromPath(location)
            .replace(Regex("eula=(?:true|false)"), "eula=$to")
        Files.newBufferedWriter(location, StandardOpenOption.WRITE).apply { write(content) }.close()
    }

    fun status(): Boolean {
        return filter(readFromPath(location)).contains("eula=true")
    }

    fun eulaURL(): URL {
        return URL(
            urlRegex.find(readFromPath(location))
                ?.groupValues
                ?.get(1)
                ?: "about:blank"
        )
    }

    fun timestamp(): LocalDateTime {
        return LocalDateTime.parse(
            dateTimeRegex.find(readFromPath(location))?.value
                ?: "Thu Jan 01 00:00:00 UTC 1970",
            dateTimeFormat
        )
    }

    fun agree() {
        change("true")
    }

    fun disagree() {
        change("false")
    }

}

private fun filter(input: String): String {
    return input.lines()
        .filter { !it.startsWith('#') }
        .joinToString(System.lineSeparator())
}