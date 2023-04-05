package msw.server.core.common

import kotlin.random.Random

val fixedSeedRandom = Random(1234)

const val parentDir = "./src/test/resources"
const val existsName = "DirectoryTest"
const val exists = "$parentDir/$existsName"
const val existsNotName = "NonExistent"
const val existsNot = "$parentDir/$existsNotName"
const val createName = "Create"
const val create = "$parentDir/$createName"
const val alphanum = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
val escapeThese = charArrayOf('~', '/', '+', '-')