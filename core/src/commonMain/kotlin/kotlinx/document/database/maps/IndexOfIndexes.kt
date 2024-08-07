package kotlinx.document.database.maps

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.document.database.PersistentMap
import kotlinx.document.database.SerializableEntry
import kotlinx.document.database.UpdateResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

public fun PersistentMap<String, String>.asIndexOfIndexes(): IndexOfIndexes = IndexOfIndexes(this)

private fun String.split() = Json.decodeFromString<List<String>>(this)

private fun List<String>.join() = Json.encodeToString(this)

public class IndexOfIndexes(private val delegate: PersistentMap<String, String>) : PersistentMap<String, List<String>> {
    override suspend fun clear(): Unit = delegate.clear()

    override suspend fun size(): Long = delegate.size()

    override suspend fun isEmpty(): Boolean = delegate.isEmpty()

    override fun close() {
        delegate.close()
    }

    override suspend fun remove(key: String): List<String>? = delegate.remove(key)?.split()

    override suspend fun containsKey(key: String): Boolean = delegate.containsKey(key)

    override suspend fun get(key: String): List<String>? = delegate.get(key)?.split()

    override suspend fun put(
        key: String,
        value: List<String>,
    ): List<String>? = delegate.put(key, value.join())?.split()

    override suspend fun update(
        key: String,
        value: List<String>,
        updater: (List<String>) -> List<String>,
    ): UpdateResult<List<String>> =
        delegate.update(
            key = key,
            value = value.join(),
            updater = { updater(it.split()).join() },
        ).let { UpdateResult(it.oldValue?.split(), it.newValue.split()) }

    override suspend fun getOrPut(
        key: String,
        defaultValue: () -> List<String>,
    ): List<String> =
        delegate.getOrPut(
            key = key,
            defaultValue = { defaultValue().join() },
        ).split()

    override fun entries(fromIndex: Long): Flow<Map.Entry<String, List<String>>> =
        delegate.entries(fromIndex)
            .map { SerializableEntry(it.key, it.value.split()) }
}
