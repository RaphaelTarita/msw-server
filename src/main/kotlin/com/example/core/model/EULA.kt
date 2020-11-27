package com.example.core.model

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.util.*

class EULA(val location: File) {
    private val urlRegex =
        Regex("EULA\\s?\\((https?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b[-a-zA-Z0-9@:%_+.~#?&/=]*)\\)")
    private val dateTimeRegex = Regex("\\w{3}\\s\\w{3}\\s\\d{2}\\s\\d{2}(?::\\d{2}){2}\\s\\w{2,4}\\s\\d{4}")
    private val dateTimeFormat = DateTimeFormatterBuilder()
        .appendPattern("E MMM dd HH:mm:ss ")
        .appendZoneText(TextStyle.SHORT, setOf(ZoneId.of("Europe/Vienna")))
        .appendPattern(" yyyy")
        .toFormatter(Locale.ENGLISH)

    private fun change(to: String) {
        val content = Scanner(location)
            .useDelimiter("\\Z")
            .next()
            .replace(Regex("eula=(?:true|false)"), "eula=$to")
        BufferedWriter(FileWriter(location, false)).apply { write(content) }.close()
    }

    fun status(): Boolean {
        return filter(Scanner(location)).contains("eula=true")
    }

    fun eulaURL(): URL {
        var closeThis: Scanner? = null
        return URL(
            urlRegex.find(Scanner(location).apply { closeThis = this }.useDelimiter("\\Z").next())
                ?.groupValues
                ?.get(1)
                ?: "about:blank"
        ).also { closeThis?.close() }
    }

    fun timestamp(): LocalDateTime {
        var closeThis: Scanner? = null
        return LocalDateTime.parse(
            dateTimeRegex.find(Scanner(location).apply { closeThis = this }.useDelimiter("\\Z").next())?.value
                ?: "Thu Jan 01 00:00:00 UTC 1970",
            dateTimeFormat
        ).also { closeThis?.close() }
    }

    fun agree() {
        change("true")
    }

    fun disagree() {
        change("false")
    }

}

private fun filter(reader: Scanner): String {
    return reader.useDelimiter("\\Z").next()
        .lines()
        .filter { !it.startsWith('#') }
        .joinToString(System.lineSeparator())
        .also { reader.close() }
}