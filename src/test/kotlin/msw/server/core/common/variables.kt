package msw.server.core.common

import kotlin.random.Random

val fixedSeedRandom = Random(1234)

const val exists = "./src/test/resources/DirectoryTest"
const val existsNot = "./src/test/resources/NonExistent"
const val create = "./src/test/resources/Create"
const val alphanum = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
val escapeThese = charArrayOf('~', '/', '+', '-')