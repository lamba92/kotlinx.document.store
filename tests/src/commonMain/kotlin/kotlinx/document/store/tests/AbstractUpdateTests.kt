@file:Suppress("FunctionName")

package kotlinx.document.store.tests

import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.TestResult
import kotlinx.document.store.getObjectCollection
import kotlinx.document.store.updateWhere
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Abstract base class for testing update functionality within a document store.
 *
 * It extends [BaseTest] and validates both updating and upserting (updating or inserting) operations
 * on documents. The tests include scenarios with and without indexes to ensure the expected behavior
 * when modifying existing documents or adding them when they don’t exist.
 *
 * This class is intended to be extended to implement platform-specific `DataStoreProvider` behaviors.
 */
public abstract class AbstractUpdateTests(store: DataStoreProvider) : BaseTest(store) {
    public companion object {
        public const val TEST_NAME_1: String = "updates_a_document_without_index"
        public const val TEST_NAME_2: String = "updates_a_document_with_index"
        public const val TEST_NAME_3: String = "upsert_inserts_a_document_without_index"
        public const val TEST_NAME_4: String = "upsert_inserts_a_document_with_index"
    }

    @Test
    public fun updatesADocumentWithoutIndex(): TestResult =
        runDatabaseTest(TEST_NAME_1) { db ->
            val collection = db.getObjectCollection<TestUser>("test")

            val marioWithId = collection.insert(TestUser.Mario)
            val marioId = requireNotNull(marioWithId.id) { "Mario should have an id" }
            val antonio = marioWithId.copy(name = "Antonio")
            collection.updateById(marioId) { antonio }

            assertEquals(
                expected = antonio,
                actual = collection.iterateAll().single(),
                message = "Collection should have 1 element",
            )
        }

    @Test
    public fun updatesADocumentWithIndex(): TestResult =
        runDatabaseTest(TEST_NAME_2) { db ->
            val collection = db.getObjectCollection<TestUser>("test")
            collection.createIndex("name")
            val marioWithId = collection.insert(TestUser.Mario)

            val marioId = requireNotNull(marioWithId.id) { "Mario should have an id" }
            val antonio = marioWithId.copy(name = "Antonio")
            collection.updateById(marioId) { antonio }

            assertEquals(
                expected = antonio,
                actual = collection.iterateAll().single(),
                message = "Collection should have 1 element",
            )

            assertEquals(
                expected =
                    collection.details()
                        .indexes
                        .getValue("name")
                        .getValue(JsonPrimitive(antonio.name)),
                actual = setOf(antonio.id),
                message = "Index should be the one of Antonio",
            )
        }

    @Test
    public fun upsertInsertsADocumentWithoutIndex(): TestResult =
        runDatabaseTest(TEST_NAME_3) { db ->
            val collection = db.getObjectCollection<TestUser>("test")

            collection.updateWhere(
                fieldSelector = "name",
                fieldValue = TestUser.Mario.name,
                upsert = true,
                update = TestUser.Mario,
            )

            val marioWithId = collection.iterateAll().single()

            assertEquals(
                expected = TestUser.Mario.copy(id = marioWithId.id),
                actual = marioWithId,
                message = "Collection should have 1 element",
            )
        }

    @Test
    public fun upsertInsertsADocumentWithIndex(): TestResult =
        runDatabaseTest(TEST_NAME_4) { db ->
            val collection = db.getObjectCollection<TestUser>("test")
            collection.createIndex("name")

            val result =
                collection.updateWhere(
                    fieldSelector = "name",
                    fieldValue = TestUser.Mario.name,
                    upsert = true,
                    update = TestUser.Mario,
                )

            assertTrue(result, "Upsert should have inserted a document")

            val marioWithId = collection.iterateAll().single()

            assertEquals(
                expected = TestUser.Mario.copy(id = marioWithId.id),
                actual = marioWithId,
                message = "Collection should have 1 element",
            )

            assertEquals(
                expected =
                    collection.details()
                        .indexes
                        .getValue("name")
                        .getValue(JsonPrimitive(marioWithId.name)),
                actual = setOf(marioWithId.id),
                message = "Index should be the one of Mario",
            )
        }
}
