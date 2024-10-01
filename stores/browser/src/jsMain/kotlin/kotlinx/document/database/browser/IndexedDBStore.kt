package kotlinx.document.database.browser

import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.document.database.DataStore
import kotlinx.document.database.PersistentMap
import kotlinx.document.database.SerializableEntry
import kotlinx.document.database.UpdateResult
import kotlinx.document.database.drop

object IndexedDBStore : DataStore {
    override val commitStrategy: DataStore.CommitStrategy = DataStore.CommitStrategy.OnChange

    override suspend fun getMap(name: String): PersistentMap<String, String> = IndexedDBMap(name)

    override suspend fun deleteMap(name: String) {
        keyval.keys().await()
            .filter { it.startsWith(name) }
            .let { keyval.delMany(it.toTypedArray()).await() }
    }

    override suspend fun close() {}

    override suspend fun commit() {}
}

class IndexedDBMap(private val prefix: String) : PersistentMap<String, String> {
    private val mutex = Mutex()

    override suspend fun clear() =
        keyval.keys()
            .await()
            .filter { it.startsWith("${prefix}_") }
            .let { keyval.delMany(it.toTypedArray()).await() }

    override suspend fun size() =
        keyval.keys()
            .await()
            .filter { it.startsWith("${prefix}_") }
            .size
            .toLong()

    override suspend fun isEmpty() = size() == 0L

    override suspend fun get(key: String) = keyval.get("${prefix}_$key").await()

    override suspend fun put(
        key: String,
        value: String,
    ) = mutex.withLock { unsafePut(key, value) }

    private suspend fun IndexedDBMap.unsafePut(
        key: String,
        value: String,
    ): String? {
        val previous = get(key)
        keyval.set("${prefix}_$key", value).await()
        return previous
    }

    override suspend fun remove(key: String): String? =
        mutex.withLock {
            val previous = get(key)
            keyval.del("${prefix}_$key").await()
            previous
        }

    override suspend fun containsKey(key: String) = get(key) != null

    override suspend fun update(
        key: String,
        value: String,
        updater: (String) -> String,
    ): UpdateResult<String> =
        mutex.withLock {
            val oldValue = get(key)
            val newValue = oldValue?.let(updater) ?: value
            keyval.set("${prefix}_$key", newValue).await()
            UpdateResult(oldValue, newValue)
        }

    override suspend fun getOrPut(
        key: String,
        defaultValue: () -> String,
    ): String =
        mutex.withLock {
            get(key) ?: defaultValue().also { unsafePut(key, it) }
        }

    override fun entries(fromIndex: Long): Flow<Map.Entry<String, String>> =
        flow {
            keyval.keys()
                .await()
                .asFlow()
                .filter { it.startsWith("${prefix}_") }
                .drop(fromIndex)
                .collect { key ->
                    keyval.get(key).await()?.let { value ->
                        emit(SerializableEntry(key.removePrefix("${prefix}_"), value))
                    }
                }
        }
}
