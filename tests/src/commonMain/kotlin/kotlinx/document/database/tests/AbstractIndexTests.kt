@file:Suppress("FunctionName")

package kotlinx.document.database.tests

import kotlinx.coroutines.flow.first
import kotlinx.document.database.DataStore
import kotlinx.document.database.getObjectCollection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class AbstractIndexTests(store: DataStore) : BaseTest(store) {
    companion object {
        val json = Json { prettyPrint = true }
    }

    @Test
    @JsName("id_is_correctly_increased_after_insert")
    fun `id is correctly increased after insert`() =
        runDatabaseTest {
            val collection = db.getObjectCollection<TestUser>("test")
            collection.insert(TestUser.Mario)

            val marioId =
                collection.jsonCollection.iterateAll()
                    .first { it["name"]?.jsonPrimitive?.content == TestUser.Mario.name }["_id"]
                    ?.jsonPrimitive
                    ?.long
                    ?: error("No id found")

            collection.insert(TestUser.Luigi)

            val luigiId =
                collection.jsonCollection.iterateAll()
                    .first { it["name"]?.jsonPrimitive?.content == TestUser.Luigi.name }["_id"]
                    ?.jsonPrimitive
                    ?.long
                    ?: error("No id found")

            println(json.encodeToString(db.databaseDetails()))
            assertTrue(luigiId > marioId, "Luigi's id should be greater than Mario's id")
        }

    @Test
    @JsName("index_is_correctly_created_after_insert")
    fun `index is correctly created after insert`() =
        runDatabaseTest {
            val collection = db.getObjectCollection<TestUser>("test")
            collection.insert(TestUser.Mario)
            collection.createIndex("name")

            val userId =
                collection.jsonCollection.iterateAll()
                    .first()["_id"]
                    ?.jsonPrimitive
                    ?.long
                    ?: error("No id found")

            assertEquals(
                expected = userId,
                actual = collection.getIndex("name")?.get("mario")?.single(),
                message = "Index should have 1 element",
            )
        }

    @Test
    @JsName("index_is_correctly_created_before_insert")
    fun `index is correctly created before insert`() =
        runDatabaseTest {
            val collection = db.getObjectCollection<TestUser>("test")
            collection.createIndex("name")
            collection.insert(TestUser.Mario)

            val userId =
                collection.jsonCollection.iterateAll()
                    .first()["_id"]
                    ?.jsonPrimitive
                    ?.long
                    ?: error("No id found")

            assertEquals(
                expected = userId,
                actual = collection.getIndex("name")?.get("mario")?.single(),
                message = "Index should have 1 element",
            )
        }

    @Test
    @JsName("index_is_correctly_created_after_insert_and_update")
    fun `index is correctly created after insert and update`() =
        runDatabaseTest {
            val collection = db.getObjectCollection<TestUser>("test")
            collection.insert(TestUser.Mario)
            collection.createIndex("name")

            val marioId =
                collection.jsonCollection.iterateAll()
                    .first()["_id"]
                    ?.jsonPrimitive
                    ?.long
                    ?: error("No id found")

            assertEquals(
                expected = marioId,
                actual = collection.getIndex("name")?.get("mario")?.single(),
                message = "Index should have 1 element",
            )
            println(json.encodeToString(db.databaseDetails()))
            collection.removeById(marioId)
            collection.insert(TestUser.Luigi)
            println(json.encodeToString(db.databaseDetails()))
            val luigiId =
                collection.jsonCollection.iterateAll()
                    .first()["_id"]
                    ?.jsonPrimitive
                    ?.long
                    ?: error("No id found")

            val actual = collection.getIndex("name")?.get("luigi")?.single()
            assertEquals(
                expected = luigiId,
                actual = actual,
                message = "Index should have 1 element",
            )
        }
}
