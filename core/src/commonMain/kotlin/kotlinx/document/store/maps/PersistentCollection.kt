package kotlinx.document.store.maps

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.document.store.PersistentMap
import kotlinx.document.store.SerializableEntry
import kotlinx.document.store.UpdateResult

/**
 * Converts a [PersistentMap] instance into a [PersistentCollection].
 *
 * @return A [PersistentCollection] that wraps the current [PersistentMap].
 */
public fun PersistentMap<String, String>.asCollectionMap(): PersistentCollection = PersistentCollection(this)

/**
 * PersistentCollection is an implementation of the PersistentMap interface, which adapts
 * a PersistentMap<String, String> as a PersistentMap<Long, String>. It provides support
 * for suspendable and persistent key-value operations, where the keys are of type Long.
 *
 * This class leverages an underlying PersistentMap<String, String> for its operations,
 * and ensures compatibility by converting Long keys to their String representation.
 *
 * @constructor Creates a PersistentCollection instance with a specified PersistentMap<String, String>.
 * @param delegate The underlying PersistentMap<String, String> used to perform operations.
 */
public class PersistentCollection(private val delegate: PersistentMap<String, String>) : PersistentMap<Long, String> {
    override suspend fun clear(): Unit = delegate.clear()

    override suspend fun size(): Long = delegate.size()

    override suspend fun isEmpty(): Boolean = delegate.isEmpty()

    override fun close() {
        delegate.close()
    }

    override suspend fun get(key: Long): String? = delegate.get(key.toString())

    override suspend fun put(
        key: Long,
        value: String,
    ): String? = delegate.put(key.toString(), value)

    override suspend fun remove(key: Long): String? = delegate.remove(key.toString())

    override suspend fun containsKey(key: Long): Boolean = delegate.containsKey(key.toString())

    override suspend fun update(
        key: Long,
        value: String,
        updater: (String) -> String,
    ): UpdateResult<String> =
        delegate.update(
            key = key.toString(),
            value = value,
            updater = { updater(it) },
        )

    override suspend fun getOrPut(
        key: Long,
        defaultValue: () -> String,
    ): String =
        delegate.getOrPut(
            key = key.toString(),
            defaultValue = { defaultValue() },
        )

    override fun entries(): Flow<Map.Entry<Long, String>> =
        delegate.entries()
            .map { SerializableEntry(it.key.toLong(), it.value) }
}
