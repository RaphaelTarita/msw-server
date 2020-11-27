package com.example.core

import com.example.core.common.Directory
import com.example.core.versions.DownloadManager
import com.example.core.versions.ManifestCreator
import kotlinx.serialization.ExperimentalSerializationApi
import java.nio.file.Path
import kotlin.system.measureTimeMillis

@ExperimentalSerializationApi
fun main() {
    val directory = ServerDirectory(Directory("C:\\Users\\rapha\\Desktop\\minecraft_server"))
    with(directory) {
        println(bannedIPs.get())
        println(bannedPlayers.get())
        println(ops.get())
        println(usercache.get())
        println(whitelist.get())
    }

    val creator = ManifestCreator()
    val manifest = creator.latestRelease()
    println(manifest)
    val manager = DownloadManager(manifest, Path.of("C:\\Users\\rapha\\Desktop\\download.jar"))
    val millis = measureTimeMillis {
        manager.download()
    }
    println("completed download in $millis milliseconds")
}