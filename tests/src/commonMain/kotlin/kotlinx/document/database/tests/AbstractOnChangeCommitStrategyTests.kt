package kotlinx.document.database.tests

import kotlinx.document.database.DataStore
import kotlinx.document.database.getObjectCollection
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertTrue

abstract class AbstractOnChangeCommitStrategyTests(val store: DataStore) : BaseTest(store) {
    abstract fun getStoreFileSize(): Long

    @Test
    @JsName("cache_is_periodically_flushed")
    fun `test if insert are directly committed on disk`() =
        runDatabaseTest {
            assertTrue(
                actual = store.commitStrategy is DataStore.CommitStrategy.OnChange,
                message = "Commit policy should be OnChange",
            )

            val collection = db.getObjectCollection<TestUser>("test")

            var fileSize = getStoreFileSize()

            TestUser.generateUsers(100).forEach {
                collection.insert(it)
                val actualFileSize = getStoreFileSize()
                assertTrue(actualFileSize > fileSize)
                fileSize = actualFileSize
            }
        }
}
