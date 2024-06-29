package kotlinx.document.database

import browser.indexeddb.IDBDatabase
import browser.indexeddb.IDBFactory
import browser.indexeddb.IDBObjectStore
import browser.indexeddb.IDBObjectStoreParameters
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.flow.Flow

suspend fun IDBFactory.openSuspend(name: String) = suspendCoroutine {
    val idbOpenDBRequest = open(name)
    idbOpenDBRequest.onsuccess = { event ->
        it.resume(event.target.result)
    }
    idbOpenDBRequest.onerror = { event ->
        it.resumeWithException(Exception(event.target.error.toString()))
    }
}

private data class IndexedDBStoreParams(
    override val keyPath: dynamic,
    override val autoIncrement: Boolean?
) : IDBObjectStoreParameters

fun IDBObjectStore.asIndexedDBMap() = IndexedDBMap(this)

class IndexedDBStore(val delegate: IDBDatabase) : DataStore {

    override suspend fun getMap(name: String): PersistentMap<String, String> =
        delegate.createObjectStore(name, IndexedDBStoreParams(null, false))
            .asIndexedDBMap()

    override suspend fun deleteMap(name: String) {
        TODO("Not yet implemented")
    }
}

class IndexedDBMap(private val delegate: IDBObjectStore) : PersistentMap<String, String> {
    override suspend fun clear() = suspendCoroutine {
        delegate.clear()
    }

    override suspend fun size(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override suspend fun get(key: String): String? {
        TODO("Not yet implemented")
    }

    override suspend fun put(key: String, value: String): String? {
        TODO("Not yet implemented")
    }

    override suspend fun remove(key: String): String? {
        TODO("Not yet implemented")
    }

    override suspend fun containsKey(key: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun update(key: String, value: String, updater: (String) -> String): UpdateResult<String> {
        TODO("Not yet implemented")
    }

    override suspend fun getOrPut(key: String, defaultValue: () -> String): String {
        TODO("Not yet implemented")
    }

    override fun entries(): Flow<Map.Entry<String, String>> {
        TODO("Not yet implemented")
    }
}