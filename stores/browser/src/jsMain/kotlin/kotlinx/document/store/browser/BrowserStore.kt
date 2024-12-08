package kotlinx.document.store.browser

import kotlinx.coroutines.await
import kotlinx.document.store.AbstractDataStore
import kotlinx.document.store.PersistentMap

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
