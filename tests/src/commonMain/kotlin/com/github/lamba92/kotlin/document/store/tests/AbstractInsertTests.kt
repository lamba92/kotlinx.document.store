@file:Suppress("FunctionName")

package com.github.lamba92.kotlin.document.store.tests

import com.github.lamba92.kotlin.document.store.core.find
import com.github.lamba92.kotlin.document.store.core.getObjectCollection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.assertEquals

/**
 * Abstract base class for testing insertion functionality within a document store.
 *
 * It extends [BaseTest] and provides tests for validating the insertion and retrieval processes
 * of documents. The tests include scenarios with and without indexes, as well as using complex
 * indexes, to ensure that documents are correctly stored and retrieved in the expected manner.
 *
 * This class is intended to be extended to define specific implementations of `DataStoreProvider`
 * for different Kotlin platforms.
 */
public abstract class AbstractInsertTests(store: DataStoreProvider) : BaseTest(store) {
    public companion object {
        public const val TEST_NAME_1: String = "inserts_and_retrieves_a_document_without_index"
        public const val TEST_NAME_2: String = "inserts_and_retrieves_a_document_with_index"
        public const val TEST_NAME_3: String = "inserts_and_retrieves_a_document_using_complex_index"
    }

    @Test
    public fun insertsAndRetrievesADocumentWithoutIndex(): TestResult =
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
    public fun insertsAndRetrievesADocumentWithIndex(): TestResult =
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
    public fun insertsAndRetrievesADocumentUsingComplexIndex(): TestResult =
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
