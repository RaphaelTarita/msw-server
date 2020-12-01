package msw.server.core.model

import msw.server.core.common.Directory
import msw.server.core.common.JSONFile
import java.io.File

// https://minecraft-de.gamepedia.com/Spielstand-Speicherung#Weltordner
class World(val root: Directory) {
    companion object {
        fun collectStats(dir: Directory): List<JSONFile<PlayerStats>> {
            val children = dir.listFiles { file: File -> !file.isDirectory } ?: emptyArray()
            val res = mutableListOf<JSONFile<PlayerStats>>()
            for (f in children) {
                res.add(JSONFile(f.toPath(), PlayerStats.serializer()))
            }
            return res
        }
    }

    val advancements = Directory(root, "advancements")
    val data = Directory(root, "data")
    val datapacks = Directory(root, "datapacks", require = false)
    val dimMinus1 = Directory(root, "DIM-1")
    val dimPlus1 = Directory(root, "DIM1")
    val generated = Directory(root, "generated", require = false)
    val playerdata = Directory(root, "playerdata")
    val players = Directory(root, "players", require = false)
    val poi = Directory(root, "poi", require = false)
    val region = Directory(root, "region")
    val stats = collectStats(Directory(root, "stats"))
    val levelData = File(root, "level.dat")
    val levelDataOld = File(root, "level.dat")
    val ressources = File(root, "resources.zip")
    val sessionLock = File(root, "session.lock")
}