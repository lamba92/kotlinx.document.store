package kotlinx.document.store.browser

import kotlinx.coroutines.await
import kotlinx.document.store.AbstractDataStore
import kotlinx.document.store.DataStore
import kotlinx.document.store.PersistentMap

/**
 * Implementation of the [DataStore] for use in web browsers.
 *
 * `BrowserStore` uses `IndexedDB` as the underlying storage mechanism, providing
 * persistent key-value storage in the user's browser. It is designed for use in
 * web applications that require durable storage across browser sessions.
 *
 * This class supports the creation, retrieval, and deletion of named maps, where
 * each map is implemented as an [IndexedDBMap]. Concurrency and synchronization
 * are managed using locks to ensure thread safety during access to individual maps.
 *
 * This implementation extends [AbstractDataStore], inheriting utility methods for
 * managing locks and operations related to the data store.
 */
public object BrowserStore : AbstractDataStore() {
    override suspend fun getMap(name: String): PersistentMap<String, String> = withStoreLock { IndexedDBMap(name, getMutex(name)) }

    override suspend fun deleteMap(name: String): Unit =
        withStoreLock {
            lockAndRemoveMutex(name) {
                keyval.keys()
                    .await()
                    .filter { it.startsWith(IndexedDBMap.buildPrefix(name)) }
                    .let { keyval.delMany(it.toTypedArray()).await() }
            }
        }
}
