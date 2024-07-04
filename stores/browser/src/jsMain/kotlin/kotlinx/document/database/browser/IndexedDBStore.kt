package kotlinx.document.database.browser

import keyval.Store
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.document.database.DataStore
import kotlinx.document.database.PersistentMap
import kotlinx.document.database.SimpleEntry
import kotlinx.document.database.UpdateResult

class IndexedDBStore(val databaseName: String) : DataStore {

    override suspend fun getMap(name: String): PersistentMap<String, String> =
        IndexedDBMap(Store(databaseName, name))

    override suspend fun deleteMap(name: String) {
//        js("window").indexedDb
    }
}

class IndexedDBMap(val delegate: Store) : PersistentMap<String, String> {

    private val mutex = Mutex()

    override suspend fun clear() = keyval.clear(delegate).await()

    override suspend fun size() = keyval.keys(delegate).await().size.toLong()

    override suspend fun isEmpty(): Boolean = size() == 0L

    override suspend fun get(key: String) = keyval.get(key, delegate).await()

    override suspend fun put(key: String, value: String) = mutex.withLock {
        val previous = get(key)
        keyval.set(key, value, delegate).await()
        previous
    }

    override suspend fun remove(key: String): String? = mutex.withLock {
        val previous = get(key)
        keyval.del(key, delegate).await()
        previous
    }

    override suspend fun containsKey(key: String) = key in keyval.keys(delegate).await()

    override suspend fun update(key: String, value: String, updater: (String) -> String): UpdateResult<String> =
        mutex.withLock {
            val oldValue = get(key)
            val newValue = oldValue?.let(updater) ?: value
            keyval.set(key, newValue, delegate).await()
            UpdateResult(oldValue, newValue)
        }

    override suspend fun getOrPut(key: String, defaultValue: () -> String): String = mutex.withLock {
        get(key) ?: defaultValue().also { put(key, it) }
    }

    override fun entries(): Flow<Map.Entry<String, String>> = flow {
        keyval.keys(delegate).await()
            .asFlow()
            .mapNotNull { key ->
                get(key)?.let { value -> SimpleEntry(key, value) }
            }
            .collect { emit(it) }
    }
}