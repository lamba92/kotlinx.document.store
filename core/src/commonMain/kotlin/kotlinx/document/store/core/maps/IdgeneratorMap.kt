package kotlinx.document.store.core.maps

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.document.store.core.PersistentMap
import kotlinx.document.store.core.SerializableEntry
import kotlinx.document.store.core.UpdateResult

/**
 * Converts the current `PersistentMap` instance to an instance of `IdGenerator`.
 *
 * This transformation allows the key-value pairs in the `PersistentMap<String, String>`
 * to be used as a persistent key-value store with key type `String` and value type `Long`,
 * providing seamless serialization and deserialization of the values.
 *
 * @return An `IdGenerator` instance backed by the current `PersistentMap`.
 */
public fun PersistentMap<String, String>.asIdGenerator(): IdGenerator = IdGenerator(this)

/**
 * An implementation of the `PersistentMap` interface where the values are
 * automatically converted between `String` and `Long` for storage and retrieval.
 *
 * This class delegates the actual storage and persistence operations to another
 * `PersistentMap` instance, while providing a type-safe interface with `Long`
 * values for the users. String-to-Long and Long-to-String conversions are handled
 * implicitly during operations.
 *
 * @constructor Creates an `IdGenerator` with the specified delegate map.
 * @param delegate The underlying `PersistentMap` that performs the actual
 * storage operations. This map stores values as `String`.
 */
public class IdGenerator(private val delegate: PersistentMap<String, String>) : PersistentMap<String, Long> {
    override suspend fun clear(): Unit = delegate.clear()

    override suspend fun size(): Long = delegate.size()

    override suspend fun isEmpty(): Boolean = delegate.isEmpty()

    override fun close() {
        delegate.close()
    }

    override suspend fun get(key: String): Long? = delegate.get(key)?.toLong()

    override suspend fun put(
        key: String,
        value: Long,
    ): Long? = delegate.put(key, value.toString())?.toLong()

    override suspend fun remove(key: String): Long? = delegate.remove(key)?.toLong()

    override suspend fun containsKey(key: String): Boolean = delegate.containsKey(key)

    override suspend fun update(
        key: String,
        value: Long,
        updater: (Long) -> Long,
    ): UpdateResult<Long> =
        delegate.update(
            key = key,
            value = value.toString(),
            updater = { updater(it.toLong()).toString() },
        ).let { UpdateResult(it.oldValue?.toLong(), it.newValue.toLong()) }

    override suspend fun getOrPut(
        key: String,
        defaultValue: () -> Long,
    ): Long =
        delegate.getOrPut(
            key = key,
            defaultValue = { defaultValue().toString() },
        ).toLong()

    override fun entries(): Flow<Map.Entry<String, Long>> =
        delegate.entries()
            .map { SerializableEntry(it.key, it.value.toLong()) }
}
