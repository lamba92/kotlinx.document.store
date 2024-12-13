package kotlinx.document.store.core

import kotlinx.coroutines.flow.Flow

/**
 * Represents a persistent key-value map with suspendable operations.
 *
 * This interface provides methods to perform basic CRUD operations, size evaluation,
 * and other utility operations on a persistent map, ensuring data durability and
 * consistency. The implementation is expected to support concurrent usage and persistence
 * across application restarts.
 *
 * @param K The type of keys maintained by the map.
 * @param V The type of values maintained by the map.
 */
public interface PersistentMap<K, V> : AutoCloseable {
    /**
     * Retrieves the value associated with the specified key from the persistent map.
     *
     * @param key The key whose associated value is to be retrieved.
     * @return The value associated with the specified key, or `null` if the key is not present in the map.
     */
    public suspend fun get(key: K): V?

    /**
     * Associates the specified value with the specified key in the persistent map.
     * If the map previously contained a mapping for the key, the old value is replaced.
     *
     * @param key The key with which the specified value is to be associated.
     * @param value The value to be associated with the specified key.
     * @return The previous value associated with the key, or `null` if there was no mapping for the key.
     */
    public suspend fun put(
        key: K,
        value: V,
    ): V?

    /**
     * Removes the entry for the specified key from the persistent map, if it exists.
     *
     * @param key The key whose associated entry is to be removed.
     * @return The value that was associated with the key before removal, or `null` if the key was not present in the map.
     */
    public suspend fun remove(key: K): V?

    /**
     * Checks whether the specified key exists in the persistent map.
     *
     * @param key The key to be checked for existence in the map.
     * @return `true` if the key exists in the map, `false` otherwise.
     */
    public suspend fun containsKey(key: K): Boolean

    /**
     * Clears all entries in the persistent map.
     * This operation removes all key-value pairs, resulting in an empty map.
     * It effectively resets the map to its initial state.
     */
    public suspend fun clear()

    /**
     * Retrieves the total number of key-value pairs currently stored in the persistent map.
     *
     * @return The count of entries in the persistent map as a [Long].
     */
    public suspend fun size(): Long

    /**
     * Checks if the persistent map is empty.
     *
     * @return `true` if the map contains no key-value pairs, `false` otherwise.
     */
    public suspend fun isEmpty(): Boolean

    /**
     * Updates the entry in the persistent map associated with the specified key.
     * If the key exists, the updater function is applied to the current value
     * to compute the new value, which is then stored in the map.
     * If the key does not exist, the provided value is stored in the map as a new entry.
     *
     * @param key The key of the entry to be updated.
     * @param value The value to be associated with the key if the key does not already exist.
     * @param updater A function that computes the new value based on the existing value.
     * @return An [UpdateResult] containing the old value (if any) and the new value.
     */
    public suspend fun update(
        key: K,
        value: V,
        updater: (V) -> V,
    ): UpdateResult<V>

    /**
     * Retrieves the value associated with the specified key from the persistent map.
     * If the key does not exist in the map, the `defaultValue` function is invoked to
     * compute a value, which is then stored in the map and returned.
     *
     * @param key The key whose associated value is to be retrieved or computed.
     * @param defaultValue A function that calculates the value to associate with the key if it is not present.
     * @return The value associated with the specified key, or the value computed and stored if the key did not exist.
     */
    public suspend fun getOrPut(
        key: K,
        defaultValue: () -> V,
    ): V

    /**
     * Returns a flow of all entries in the persistent map as [Map.Entry] objects.
     *
     * @return A [Flow] emitting each key-value pair in the map as a [Map.Entry].
     */
    public fun entries(): Flow<Map.Entry<K, V>>

    override fun close() {
    }
}

/**
 * Represents the result of an update operation, indicating the previous value and the updated value.
 *
 * This data class is typically used to provide information about a completed
 * update operation, where the old value is tracked alongside the new value.
 *
 * @param V The type of values being updated.
 * @property oldValue The previous value before the update, or null if no prior value existed.
 * @property newValue The new value after the update.
 */
public data class UpdateResult<V>(
    val oldValue: V?,
    val newValue: V,
)
