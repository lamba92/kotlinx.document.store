package kotlinx.document.database.maps

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.document.database.PersistentMap
import kotlinx.document.database.SimpleEntry
import kotlinx.document.database.UpdateResult

fun PersistentMap<String, String>.asIdGenerator() = IdGenerator(this)

class IdGenerator(private val delegate: PersistentMap<String, String>) : PersistentMap<String, Long> {

    override suspend fun clear() =
        delegate.clear()

    override suspend fun size(): Long =
        delegate.size()

    override suspend fun isEmpty(): Boolean =
        delegate.isEmpty()

    override fun close() {
        delegate.close()
    }

    override suspend fun get(key: String): Long? =
        delegate.get(key)?.toLong()

    override suspend fun put(key: String, value: Long): Long? =
        delegate.put(key, value.toString())?.toLong()

    override suspend fun remove(key: String): Long? =
        delegate.remove(key)?.toLong()

    override suspend fun containsKey(key: String): Boolean =
        delegate.containsKey(key)

    override suspend fun update(key: String, value: Long, updater: (Long) -> Long): UpdateResult<Long> =
        delegate.update(
            key = key,
            value = value.toString(),
            updater = { updater(it.toLong()).toString() }
        ).let { UpdateResult(it.oldValue?.toLong(), it.newValue.toLong()) }

    override suspend fun getOrPut(key: String, defaultValue: () -> Long): Long =
        delegate.getOrPut(
            key = key,
            defaultValue = { defaultValue().toString() }
        ).toLong()

    override fun entries(): Flow<Map.Entry<String, Long>> =
        delegate.entries().map { SimpleEntry(it.key, it.value.toLong()) }

}