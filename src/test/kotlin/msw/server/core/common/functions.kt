package msw.server.core.common

fun randomString(length: Int = 10, vararg canContainAdditional: Char): String {
    val charset: List<Char> = alphanum.toCharArray()
        .toMutableList()
        .apply { addAll(canContainAdditional.toTypedArray()) }

    val builder = StringBuilder()
    for (i in 0 until length) {
        builder.append(charset[fixedSeedRandom.nextInt(charset.size)])
    }
    return builder.toString()
}