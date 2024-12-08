package kotlinx.document.store

import kotlinx.coroutines.flow.Flow

public interface PersistentMap<K, V> : AutoCloseable {
    public suspend fun get(key: K): V?

    public suspend fun put(
        key: K,
        value: V,
    ): V?

    public suspend fun remove(key: K): V?

    public suspend fun containsKey(key: K): Boolean

    public suspend fun clear()

    public suspend fun size(): Long

    public suspend fun isEmpty(): Boolean

    public suspend fun update(
        key: K,
        value: V,
        updater: (V) -> V,
    ): UpdateResult<V>

    public suspend fun getOrPut(
        key: K,
        defaultValue: () -> V,
    ): V

    public fun entries(): Flow<Map.Entry<K, V>>

    override fun close() {
    }
}

public data class UpdateResult<V>(
    val oldValue: V?,
    val newValue: V,
)
