@file:Suppress("unused")

package kotlinx.document.store.tests.stores.mvstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.document.store.mvstore.stores.MVDataStore
import kotlinx.document.store.tests.AbstractDeleteTests
import kotlinx.document.store.tests.AbstractDocumentDatabaseTests
import kotlinx.document.store.tests.AbstractFindTests
import kotlinx.document.store.tests.AbstractIndexTests
import kotlinx.document.store.tests.AbstractInsertTests
import kotlinx.document.store.tests.AbstractObjectCollectionTests
import kotlinx.document.store.tests.AbstractUpdateTests
import kotlinx.document.store.tests.DataStoreProvider
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively

object MVDataStoreProvider : DataStoreProvider {
    private fun getDbPath(testName: String) = Path(DB_PATH).resolve("$testName.mv.db")

    override fun provide(testName: String) =
        MVDataStore.open(
            getDbPath(testName)
                .apply { parent.createDirectories() }
                .absolutePathString(),
        )

    override suspend fun deleteDatabase(testName: String) =
        withContext(Dispatchers.IO) {
            getDbPath(testName).deleteRecursively()
        }
}

class MVStoreDeleteTests : AbstractDeleteTests(MVDataStoreProvider)

class MVStoreDocumentDatabaseTests : AbstractDocumentDatabaseTests(MVDataStoreProvider)

class MVStoreIndexTests : AbstractIndexTests(MVDataStoreProvider)

class MVStoreInsertTests : AbstractInsertTests(MVDataStoreProvider)

class MVStoreUpdateTests : AbstractUpdateTests(MVDataStoreProvider)

class MVStoreFindTests : AbstractFindTests(MVDataStoreProvider)

class MVStoreObjectCollectionTests : AbstractObjectCollectionTests(MVDataStoreProvider)

val DB_PATH: String by System.getenv()
