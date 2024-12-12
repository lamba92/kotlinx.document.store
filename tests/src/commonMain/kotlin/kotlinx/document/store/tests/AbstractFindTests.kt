@file:Suppress("FunctionName")

package kotlinx.document.store.tests

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.TestResult
import kotlinx.document.store.KotlinxDocumentStore
import kotlinx.document.store.find
import kotlinx.document.store.getObjectCollection
import kotlin.test.assertEquals

/**
 * Abstract base class for testing find functionality within a document store.
 *
 * It extends [BaseTest] and provides a set of tests that validate the behavior of various
 * find operations performed on collections and documents within a [KotlinxDocumentStore].
 * The tests include functionality for finding documents using both simple and complex indexes
 * to ensure precise data retrieval in diverse use cases.
 *
 * This class is intended to be extended to define specific implementations of `DataStoreProvider`
 * based on the Kotlin platform being used.
 */
public abstract class AbstractFindTests(store: DataStoreProvider) : BaseTest(store) {
    public companion object {
        public const val TEST_NAME_1: String = "finds_a_document_using_index"
        public const val TEST_NAME_2: String = "finds_a_document_using_complex_index"
    }

    @Test
    public fun findsADocumentUsingIndex(): TestResult =
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
    public fun findsADocumentUsingComplexIndex(): TestResult =
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
