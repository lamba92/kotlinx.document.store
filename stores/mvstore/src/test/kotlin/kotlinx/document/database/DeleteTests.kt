package kotlinx.document.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.test.runTest

class DeleteTests : BaseTest(MVDataStore.open(dbPath)) {
    @Test
    fun `deletes collection`() = runTest {
        db.getObjectCollection<TestUser>("test")
        db.deleteCollection("test")
        assertEquals(
            expected = 0,
            actual = db.getAllCollectionNames().count(),
            message = "Database should have 0 collections"
        )
    }

    @Test
    fun `deleting a collection actually clears it`() = runTest {
        db.getObjectCollection<TestUser>("test").insert(TestUser.Mario)
        db.deleteCollection("test")
        assertEquals(
            expected = 0,
            actual = db.getObjectCollection<TestUser>("test").size(),
            message = "Collection should have 0 elements"
        )
    }

    @Test
    fun `deleting a collection that does not exist does nothing`() = runTest {
        db.deleteCollection("test")
        assertEquals(
            expected = 0,
            actual = db.getAllCollectionNames().count(),
            message = "Database should have 0 collections"
        )
    }

}