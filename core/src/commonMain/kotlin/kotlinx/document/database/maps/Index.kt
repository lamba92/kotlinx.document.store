package kotlinx.document.database.maps

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.document.database.PersistentMap
import kotlinx.document.database.SimpleEntry
import kotlinx.document.database.UpdateResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private fun String.split() = Json.decodeFromString<Set<Long>>(this)
private fun Set<Long>.join() = Json.encodeToString(this)

fun PersistentMap<String, String>.asIndex() = Index(this)

class Index(private val delegate: PersistentMap<String, String>) : PersistentMap<String?, Set<Long>> {

    companion object {
        const val NULL_MARKER = "%%%null%%%"
    }

    override suspend fun get(key: String?): Set<Long>? =
        delegate.get(key ?: NULL_MARKER)?.split()

    override suspend fun put(key: String?, value: Set<Long>): Set<Long>? =
        delegate.put(key ?: NULL_MARKER, value.join())
            ?.split()

    override suspend fun remove(key: String?): Set<Long>? =
        delegate.remove(key ?: NULL_MARKER)?.split()

    override suspend fun containsKey(key: String?): Boolean =
        delegate.containsKey(key ?: NULL_MARKER)

    override suspend fun clear() =
        delegate.clear()

    override suspend fun size(): Long =
        delegate.size()

    override suspend fun isEmpty(): Boolean =
        delegate.isEmpty()

    override fun entries(): Flow<Map.Entry<String?, Set<Long>>> =
        delegate.entries().map { SimpleEntry(it.key, it.value.split()) }

    override fun close() {
        delegate.close()
    }

    override suspend fun getOrPut(key: String?, defaultValue: () -> Set<Long>): Set<Long> =
        delegate.getOrPut(
            key = key ?: NULL_MARKER,
            defaultValue = { defaultValue().join() }
        ).split()

    override suspend fun update(
        key: String?,
        value: Set<Long>,
        updater: (Set<Long>) -> Set<Long>
    ): UpdateResult<Set<Long>> = delegate.update(
        key = key ?: NULL_MARKER,
        value = value.join(),
        updater = { updater(it.split()).join() }
    ).let { UpdateResult(it.oldValue?.split(), it.newValue.split()) }

}