package msw.server.core

import kotlinx.coroutines.delay
import msw.server.core.common.Directory
import msw.server.core.model.ServerDirectory
import msw.server.core.watcher.ServerWatcher

suspend fun main() {
    val directory = ServerDirectory(Directory("C:\\Users\\rapha\\Desktop\\minecraft_server"))

    val watcher = ServerWatcher(directory)
    watcher.launchInstance(
        25565,
        "uummannaq",
        "1.16.3",
        "default",
        guiEnabled = true
    )

    watcher.launchInstance(
        25564,
        "ultrasurvival",
        "1.16.3",
        "restricted",
        guiEnabled = true
    )

    delay(20_000)
    watcher.sendCommand(25565, "say I am the Instance that runs on port 25565, with world 'uummannaq'")
    watcher.sendCommand(25564, "say I am the instance that runs on port 25564, with world 'ultrasurvival'")
}