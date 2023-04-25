package msw.server.core.common

import io.kotest.core.TestConfiguration
import io.kotest.engine.spec.tempdir
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoField
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.random.nextInt
import msw.server.core.versions.DownloadManifest
import msw.server.core.versions.model.VersionType

fun randomString(length: Int = 10, vararg canContainAdditional: Char): String {
    val charset: Set<Char> = alphanum + canContainAdditional.toSet()

    return List(length) { charset.random(fixedSeedRandom) }.joinToString("")
}

private fun inRangeOf(field: ChronoField): IntRange {
    return field.range().largestMinimum.toInt()..field.range().smallestMaximum.toInt()
}

fun randomOffsetDateTime(): OffsetDateTime {
    return OffsetDateTime.of(
        fixedSeedRandom.nextInt(inRangeOf(ChronoField.YEAR)),
        fixedSeedRandom.nextInt(inRangeOf(ChronoField.MONTH_OF_YEAR)),
        fixedSeedRandom.nextInt(inRangeOf(ChronoField.DAY_OF_MONTH)),
        fixedSeedRandom.nextInt(inRangeOf(ChronoField.HOUR_OF_DAY)),
        fixedSeedRandom.nextInt(inRangeOf(ChronoField.MINUTE_OF_HOUR)),
        fixedSeedRandom.nextInt(inRangeOf(ChronoField.SECOND_OF_MINUTE)),
        fixedSeedRandom.nextInt(inRangeOf(ChronoField.NANO_OF_SECOND)),
        ZoneOffset.UTC
    )
}

fun randomDownloadManifest(): DownloadManifest {
    return DownloadManifest(
        randomString(),
        URL("https://www.example.com/test"),
        (VersionType.values().toSet() - VersionType.ALL).random(fixedSeedRandom),
        randomOffsetDateTime(),
        randomOffsetDateTime(),
        fixedSeedRandom.nextLong(0, Long.MAX_VALUE),
        randomString()
    )
}

@JvmInline
value class TempDirBuilderScope(val path: Path) {
    fun tempdir(prefix: String? = null): Path {
        return Files.createTempDirectory(path, prefix)
    }

    @OptIn(ExperimentalContracts::class)
    inline fun tempdir(prefix: String? = null, builder: TempDirBuilderScope.() -> Unit): Path {
        contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
        val parent = Files.createTempDirectory(path, prefix)
        TempDirBuilderScope(parent).builder()
        return parent
    }

    fun tempfile(prefix: String? = null, suffix: String? = null): Path {
        return Files.createTempFile(path, prefix, suffix)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun TestConfiguration.tempdir(
    prefix: String? = null,
    builder: TempDirBuilderScope.() -> Unit
): Path {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    val parent = tempdir(prefix).toPath()
    TempDirBuilderScope(parent).builder()
    return parent
}

fun String.countSubstrings(substring: String): Int {
    var current = indexOf(substring)
    val len = substring.length
    var count = 0
    while (current >= 0) {
        ++count
        current = indexOf(substring, current + len)
    }
    return count
}