package kotlinx.document.database.tests

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import kotlinx.document.database.DataStore
import kotlinx.document.database.find
import kotlinx.document.database.getObjectCollection
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class AbstractFindTests(store: DataStore) : BaseTest(store) {
    @Test
    @JsName("finds_a_document_using_index")
    fun `finds a document using index`() =
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

    @Test
    @JsName("finds_a_document_using_complex_index")
    fun `finds a document using complex index`() =
        runDatabaseTest {
            val collection = db.getObjectCollection<TestUser>("test")
            collection.createIndex("addresses.$0")

            val expected = collection.insert(TestUser.Mario)

            assertEquals(
                expected = expected,
                actual = collection.find("addresses.$0", expected.addresses[0]).single(),
                message = "Collection should have 1 element",
            )
        }
}
