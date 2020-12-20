package msw.server.core.common

import kotlin.reflect.KClass

class ErrorTransformer<out S : Throwable>(
    private val handlerLookup: List<Pair<(Exception) -> Boolean, (Exception) -> S>>,
    private val fallback: (Exception) -> S
) {
    companion object {
        fun <S : Throwable> regularRulesetWithLambdaFallback(
            fallback: (Exception) -> S,
            vararg rules: Pair<(Exception) -> Boolean, (Exception) -> S>
        ): ErrorTransformer<S> {
            return ErrorTransformer(rules.toList(), fallback)
        }

        fun <S : Throwable> regularRuleset(
            fallback: S,
            vararg rules: Pair<(Exception) -> Boolean, (Exception) -> S>
        ): ErrorTransformer<S> {
            return ErrorTransformer(rules.toList()) { fallback }
        }

        fun <S : Throwable> typedRulesetWithLambdaFallback(
            fallback: (Exception) -> S,
            vararg typeRules: Pair<KClass<out Exception>, (Exception) -> S>
        ): ErrorTransformer<S> {
            return ErrorTransformer(
                typeRules.map { outer ->
                    { inner: Exception -> outer.first.isInstance(inner) } to outer.second
                }.toList(),
                fallback
            )
        }

        fun <S : Throwable> typedRuleset(
            fallback: S,
            vararg typeRules: Pair<KClass<out Exception>, (Exception) -> S>,
        ): ErrorTransformer<S> {
            return ErrorTransformer(
                typeRules.map { outer ->
                    { inner: Exception -> outer.first.isInstance(inner) } to outer.second
                }.toList(),
            ) { fallback }
        }
    }

    fun <R> pack0(throwingOperation: () -> R): () -> R {
        return {
            try {
                throwingOperation()
            } catch (exc: Exception) {
                throw handlerLookup.find { it.first(exc) }
                    ?.second?.invoke(exc)
                    ?: fallback(exc)
            }
        }
    }

    fun <R> pack0suspend(throwingOperation: suspend () -> R): suspend () -> R {
        return {
            try {
                throwingOperation()
            } catch (exc: Exception) {
                throw handlerLookup.find { it.first(exc) }
                    ?.second?.invoke(exc)
                    ?: fallback(exc)
            }
        }
    }

    fun <P0, R> pack1(throwingOperation: (P0) -> R): (P0) -> R {
        return {
            pack0 { throwingOperation(it) }()
        }
    }

    fun <P0, R> pack1suspend(throwingOperation: suspend (P0) -> R): suspend (P0) -> R {
        return {
            pack0suspend { throwingOperation(it) }()
        }
    }

    fun <P0, P1, R> pack2(throwingOperation: (P0, P1) -> R): (P0, P1) -> R {
        return { p0, p1 ->
            pack0 { throwingOperation(p0, p1) }()
        }
    }

    fun <P0, P1, R> pack2suspend(throwingOperation: suspend (P0, P1) -> R): suspend (P0, P1) -> R {
        return { p0, p1 ->
            pack0suspend { throwingOperation(p0, p1) }()
        }
    }

    fun <P0, P1, P2, R> pack3(throwingOperation: (P0, P1, P2) -> R): (P0, P1, P2) -> R {
        return { p0, p1, p2 ->
            pack0 { throwingOperation(p0, p1, p2) }()
        }
    }

    fun <P0, P1, P2, R> pack3suspend(throwingOperation: suspend (P0, P1, P2) -> R): suspend (P0, P1, P2) -> R {
        return { p0, p1, p2 ->
            pack0suspend { throwingOperation(p0, p1, p2) }()
        }
    }
}