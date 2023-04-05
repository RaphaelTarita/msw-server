package msw.server.core.model.world

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.div
import kotlin.io.path.inputStream
import kotlin.streams.asSequence
import msw.server.core.common.JSONFile
import msw.server.core.common.directory
import msw.server.core.model.PlayerStats
import net.benwoodworth.knbt.Nbt
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtVariant
import net.benwoodworth.knbt.decodeFromStream

// https://minecraft-de.gamepedia.com/Spielstand-Speicherung#Weltordner
class World(val root: Path) {
    companion object {
        private val NBT = Nbt {
            variant = NbtVariant.Java
            compression = NbtCompression.Gzip
            ignoreUnknownKeys = true
        }

        private fun collectStats(dir: Path): List<JSONFile<PlayerStats>> {
            val children = Files.list(dir)
                .asSequence()
                .filter { !Files.isDirectory(it) }
                .toList()
            val res = mutableListOf<JSONFile<PlayerStats>>()
            for (p in children) {
                res.add(JSONFile(p, PlayerStats.serializer()))
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
    val advancements = directory(root, "advancements", create = true)
    val data = directory(root, "data")
    val datapacks = directory(root, "datapacks", require = false)
    val dimMinus1 = directory(root, "DIM-1")
    val dimPlus1 = directory(root, "DIM1")
    val generated = directory(root, "generated", require = false)
    val playerdata = directory(root, "playerdata")
    val players = directory(root, "players", require = false)
    val poi = directory(root, "poi", require = false)
    val region = directory(root, "region")
    val stats = collectStats(directory(root, "stats", create = true))
    val levelData = root / "level.dat"
    val levelDataOld = root / "level.dat_old"
    val ressources = root / "resources.zip"
    val sessionLock = root / "session.lock"

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