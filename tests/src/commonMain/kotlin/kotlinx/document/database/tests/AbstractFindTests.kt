package kotlinx.document.database.tests

import kotlinx.coroutines.flow.first
import kotlinx.document.database.DataStore
import kotlinx.document.database.find
import kotlinx.document.database.getObjectCollection
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class AbstractFindTests(store: DataStore) : BaseTest(store) {
    @Test
    @JsName("finds_a_document_by_index")
    fun `finds a document by index`() =
        runDatabaseTest {
            val collection = db.getObjectCollection<TestUser>("test")
            collection.createIndex("name")

            val expected = collection.insert(TestUser.Mario)

            assertEquals(
                expected = expected,
                actual = collection.find("name", expected.name).first(),
                message = "Collection should have 1 element",
            )
        }
}
