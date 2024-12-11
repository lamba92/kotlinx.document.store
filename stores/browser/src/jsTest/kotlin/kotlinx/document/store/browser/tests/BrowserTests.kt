@file:Suppress("unused")

package kotlinx.document.store.browser.tests

import kotlinx.coroutines.await
import kotlinx.document.store.DataStore
import kotlinx.document.store.browser.BrowserStore
import kotlinx.document.store.tests.AbstractDeleteTests
import kotlinx.document.store.tests.AbstractDocumentDatabaseTests
import kotlinx.document.store.tests.AbstractFindTests
import kotlinx.document.store.tests.AbstractIndexTests
import kotlinx.document.store.tests.AbstractInsertTests
import kotlinx.document.store.tests.AbstractObjectCollectionTests
import kotlinx.document.store.tests.AbstractUpdateTests
import kotlinx.document.store.tests.DataStoreProvider
import kotlinx.document.store.tests.DatabaseDeleter

class BrowserDeleteTests :
    AbstractDeleteTests(BrowserStoreProvider),
    DatabaseDeleter by BrowserStoreProvider

class BrowserDocumentDatabaseTests :
    AbstractDocumentDatabaseTests(BrowserStoreProvider),
    DatabaseDeleter by BrowserStoreProvider

class BrowserIndexTests :
    AbstractIndexTests(BrowserStoreProvider),
    DatabaseDeleter by BrowserStoreProvider

class BrowserInsertTests :
    AbstractInsertTests(BrowserStoreProvider),
    DatabaseDeleter by BrowserStoreProvider

class BrowserUpdateTests :
    AbstractUpdateTests(BrowserStoreProvider),
    DatabaseDeleter by BrowserStoreProvider

class BrowserFindTests :
    AbstractFindTests(BrowserStoreProvider),
    DatabaseDeleter by BrowserStoreProvider

class BrowserObjectCollectionTests :
    AbstractObjectCollectionTests(BrowserStoreProvider),
    DatabaseDeleter by BrowserStoreProvider

object BrowserStoreProvider : DataStoreProvider, DatabaseDeleter {
    override suspend fun deleteDatabase(testName: String) {
        keyval.clear().await()
    }

    override fun provide(testName: String): DataStore = BrowserStore
}
