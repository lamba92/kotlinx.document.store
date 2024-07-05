@file:Suppress("unused")

package kotlinx.document.database.browser.tests

import kotlinx.coroutines.await
import kotlinx.document.database.browser.IndexedDBStore
import kotlinx.document.database.tests.AbstractDeleteTests
import kotlinx.document.database.tests.AbstractDocumentDatabaseTests
import kotlinx.document.database.tests.AbstractIndexTests
import kotlinx.document.database.tests.AbstractInsertTests
import kotlinx.document.database.tests.AbstractObjectCollectionTests
import kotlinx.document.database.tests.DatabaseDeleter

class BrowserDeleteTests :
    AbstractDeleteTests(IndexedDBStore),
    DatabaseDeleter by BrowserDeleter

class BrowserDocumentDatabaseTests :
    AbstractDocumentDatabaseTests(IndexedDBStore),
    DatabaseDeleter by BrowserDeleter

class BrowserIndexTests :
    AbstractIndexTests(IndexedDBStore),
    DatabaseDeleter by BrowserDeleter

class BrowserInsertTests :
    AbstractInsertTests(IndexedDBStore),
    DatabaseDeleter by BrowserDeleter

class BrowserObjectCollectionTests :
    AbstractObjectCollectionTests(IndexedDBStore),
    DatabaseDeleter by BrowserDeleter

object BrowserDeleter : DatabaseDeleter {
    override suspend fun deleteDatabase() {
        keyval.clear().await()
    }
}
