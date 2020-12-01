package msw.server.core

import kotlinx.serialization.ExperimentalSerializationApi
import msw.server.core.common.StringProperties
import msw.server.core.common.readFromPath
import msw.server.core.model.props.ServerProperties

@ExperimentalSerializationApi
fun main() {
    /*val directory = ServerDirectory(Directory("C:\\Users\\rapha\\Desktop\\minecraft_server"))
    with(directory) {
        println(bannedIPs.get())
        println(bannedPlayers.get())
        println(ops.get())
        println(usercache.get())
        println(whitelist.get())
    }*/

    val str = readFromPath("C:\\Users\\rapha\\Desktop\\minecraft_server\\server.properties")
    val parsed = StringProperties.decodeWithComments(ServerProperties.serializer(), str)
    println("\n\nparsed data:\n${parsed.first}")
    println("\n\nparsed comments:\n${parsed.second}")
}