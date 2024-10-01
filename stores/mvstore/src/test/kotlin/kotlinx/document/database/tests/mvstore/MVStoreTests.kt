@file:Suppress("unused")

package kotlinx.document.database.tests.mvstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.document.database.DataStore
import kotlinx.document.database.mvstore.MVDataStore
import kotlinx.document.database.tests.AbstractCacheOverflowTests
import kotlinx.document.database.tests.AbstractDeleteTests
import kotlinx.document.database.tests.AbstractDocumentDatabaseTests
import kotlinx.document.database.tests.AbstractFindTests
import kotlinx.document.database.tests.AbstractIndexTests
import kotlinx.document.database.tests.AbstractInsertTests
import kotlinx.document.database.tests.AbstractObjectCollectionTests
import kotlinx.document.database.tests.AbstractOnChangeCommitStrategyTests
import kotlinx.document.database.tests.AbstractUpdateTests
import kotlinx.document.database.tests.DatabaseDeleter
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists

class MVStoreDeleteTests :
    AbstractDeleteTests(MVDataStore.open(Path(DB_PATH), DataStore.CommitStrategy.OnChange)),
    DatabaseDeleter by MVStoreDeleter

class MVStoreDocumentDatabaseTests :
    AbstractDocumentDatabaseTests(MVDataStore.open(Path(DB_PATH), DataStore.CommitStrategy.OnChange)),
    DatabaseDeleter by MVStoreDeleter

class MVStoreIndexTests :
    AbstractIndexTests(MVDataStore.open(Path(DB_PATH), DataStore.CommitStrategy.OnChange)),
    DatabaseDeleter by MVStoreDeleter

class MVStoreInsertTests :
    AbstractInsertTests(MVDataStore.open(Path(DB_PATH), DataStore.CommitStrategy.OnChange)),
    DatabaseDeleter by MVStoreDeleter

class MVStoreUpdateTests :
    AbstractUpdateTests(MVDataStore.open(Path(DB_PATH), DataStore.CommitStrategy.OnChange)),
    DatabaseDeleter by MVStoreDeleter

class MVStoreFindTests :
    AbstractFindTests(MVDataStore.open(Path(DB_PATH), DataStore.CommitStrategy.OnChange)),
    DatabaseDeleter by MVStoreDeleter

class MVStoreObjectCollectionTests :
    AbstractObjectCollectionTests(MVDataStore.open(Path(DB_PATH), DataStore.CommitStrategy.OnChange)),
    DatabaseDeleter by MVStoreDeleter

// todo flaky
// class MVStorePeriodicCommitStrategyTests :
//    AbstractPeriodicCommitStrategyTests(
//        MVDataStore.open(
//            Path(DB_PATH),
//            DataStore.CommitStrategy.Periodic(commitInterval),
//        ),
//    ),
//    DatabaseDeleter by MVStoreDeleter {
//    override fun getUnsavedMemory() = (store as MVDataStore).delegate.unsavedMemory
//
//    override fun getTotalCacheMemorySize(): Int = (store as MVDataStore).delegate.autoCommitMemory
// }

class MVStoreOnChangeCommitStrategyTests :
    AbstractOnChangeCommitStrategyTests(store = MVDataStore.open(Path(DB_PATH), DataStore.CommitStrategy.OnChange)),
    DatabaseDeleter by MVStoreDeleter {
    override fun getStoreFileSize(): Long = File(DB_PATH).listFiles()?.sumOf { it.length() } ?: File(DB_PATH).length()
}

class MVStoreCacheOverflowTests :
    AbstractCacheOverflowTests(
        MVDataStore.open(
            Path(DB_PATH),
            DataStore.CommitStrategy.Periodic(commitInterval),
        ),
    ),
    DatabaseDeleter by MVStoreDeleter {
    override fun getUnsavedMemory() = (store as MVDataStore).delegate.unsavedMemory

    override fun getTotalCacheMemorySize(): Int = (store as MVDataStore).delegate.autoCommitMemory
}

object MVStoreDeleter : DatabaseDeleter {
    override suspend fun deleteDatabase() {
        withContext(Dispatchers.IO) {
            Path(DB_PATH).deleteIfExists()
        }
    }
}

val DB_PATH: String by System.getenv()
