package msw.server.core.model

import msw.server.core.common.Directory
import msw.server.core.common.JSONFile
import msw.server.core.common.composePath
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

    val name: String = root.name // TODO: Acquire from levelData, using lazy delegation
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
    val levelData = composePath(root, "level.dat")
    val levelDataOld = composePath(root, "level.dat")
    val ressources = composePath(root, "resources.zip")
    val sessionLock = composePath(root, "session.lock")

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return if (other !is World) false else other.name == this.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}