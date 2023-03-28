package msw.server.core.common

import com.github.ajalt.mordant.terminal.Terminal
import java.util.concurrent.Executors
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher

object SingletonInjectionImpl : GlobalInjections, AutoCloseable {
    override val terminal: Terminal = Terminal()
    private val toplevelDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    override val toplevelScope = CoroutineScope(toplevelDispatcher)
    override val netScope = CoroutineScope(EmptyCoroutineContext)
    override fun close() {
        toplevelDispatcher.close()
    }
}