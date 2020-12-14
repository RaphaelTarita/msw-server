package msw.server.core.common

class ExpirableCache<K, V>(private val expiresAfterMillis: Long) {
    companion object {
        private fun millis() = System.currentTimeMillis()
    }

    private val cache = mutableMapOf<K, Pair<Long, V>>()

    fun cache(key: K, value: V, timestamp: Long) {
        cache[key] = timestamp to value
    }

    fun cache(key: K, value: V) = cache(key, value, millis())

    fun status(key: K): Long {
        return cache[key]?.first ?: -1
    }

    fun containsKey(key: K) = cache.containsKey(key)

    fun containsValidEntry(key: K): Boolean {
        val status = status(key)
        return status >= 0 && (millis() - status) <= expiresAfterMillis
    }

    operator fun get(key: K): V? {
        val entry = cache[key] ?: return null
        if (millis() - entry.first > expiresAfterMillis) {
            cache.remove(key)
            return null
        }
        return entry.second
    }

    fun runOrGet(key: K, function: (K) -> V): V {
        val entry = get(key)
        return if (entry == null) {
            val value = function(key)
            cache(key, value)
            value
        } else {
            entry
        }
    }

    suspend fun suspendingRunOrGet(key: K, function: suspend (K) -> V): V {
        val entry = get(key)
        return if (entry == null) {
            val value = function(key)
            cache(key, value)
            value
        } else {
            entry
        }
    }
}