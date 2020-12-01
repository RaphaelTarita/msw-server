package msw.server.core.common

const val exists = "./src/test/resources/DirectoryTest"
const val existsNot = "./src/test/resources/NonExistent"
const val create = "./src/test/resources/Create"
const val alphanum = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
val escapeThese = charArrayOf('~', '/', '+', '-')
val map1 = mapOf(
    "Hello" to "World",
    "Raphael" to "Tarita",
    "MSW" to "Server",
    "Server" to "Directory",
    "KXS" to "JSON"
)
val map2 = mapOf(
    "Hello" to "Kotlin",
    "MSW" to "Client",
    "Minecraft" to "Server Watcher",
    "Test Framework" to "Kotest",
    "KXS" to "Properties"
)