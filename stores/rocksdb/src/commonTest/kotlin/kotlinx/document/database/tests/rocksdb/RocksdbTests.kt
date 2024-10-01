@file:Suppress("unused")

package kotlinx.document.database.tests.rocksdb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.document.database.DataStore
import kotlinx.document.database.rocksdb.RocksdbDataStore
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
import kotlin.io.path.deleteRecursively

class RocksdbDeleteTests :
    AbstractDeleteTests(RocksdbDataStore.open(Path(DB_PATH), DataStore.CommitStrategy.OnChange)),
    DatabaseDeleter by RocksdbDeleter

class RocksdbDocumentDatabaseTests :
    AbstractDocumentDatabaseTests(RocksdbDataStore.open(Path(DB_PATH), DataStore.CommitStrategy.OnChange)),
    DatabaseDeleter by RocksdbDeleter

class RocksdbIndexTests :
    AbstractIndexTests(RocksdbDataStore.open(Path(DB_PATH), DataStore.CommitStrategy.OnChange)),
    DatabaseDeleter by RocksdbDeleter

class RocksdbInsertTests :
    AbstractInsertTests(RocksdbDataStore.open(Path(DB_PATH), DataStore.CommitStrategy.OnChange)),
    DatabaseDeleter by RocksdbDeleter

class RocksdbUpdateTests :
    AbstractUpdateTests(RocksdbDataStore.open(Path(DB_PATH), DataStore.CommitStrategy.OnChange)),
    DatabaseDeleter by RocksdbDeleter

class RocksdbFindTests :
    AbstractFindTests(RocksdbDataStore.open(Path(DB_PATH), DataStore.CommitStrategy.OnChange)),
    DatabaseDeleter by RocksdbDeleter

class RocksdbObjectCollectionTests :
    AbstractObjectCollectionTests(RocksdbDataStore.open(Path(DB_PATH), DataStore.CommitStrategy.OnChange)),
    DatabaseDeleter by RocksdbDeleter

class RocksdbOnChangeCommitStrategyTests :
    AbstractOnChangeCommitStrategyTests(
        RocksdbDataStore.open(Path(DB_PATH), DataStore.CommitStrategy.OnChange),
    ),
    DatabaseDeleter by RocksdbDeleter {
    override fun getStoreFileSize(): Long = File(DB_PATH).listFiles()?.sumOf { it.length() } ?: File(DB_PATH).length()
}

// TODO implement RocksdbCommitStrategies Tests

object RocksdbDeleter : DatabaseDeleter {
    override suspend fun deleteDatabase() =
        withContext(Dispatchers.IO) {
            Path(DB_PATH).toRealPath()
        }.deleteRecursively()
}

// expect suspend fun Path.deleteRecursively()

expect val DB_PATH: String
