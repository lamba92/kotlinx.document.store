@file:Suppress("FunctionName")

package kotlinx.document.store.tests

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.TestResult
import kotlinx.document.store.find
import kotlinx.document.store.getObjectCollection
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

public abstract class AbstractFindTests(store: DataStoreProvider) : BaseTest(store) {
    public companion object {
        public const val TEST_NAME_1: String = "finds_a_document_using_index"
        public const val TEST_NAME_2: String = "finds_a_document_using_complex_index"
    }

    @Test
    @JsName(TEST_NAME_1)
    public fun `finds a document using index`(): TestResult =
        runDatabaseTest(TEST_NAME_1) { db ->
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
    @JsName(TEST_NAME_2)
    public fun `finds a document using complex index`(): TestResult =
        runDatabaseTest(TEST_NAME_2) { db ->
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
