package msw.server.core.common

import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.CoroutineScope

interface GlobalInjections {
    val terminal: Terminal
    val toplevelScope: CoroutineScope
    val netScope: CoroutineScope
}