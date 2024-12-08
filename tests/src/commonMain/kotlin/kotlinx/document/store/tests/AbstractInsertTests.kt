@file:Suppress("FunctionName")

package kotlinx.document.store.tests

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.document.store.find
import kotlinx.document.store.getObjectCollection
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

public abstract class AbstractInsertTests(store: DataStoreProvider) : BaseTest(store) {
    public companion object {
        public const val TEST_NAME_1: String = "inserts_and_retrieves_a_document_without_index"
        public const val TEST_NAME_2: String = "inserts_and_retrieves_a_document_with_index"
        public const val TEST_NAME_3: String = "inserts_and_retrieves_a_document_using_complex_index"
    }

    @Test
    @JsName(TEST_NAME_1)
    public fun `inserts and retrieves a document without index`(): TestResult =
        runDatabaseTest(TEST_NAME_1) { db ->
            val collection = db.getObjectCollection<TestUser>("test")
            val testUser =
                TestUser(
                    name = "mario",
                    age = 20,
                    addresses = listOf(Address("street", 1)),
                )

            val expected = collection.insert(testUser)

            assertEquals(
                expected = expected,
                actual = collection.iterateAll().first(),
                message = "Collection should have 1 element",
            )
        }

    @Test
    @JsName(TEST_NAME_2)
    public fun `inserts and retrieves a document with index`(): TestResult =
        runDatabaseTest(TEST_NAME_2) { db ->
            val collection = db.getObjectCollection<TestUser>("test")
            collection.createIndex("name")

            val expected = collection.insert(TestUser.Mario)
            val expectedId = expected.id ?: error("Id should not be null")

            assertEquals(
                expected = expected,
                actual = collection.findById(expectedId),
                message = "Collection should have 1 element",
            )

            assertEquals(
                expected =
                    collection.details()
                        .indexes
                        .getValue("name")
                        .getValue(JsonPrimitive(expected.name)),
                actual = setOf(expectedId),
                message = "Index should be the one of Mario",
            )
        }

    @Test
    @JsName(TEST_NAME_3)
    public fun `inserts and retrieves a document using complex index`(): TestResult =
        runDatabaseTest(TEST_NAME_3) { db ->
            val collection = db.getObjectCollection<TestUser>("test")
            collection.createIndex("addresses.$0")

            val expected = collection.insert(TestUser.Mario)

            val actual =
                collection.find(
                    selector = "addresses.$0",
                    value = expected.addresses[0],
                ).first()

            assertEquals(
                expected = expected,
                actual = actual,
                message = "Collection should have 1 element",
            )

            assertEquals(
                expected =
                    collection.details()
                        .indexes
                        .getValue("addresses.$0")
                        .getValue(collection.json.encodeToJsonElement(expected.addresses[0])),
                actual = setOf(expected.id),
                message = "Index should be the one of Mario",
            )
        }
}
