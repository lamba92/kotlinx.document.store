@file:Suppress("unused")

package kotlinx.document.database.tests.mvstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.document.database.mvstore.MVDataStore
import kotlinx.document.database.tests.AbstractDeleteTests
import kotlinx.document.database.tests.AbstractDocumentDatabaseTests
import kotlinx.document.database.tests.AbstractFindTests
import kotlinx.document.database.tests.AbstractIndexTests
import kotlinx.document.database.tests.AbstractInsertTests
import kotlinx.document.database.tests.AbstractObjectCollectionTests
import kotlinx.document.database.tests.AbstractUpdateTests
import kotlinx.document.database.tests.DatabaseDeleter
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists

class MVStoreDeleteTests :
    AbstractDeleteTests(MVDataStore.open(DB_PATH)),
    DatabaseDeleter by MVStoreDeleter

class MVStoreDocumentDatabaseTests :
    AbstractDocumentDatabaseTests(MVDataStore.open(DB_PATH)),
    DatabaseDeleter by MVStoreDeleter

class MVStoreIndexTests :
    AbstractIndexTests(MVDataStore.open(DB_PATH)),
    DatabaseDeleter by MVStoreDeleter

class MVStoreInsertTests :
    AbstractInsertTests(MVDataStore.open(DB_PATH)),
    DatabaseDeleter by MVStoreDeleter

class MVStoreUpdateTests :
    AbstractUpdateTests(MVDataStore.open(DB_PATH)),
    DatabaseDeleter by MVStoreDeleter

class MVStoreFindTests :
    AbstractFindTests(MVDataStore.open(DB_PATH)),
    DatabaseDeleter by MVStoreDeleter

class MVStoreObjectCollectionTests :
    AbstractObjectCollectionTests(MVDataStore.open(DB_PATH)),
    DatabaseDeleter by MVStoreDeleter

object MVStoreDeleter : DatabaseDeleter {
    override suspend fun deleteDatabase() {
        withContext(Dispatchers.IO) {
            Path(DB_PATH).deleteIfExists()
        }
    }
}

val DB_PATH: String by System.getenv()
