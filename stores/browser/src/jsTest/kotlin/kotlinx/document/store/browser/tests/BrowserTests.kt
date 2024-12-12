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

class BrowserDeleteTests : AbstractDeleteTests(BrowserStoreProvider)

class BrowserDocumentDatabaseTests : AbstractDocumentDatabaseTests(BrowserStoreProvider)

class BrowserIndexTests : AbstractIndexTests(BrowserStoreProvider)

class BrowserInsertTests : AbstractInsertTests(BrowserStoreProvider)

class BrowserUpdateTests : AbstractUpdateTests(BrowserStoreProvider)

class BrowserFindTests : AbstractFindTests(BrowserStoreProvider)

class BrowserObjectCollectionTests : AbstractObjectCollectionTests(BrowserStoreProvider)

object BrowserStoreProvider : DataStoreProvider {
    override suspend fun deleteDatabase(testName: String) {
        keyval.clear().await()
    }

    override fun provide(testName: String): DataStore = BrowserStore
}
