package kotlinx.document.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

class IndexTests : MVDataStoreTest() {

    companion object {
        val json = Json { prettyPrint = true }
    }

    @Test
    fun `id is correctly increased after insert`() = runTest {
        val collection = db.getObjectCollection<TestUser>("test")
        collection.insert(TestUser.Mario)

        val marioId = collection.jsonCollection.iterateAll()
            .first {
                it["name"]?.jsonPrimitive?.content == TestUser.Mario.name
            }["_id"]
            ?.jsonPrimitive
            ?.long
            ?: error("No id found")

        collection.insert(TestUser.Luigi)

        val luigiId = collection.jsonCollection.iterateAll()
            .first { it["name"]?.jsonPrimitive?.content == TestUser.Luigi.name }["_id"]
            ?.jsonPrimitive
            ?.long
            ?: error("No id found")

        println(json.encodeToString(db.databaseDetails()))
        assert(luigiId > marioId) { "Luigi's id should be greater than Mario's id" }

    }

    @Test
    fun `index is correctly created after insert`() = runTest {
        val collection = db.getObjectCollection<TestUser>("test")
        collection.insert(TestUser.Mario)
        collection.createIndex("name")

        val userId = collection.jsonCollection.iterateAll()
            .first()["_id"]
            ?.jsonPrimitive
            ?.long
            ?: error("No id found")

        assertEquals(
            expected = userId,
            actual = collection.getIndex("name")?.get("mario")?.single(),
            message = "Index should have 1 element"
        )

    }

    @Test
    fun `index is correctly created before insert`() = runTest {
        val collection = db.getObjectCollection<TestUser>("test")
        collection.createIndex("name")
        collection.insert(TestUser.Mario)

        val userId = collection.jsonCollection.iterateAll()
            .first()["_id"]
            ?.jsonPrimitive
            ?.long
            ?: error("No id found")

        assertEquals(
            expected = userId,
            actual = collection.getIndex("name")?.get("mario")?.single(),
            message = "Index should have 1 element"
        )

    }

    @Test
    fun `index is correctly created after insert and update`() = runTest {
        val collection = db.getObjectCollection<TestUser>("test")
        collection.insert(TestUser.Mario)
        collection.createIndex("name")

        val marioId = collection.jsonCollection.iterateAll()
            .first()["_id"]
            ?.jsonPrimitive
            ?.long
            ?: error("No id found")

        assertEquals(
            expected = marioId,
            actual = collection.getIndex("name")?.get("mario")?.single(),
            message = "Index should have 1 element"
        )
        println(json.encodeToString(db.databaseDetails()))
        collection.removeById(marioId)
        collection.insert(TestUser.Luigi)
        println(json.encodeToString(db.databaseDetails()))
        val luigiId = collection.jsonCollection.iterateAll()
            .first()["_id"]
            ?.jsonPrimitive
            ?.long
            ?: error("No id found")

        val actual = collection.getIndex("name")?.get("luigi")?.single()
        assertEquals(
            expected = luigiId,
            actual = actual,
            message = "Index should have 1 element"
        )

    }

}