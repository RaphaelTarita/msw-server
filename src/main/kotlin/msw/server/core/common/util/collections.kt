package msw.server.core.common.util

fun <K, V, R> Map<K, V>.ifContainsKey(key: K, action: (Pair<K, V>) -> R): R? {
    return if (containsKey(key)) {
        action(key to this.getValue(key))
    } else {
        null
    }
}

fun <T, K> Sequence<T>.distinctBy(selector: (T) -> K, onDuplicates: (T) -> Unit): Sequence<T> {
    val observed = mutableSetOf<K>()
    return sequence {
        forEach {
            val select = selector(it)
            if (select in observed) {
                onDuplicates(it)
            } else {
                observed += select
                yield(it)
            }
        }
    }
}