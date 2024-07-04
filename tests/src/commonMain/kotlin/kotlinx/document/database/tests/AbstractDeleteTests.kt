@file:Suppress("FunctionName")

package kotlinx.document.database.tests

import kotlinx.coroutines.flow.count
import kotlinx.document.database.DataStore
import kotlinx.document.database.getObjectCollection
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class AbstractDeleteTests(store: DataStore) : BaseTest(store) {
    @Test
    @JsName("deletes_collection")
    fun `deletes collection`() =
        runDatabaseTest {
            db.getObjectCollection<TestUser>("test")
            db.deleteCollection("test")
            assertEquals(
                expected = 0,
                actual = db.getAllCollectionNames().count(),
                message = "Database should have 0 collections",
            )
        }

    @Test
    @JsName("deleting_a_collection_actually_clears_it")
    fun `deleting a collection actually clears it`() =
        runDatabaseTest {
            db.getObjectCollection<TestUser>("test").insert(TestUser.Mario)
            db.deleteCollection("test")
            assertEquals(
                expected = 0,
                actual = db.getObjectCollection<TestUser>("test").size(),
                message = "Collection should have 0 elements",
            )
        }

    @Test
    @JsName("deleting_a_collection_that_does_not_exist_does_nothing")
    fun `deleting a collection that does not exist does nothing`() =
        runDatabaseTest {
            db.deleteCollection("test")
            assertEquals(
                expected = 0,
                actual = db.getAllCollectionNames().count(),
                message = "Database should have 0 collections",
            )
        }
}
