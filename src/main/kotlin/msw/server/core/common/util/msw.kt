package msw.server.core.common.util

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import com.toasttab.protokt.Timestamp
import java.time.OffsetDateTime
import msw.server.core.common.MemoryAmount
import msw.server.core.common.MemoryUnit
import msw.server.core.versions.DownloadManifest
import msw.server.core.versions.model.VersionType
import msw.server.rpc.versions.VersionDetails
import msw.server.rpc.versions.VersionLabel

fun VersionType.toVersionLabel(): VersionLabel {
    return when (this) {
        VersionType.OLD_ALPHA -> VersionLabel.ALPHA
        VersionType.OLD_BETA -> VersionLabel.BETA
        VersionType.RELEASE -> VersionLabel.RELEASE
        VersionType.SNAPSHOT -> VersionLabel.SNAPSHOT
        VersionType.ALL -> throw IllegalArgumentException("VersionType was '$this', which is an illegal value (only used for filtering)")
    }
}

fun OffsetDateTime.toTimestamp() = Timestamp {
    seconds = this@toTimestamp.toEpochSecond()
    nanos = this@toTimestamp.nano
}

fun DownloadManifest.toVersionDetails() = VersionDetails {
    versionID = this@toVersionDetails.versionID
    label = this@toVersionDetails.type.toVersionLabel()
    time = this@toVersionDetails.time.toTimestamp()
    releaseTime = this@toVersionDetails.releaseTime.toTimestamp()
    size = this@toVersionDetails.size
    sha1 = this@toVersionDetails.sha1
}

val Long.bytes: MemoryAmount
    get() = MemoryAmount {
        amount = this@bytes
        unit = MemoryUnit.BYTES
    }

val Long.kibibytes: MemoryAmount
    get() = MemoryAmount {
        amount = this@kibibytes
        unit = MemoryUnit.KIBIBYTES
    }

val Long.mebibytes: MemoryAmount
    get() = MemoryAmount {
        amount = this@mebibytes
        unit = MemoryUnit.MEBIBYTES
    }

val Long.gibibytes: MemoryAmount
    get() = MemoryAmount {
        amount = this@gibibytes
        unit = MemoryUnit.GIBIBYTES
    }

fun MemoryAmount.toCommandString(): String {
    return "$amount${
        when (unit) {
            MemoryUnit.BYTES -> ""
            MemoryUnit.KIBIBYTES -> "k"
            MemoryUnit.MEBIBYTES -> "m"
            MemoryUnit.GIBIBYTES -> "g"
            else -> throw IllegalArgumentException("Unrecognized memory unit: '$unit'")
        }
    }"
}


fun Terminal.readyMsg(component: String) {
    println(TextColors.green("✓ $component initialized and ready."))
}