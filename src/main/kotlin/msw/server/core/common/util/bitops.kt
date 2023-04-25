package msw.server.core.common.util

val HEX_CHARS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

fun ByteArray.toHexString(): String {
    val hexChars = CharArray(size * 2)
    for (i in indices) {
        val v = this[i].toInt() and 0xFF
        hexChars[i * 2] = HEX_CHARS[v ushr 4]
        hexChars[i * 2 + 1] = HEX_CHARS[v and 0x0F]
    }
    return String(hexChars)
}