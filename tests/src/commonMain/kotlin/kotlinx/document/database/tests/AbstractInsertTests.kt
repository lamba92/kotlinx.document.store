package kotlinx.document.database.tests

import kotlinx.coroutines.flow.first
import kotlinx.document.database.DataStore
import kotlinx.document.database.getObjectCollection
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class AbstractInsertTests(store: DataStore) : BaseTest(store) {
    @Test
    @JsName("inserts_and_retrieves_a_document")
    fun `inserts and retrieves a document`() =
        runDatabaseTest {
            val collection = db.getObjectCollection<TestUser>("test")
            val testUser =
                TestUser(
                    name = "mario",
                    age = 20,
                    addresses = listOf(Address("street", 1)),
                )

            collection.insert(testUser)

            assertEquals(
                expected = testUser,
                actual = collection.iterateAll().first(),
                message = "Collection should have 1 element",
            )
        }
}
