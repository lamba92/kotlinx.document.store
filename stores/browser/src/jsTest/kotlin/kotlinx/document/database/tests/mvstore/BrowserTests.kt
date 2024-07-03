@file:Suppress("unused")

package kotlinx.document.database.tests.mvstore

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.document.database.browser.IndexedDBStore
import kotlinx.document.database.tests.AbstractDeleteTests
import kotlinx.document.database.tests.AbstractDocumentDatabaseTests
import kotlinx.document.database.tests.AbstractIndexTests
import kotlinx.document.database.tests.AbstractInsertTests
import kotlinx.document.database.tests.AbstractObjectCollectionTests
import kotlinx.document.database.tests.DatabaseDeleter

val databaseName = "a-test"

class BrowserDeleteTests : AbstractDeleteTests(IndexedDBStore(databaseName)),
    DatabaseDeleter by BrowserDeleter

class BrowserDocumentDatabaseTests : AbstractDocumentDatabaseTests(IndexedDBStore(databaseName)),
    DatabaseDeleter by BrowserDeleter

class BrowserIndexTests : AbstractIndexTests(IndexedDBStore(databaseName)),
    DatabaseDeleter by BrowserDeleter

class BrowserInsertTests : AbstractInsertTests(IndexedDBStore(databaseName)),
    DatabaseDeleter by BrowserDeleter

class BrowserObjectCollectionTests : AbstractObjectCollectionTests(IndexedDBStore(databaseName)),
    DatabaseDeleter by BrowserDeleter

object BrowserDeleter : DatabaseDeleter {
    override suspend fun deleteDatabase() {
        suspendCoroutine { continuation ->
            val request = js("window").indexedDB.deleteDatabase(databaseName)
            request.onsuccess = {
                continuation.resume(Unit)
            }
            request.onerror = {
                continuation.resumeWithException(RuntimeException("Error deleting database."))
            }
        }
    }
}
