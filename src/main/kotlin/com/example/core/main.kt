package com.example.core

import com.example.core.common.StringProperties
import com.example.core.common.readFromPath
import com.example.core.model.props.ServerProperties
import kotlinx.serialization.ExperimentalSerializationApi

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