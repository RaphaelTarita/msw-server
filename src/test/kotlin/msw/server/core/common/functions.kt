package msw.server.core.common

import java.util.Random

fun randomString(length: Int = 10, vararg canContainAdditional: Char): String {
    val charset: List<Char> = alphanum.toCharArray()
        .toMutableList()
        .apply { addAll(canContainAdditional.toTypedArray()) }

    val r = Random()
    val builder = StringBuilder()
    for (i in 0 until length) {
        builder.append(charset[r.nextInt(charset.size)])
    }
    return builder.toString()
}