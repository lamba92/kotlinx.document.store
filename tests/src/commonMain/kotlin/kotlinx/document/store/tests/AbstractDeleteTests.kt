@file:Suppress("FunctionName")

package kotlinx.document.store.tests

import kotlinx.coroutines.flow.count
import kotlinx.coroutines.test.TestResult
import kotlinx.document.store.getObjectCollection
import kotlinx.document.store.removeWhere
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

public abstract class AbstractDeleteTests(store: DataStoreProvider) : BaseTest(store) {
    public companion object {
        public const val TEST_NAME_1: String = "deletes_collection"
        public const val TEST_NAME_2: String = "deleting_a_collection_actually_clears_it"
        public const val TEST_NAME_3: String = "deleting_a_collection_that_does_not_exist_does_nothing"
        public const val TEST_NAME_4: String = "deletes_a_document_with_index"
        public const val TEST_NAME_5: String = "deletes_a_document_using_selector_without_index"
        public const val TEST_NAME_6: String = "deletes_a_document_using_selector_with_index"
        public const val TEST_NAME_7: String = "deletes_a_document_using_complex_selector"
        public const val TEST_NAME_8: String = "deletes_a_document_using_complex_selector_with_index"
    }

    @Test
    @JsName(TEST_NAME_1)
    public fun `deletes collection`(): TestResult =
        runDatabaseTest(TEST_NAME_1) { db ->
            db.getObjectCollection<TestUser>("test")
            db.deleteCollection("test")
            assertEquals(
                expected = 0,
                actual = db.getAllCollectionNames().count(),
                message = "Database should have 0 collections",
            )
        }

    @Test
    @JsName(TEST_NAME_2)
    public fun `deleting a collection actually clears it`(): TestResult =
        runDatabaseTest(TEST_NAME_2) { db ->
            db.getObjectCollection<TestUser>("test").insert(TestUser.Mario)
            db.deleteCollection("test")
            assertEquals(
                expected = 0,
                actual = db.getObjectCollection<TestUser>("test").size(),
                message = "Collection should have 0 elements",
            )
        }

    @Test
    @JsName(TEST_NAME_3)
    public fun `deleting a collection that does not exist does nothing`(): TestResult =
        runDatabaseTest(TEST_NAME_3) { db ->
            db.deleteCollection("test")
            assertEquals(
                expected = 0,
                actual = db.getAllCollectionNames().count(),
                message = "Database should have 0 collections",
            )
        }

    @Test
    @JsName(TEST_NAME_4)
    public fun `deletes a document with index`(): TestResult =
        runDatabaseTest(TEST_NAME_4) { db ->
            val collection = db.getObjectCollection<TestUser>("test")
            collection.createIndex("name")
            val marioWithId = collection.insert(TestUser.Mario)
            val marioId = requireNotNull(marioWithId.id) { "Mario should have an id" }
            collection.removeById(marioId)
            assertEquals(
                expected = 0,
                actual = collection.iterateAll().count(),
                message = "Collection should have 0 elements",
            )
            assertEquals(
                expected = emptySet(),
                actual =
                    collection
                        .details()
                        .indexes.getValue("name")
                        .getValue(JsonPrimitive(TestUser.Mario.name)),
                message = "Index should be empty",
            )
        }

    @Test
    @JsName(TEST_NAME_5)
    public fun `deletes a document using selector without index`(): TestResult =
        runDatabaseTest(TEST_NAME_5) { db ->
            val collection = db.getObjectCollection<TestUser>("test")
            collection.insert(TestUser.Mario)
            collection.removeWhere(
                fieldSelector = "name",
                fieldValue = TestUser.Mario.name,
            )
            assertEquals(
                expected = 0,
                actual = collection.iterateAll().count(),
                message = "Collection should have 0 elements",
            )
        }

    @Test
    @JsName(TEST_NAME_6)
    public fun `deletes a document using selector with index`(): TestResult =
        runDatabaseTest(TEST_NAME_6) { db ->
            val collection = db.getObjectCollection<TestUser>("test")
            collection.createIndex("name")
            collection.insert(TestUser.Mario)
            collection.removeWhere(
                fieldSelector = "name",
                fieldValue = TestUser.Mario.name,
            )
            assertEquals(
                expected = 0,
                actual = collection.iterateAll().count(),
                message = "Collection should have 0 elements",
            )
            assertEquals(
                expected = emptySet(),
                actual =
                    collection.details()
                        .indexes
                        .getValue("name")
                        .getValue(JsonPrimitive(TestUser.Mario.name)),
                message = "Index should be empty",
            )
        }

    @Test
    @JsName(TEST_NAME_7)
    public fun `deletes a document using complex selector`(): TestResult =
        runDatabaseTest(TEST_NAME_7) { db ->
            val collection = db.getObjectCollection<TestUser>("test")
            collection.insert(TestUser.Mario)
            collection.removeWhere(
                fieldSelector = "addresses.$0",
                fieldValue = TestUser.Mario.addresses.first(),
            )
            assertEquals(
                expected = 0,
                actual = collection.iterateAll().count(),
                message = "Collection should have 0 elements",
            )
        }

    @Test
    @JsName(TEST_NAME_8)
    public fun `deletes a document using complex selector with index`(): TestResult =
        runDatabaseTest(TEST_NAME_8) { db ->
            val collection = db.getObjectCollection<TestUser>("test")
            collection.createIndex("addresses.$0")
            collection.insert(TestUser.Mario)
            collection.removeWhere(
                fieldSelector = "addresses.$0",
                fieldValue = TestUser.Mario.addresses.first(),
            )
            assertEquals(
                expected = 0,
                actual = collection.iterateAll().count(),
                message = "Collection should have 0 elements",
            )
            assertEquals(
                expected = emptySet(),
                actual =
                    collection.details()
                        .indexes
                        .getValue("addresses.$0")
                        .getValue(collection.json.encodeToJsonElement(TestUser.Mario.addresses.first())),
                message = "Index should be empty",
            )
        }
}
