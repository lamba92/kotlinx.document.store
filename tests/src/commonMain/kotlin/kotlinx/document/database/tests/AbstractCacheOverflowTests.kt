package kotlinx.document.database.tests

import kotlinx.document.database.DataStore
import kotlinx.document.database.getObjectCollection
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

abstract class AbstractCacheOverflowTests(val store: DataStore) : BaseTest(store) {
    companion object {
        val commitInterval = 1.days
    }

    abstract fun getUnsavedMemory(): Int

    abstract fun getTotalCacheMemorySize(): Int

    @Test
    @JsName("cache_is_flushed_when_memory_is_full")
    fun `test if Cache Is flushed when memory is full`() =
        runDatabaseTest {
            assertTrue(
                actual = store.commitStrategy is DataStore.CommitStrategy.Periodic,
                message = "Commit policy should be periodic",
            )
            assertEquals(
                expected = 1.days,
                actual = (store.commitStrategy as DataStore.CommitStrategy.Periodic).interval,
                message = "Commit interval should be set to 1 day for this test",
            )

            val memorySize = getTotalCacheMemorySize()
            val collection = db.getObjectCollection<TestUser>("test")

            var lastAvailableMemory = getTotalCacheMemorySize() - getUnsavedMemory()
            val testUsers = TestUser.generateUsers(1000)

            while (true) {
                testUsers.forEach { collection.insert(it) }
                val actualAvailableMemory = memorySize - getUnsavedMemory()

                if (lastAvailableMemory < actualAvailableMemory) {
                    assertTrue(lastAvailableMemory < actualAvailableMemory)
                    break
                }
                assertTrue(lastAvailableMemory >= actualAvailableMemory)
                lastAvailableMemory = actualAvailableMemory
            }
        }
}
