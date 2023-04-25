package msw.server.core.common.util

fun String.truncate(maxlen: Int, suffix: String = "..."): String {
    val internalMaxlen = maxlen - suffix.length
    return if (length <= maxlen) this else substring(0, internalMaxlen) + suffix
}

fun String.replaceMultiple(map: Map<String, String>): String {
    var ret = this
    for ((old, new) in map) {
        ret = ret.replace(old, new)
    }
    return ret
}

fun String.escape(charsToEscape: CharArray): String {
    return replaceMultiple(charsToEscape.associate { it.toString() to "\\" + it })
}