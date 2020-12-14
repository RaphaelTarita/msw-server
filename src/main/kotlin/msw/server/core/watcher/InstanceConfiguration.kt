package msw.server.core.watcher

import msw.server.core.common.MemoryAmount
import msw.server.core.common.m
import java.nio.file.Path

data class InstanceConfiguration(
    val version: Path,
    val presetID: String = "default",
    val heapInit: MemoryAmount = 1024L.m,
    val heapMax: MemoryAmount = 1024L.m,
    val guiEnabled: Boolean = false
)