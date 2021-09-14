package msw.server.core.model.world

import msw.server.core.common.Directory
import msw.server.core.common.JSONFile
import msw.server.core.common.composePath
import msw.server.core.model.PlayerStats
import net.benwoodworth.knbt.Nbt
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtVariant
import net.benwoodworth.knbt.decodeFromStream
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.inputStream

// https://minecraft-de.gamepedia.com/Spielstand-Speicherung#Weltordner
class World(val root: Directory) {
    companion object {
        private val NBT = Nbt {
            variant = NbtVariant.Java
            compression = NbtCompression.Gzip
            ignoreUnknownKeys = true
        }

        private fun collectStats(dir: Directory): List<JSONFile<PlayerStats>> {
            val children = dir.listFiles { file: File -> !file.isDirectory } ?: emptyArray()
            val res = mutableListOf<JSONFile<PlayerStats>>()
            for (f in children) {
                res.add(JSONFile(f.toPath(), PlayerStats.serializer()))
            }
            return res
        }

        private fun findName(levelData: Path): String {
            return levelData.inputStream(StandardOpenOption.READ).use {
                NBT.decodeFromStream<LevelRoot>(it).data.levelName
            }
        }
    }

    val name: String by lazy { findName(levelData) }
    val advancements = Directory(root, "advancements", create = true)
    val data = Directory(root, "data")
    val datapacks = Directory(root, "datapacks", require = false)
    val dimMinus1 = Directory(root, "DIM-1")
    val dimPlus1 = Directory(root, "DIM1")
    val generated = Directory(root, "generated", require = false)
    val playerdata = Directory(root, "playerdata")
    val players = Directory(root, "players", require = false)
    val poi = Directory(root, "poi", require = false)
    val region = Directory(root, "region")
    val stats = collectStats(Directory(root, "stats", create = true))
    val levelData = composePath(root, "level.dat")
    val levelDataOld = composePath(root, "level.dat_old")
    val ressources = composePath(root, "resources.zip")
    val sessionLock = composePath(root, "session.lock")

    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        Array(5) { it }
        if (other === this) return true
        return if (other !is World) false else other.name == this.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}