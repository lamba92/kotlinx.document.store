@file:Suppress("unused")

package kotlinx.document.database.tests.rocksdb

import kotlinx.document.database.rocksdb.RocksdbDataStore
import kotlinx.document.database.tests.AbstractDeleteTests
import kotlinx.document.database.tests.AbstractDocumentDatabaseTests
import kotlinx.document.database.tests.AbstractIndexTests
import kotlinx.document.database.tests.AbstractInsertTests
import kotlinx.document.database.tests.AbstractObjectCollectionTests
import kotlinx.document.database.tests.AbstractUpdateTests
import kotlinx.document.database.tests.DatabaseDeleter
import kotlinx.io.files.Path

class RocksdbDeleteTests :
    AbstractDeleteTests(RocksdbDataStore.open(DB_PATH)),
    DatabaseDeleter by RocksdbDeleter

class RocksdbDocumentDatabaseTests :
    AbstractDocumentDatabaseTests(RocksdbDataStore.open(DB_PATH)),
    DatabaseDeleter by RocksdbDeleter

class RocksdbIndexTests :
    AbstractIndexTests(RocksdbDataStore.open(DB_PATH)),
    DatabaseDeleter by RocksdbDeleter

class RocksdbInsertTests :
    AbstractInsertTests(RocksdbDataStore.open(DB_PATH)),
    DatabaseDeleter by RocksdbDeleter

class RocksdbUpdateTests :
    AbstractUpdateTests(RocksdbDataStore.open(DB_PATH)),
    DatabaseDeleter by RocksdbDeleter

class RocksdbObjectCollectionTests :
    AbstractObjectCollectionTests(RocksdbDataStore.open(DB_PATH)),
    DatabaseDeleter by RocksdbDeleter

object RocksdbDeleter : DatabaseDeleter {
    override suspend fun deleteDatabase() = Path(DB_PATH).deleteRecursively()
}

expect suspend fun Path.deleteRecursively()

expect val DB_PATH: String
