@file:OptIn(InternalSerializationApi::class)

package kotlinx.document.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.InternalSerializationApi

class InsertTests : BaseTest(MVDataStore.open(dbPath)) {

    @Test
    fun `inserts and retrieves a document`() = runTest {
        val collection = db.getObjectCollection<TestUser>("test")
        val testUser = TestUser(
            name = "mario",
            age = 20,
            addresses = listOf(Address("street", 1))
        )

        collection.insert(testUser)

        assertEquals(
            expected = testUser,
            actual = collection.iterateAll().first(),
            message = "Collection should have 1 element"
        )

    }

}

