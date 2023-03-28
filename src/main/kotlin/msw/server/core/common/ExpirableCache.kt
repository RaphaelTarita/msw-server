package msw.server.core.common

class ExpirableCache<K, V>(private val expiresAfterMillis: Long) {
    @PublishedApi
    internal sealed interface EntryToken<out T> {
        @JvmInline
        value class HIT<T>(val value: T) : EntryToken<T>
        object MISS : EntryToken<Nothing>
    }

    companion object {
        private fun millis() = System.currentTimeMillis()
    }

    private var cache = mutableMapOf<K, Pair<Long, V>>()

    @PublishedApi
    internal fun getInternal(key: K): EntryToken<V> {
        val entry = cache[key] ?: return EntryToken.MISS
        if (millis() - entry.first > expiresAfterMillis) {
            cache.remove(key)
            return EntryToken.MISS
        }
        return EntryToken.HIT(entry.second)
    }

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
        return when (val token = getInternal(key)) {
            is EntryToken.HIT -> token.value
            is EntryToken.MISS -> return null
        }
    }

    inline fun runOrGet(key: K, function: (K) -> V): V {
        return when (val token = getInternal(key)) {
            is EntryToken.HIT -> token.value
            is EntryToken.MISS -> {
                val value = function(key)
                cache(key, value)
                value
            }
        }
    }

    fun gc() {
        cache = cache.filterValues { (timestamp, _) ->
            millis() - timestamp <= expiresAfterMillis
        }.toMutableMap()
    }

    fun validCount(gc: Boolean = false): Int {
        return if (gc) {
            gc()
            cache.size
        } else {
            cache.values.count { (timestamp, _) ->
                millis() - timestamp <= expiresAfterMillis
            }
        }
    }
}