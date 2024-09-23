package kotlinx.document.database.tests

import kotlinx.document.database.DataStore
import kotlinx.document.database.getObjectCollection
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

abstract class AbstractPeriodicCommitStrategyTests(val store: DataStore) : BaseTest(store) {
    companion object {
        val commitInterval = 3.seconds
    }

    abstract fun getUnsavedMemory(): Int

    abstract fun getTotalCacheMemorySize(): Int

    @Test
    @JsName("cache_is_periodically_flushed")
    fun `test if Cache Is Periodically Flushed correctly`() =
        runDatabaseTest {
            assertTrue(
                actual = store.commitStrategy is DataStore.CommitStrategy.Periodic,
                message = "Commit policy should be periodic",
            )
            assertEquals(
                expected = commitInterval,
                actual = (store.commitStrategy as DataStore.CommitStrategy.Periodic).interval,
                message = "Commit interval should be taken from the companion object",
            )

            val collection = db.getObjectCollection<TestUser>("test")

            val initialUnsavedMemorySize = getUnsavedMemory().also(::println)
            TestUser.generateUsers(100).forEach { collection.insert(it) }
            val afterOpUnsavedMemorySize = getUnsavedMemory().also(::println)
            assertTrue("Memory cache size does not increase as expected") {
                afterOpUnsavedMemorySize > initialUnsavedMemorySize
            }

            val start = TimeSource.Monotonic.markNow()

            while (start.elapsedNow() < commitInterval) { // wait for the commit interval
            }

            assertTrue("Memory cache size does not decrease as expected") { afterOpUnsavedMemorySize > getUnsavedMemory().also(::println) }
        }
}
