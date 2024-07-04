@file:Suppress("FunctionName")

package kotlinx.document.database.tests

import kotlinx.coroutines.flow.toList
import kotlinx.document.database.DataStore
import kotlinx.document.database.getObjectCollection
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class AbstractDocumentDatabaseTests(store: DataStore) : BaseTest(store) {
    @Test
    @JsName("gets_all_collection_names")
    fun `gets all collection names`() =
        runDatabaseTest {
            db.getObjectCollection<TestUser>("test")
            db.getObjectCollection<TestUser>("test2")

            assertEquals(
                expected = listOf("test", "test2"),
                actual = db.getAllCollectionNames().toList(),
                message = "Database should have 2 collections",
            )
        }
}
