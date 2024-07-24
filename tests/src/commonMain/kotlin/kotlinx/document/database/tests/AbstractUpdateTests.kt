package kotlinx.document.database.tests

import kotlinx.coroutines.flow.single
import kotlinx.document.database.DataStore
import kotlinx.document.database.getObjectCollection
import kotlinx.document.database.updateWhere
import kotlinx.serialization.json.JsonPrimitive
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class AbstractUpdateTests(store: DataStore) : BaseTest(store) {
    @Test
    @JsName("updates_a_document_without_index")
    fun `updates a document without index`() =
        runDatabaseTest {
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
    @JsName("updates_a_document_with_index")
    fun `updates a document with index`() =
        runDatabaseTest {
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
    @JsName("upsert_inserts_a_document_without_index")
    fun `upsert inserts a document without index`() =
        runDatabaseTest {
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
    @JsName("upsert_inserts_a_document_with_index")
    fun `upsert inserts a document with index`() =
        runDatabaseTest {
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
