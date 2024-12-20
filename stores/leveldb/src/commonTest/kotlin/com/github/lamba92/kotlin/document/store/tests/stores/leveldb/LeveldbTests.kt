@file:Suppress("unused")

package com.github.lamba92.kotlin.document.store.tests.stores.leveldb

import com.github.lamba92.kotlin.document.store.core.DataStore
import com.github.lamba92.kotlin.document.store.stores.leveldb.LevelDBStore
import com.github.lamba92.kotlin.document.store.tests.AbstractDeleteTests
import com.github.lamba92.kotlin.document.store.tests.AbstractDocumentDatabaseTests
import com.github.lamba92.kotlin.document.store.tests.AbstractFindTests
import com.github.lamba92.kotlin.document.store.tests.AbstractIndexTests
import com.github.lamba92.kotlin.document.store.tests.AbstractInsertTests
import com.github.lamba92.kotlin.document.store.tests.AbstractObjectCollectionTests
import com.github.lamba92.kotlin.document.store.tests.AbstractUpdateTests
import com.github.lamba92.kotlin.document.store.tests.DataStoreProvider
import com.github.lamba92.leveldb.LevelDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class LevelDBDeleteTests : AbstractDeleteTests(LevelDBStoreProvider)

class LevelDBDocumentDatabaseTests : AbstractDocumentDatabaseTests(LevelDBStoreProvider)

class LevelDBIndexTests : AbstractIndexTests(LevelDBStoreProvider)

class LevelDBInsertTests : AbstractInsertTests(LevelDBStoreProvider)

class LevelDBUpdateTests : AbstractUpdateTests(LevelDBStoreProvider)

class LevelDBFindTests : AbstractFindTests(LevelDBStoreProvider)

class LevelDBObjectCollectionTests : AbstractObjectCollectionTests(LevelDBStoreProvider)

object LevelDBStoreProvider : DataStoreProvider {
    private fun getDbPath(testName: String) = Path(DB_PATH).resolve(testName)

    override suspend fun deleteDatabase(testName: String) =
        withContext(Dispatchers.IO) {
            getDbPath(testName).deleteRecursively()
        }

    override fun provide(testName: String): DataStore =
        LevelDBStore(
            LevelDB(
                getDbPath(testName)
                    .createDirectories()
                    .toString(),
            ),
        )
}

fun Path.resolve(vararg other: String): Path = Path(this, *other)

fun Path.createDirectories(): Path {
    SystemFileSystem.createDirectories(this, false)
    return this
}

expect val DB_PATH: String

val Path.isFile: Boolean
    get() = SystemFileSystem.metadataOrNull(this)?.isRegularFile == true

val Path.isDirectory
    get() = SystemFileSystem.metadataOrNull(this)?.isDirectory == true

fun Path.delete() = SystemFileSystem.delete(this, false)

fun Path.deleteRecursively() {
    if (isDirectory) {
        SystemFileSystem.list(this@deleteRecursively)
            .forEach {
                when {
                    it.isFile -> it.delete()
                    it.isDirectory -> it.deleteRecursively()
                }
            }
    }
    delete()
}
