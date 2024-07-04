package kotlinx.document.database

import kotlinx.coroutines.flow.Flow

interface PersistentMap<K, V> : AutoCloseable {
    suspend fun get(key: K): V?

    suspend fun put(
        key: K,
        value: V,
    ): V?

    suspend fun remove(key: K): V?

    suspend fun containsKey(key: K): Boolean

    suspend fun clear()

    suspend fun size(): Long

    suspend fun isEmpty(): Boolean

    suspend fun update(
        key: K,
        value: V,
        updater: (V) -> V,
    ): UpdateResult<V>

    suspend fun getOrPut(
        key: K,
        defaultValue: () -> V,
    ): V

    fun entries(): Flow<Map.Entry<K, V>>

    override fun close() {
    }
}

data class UpdateResult<V>(
    val oldValue: V?,
    val newValue: V,
)
