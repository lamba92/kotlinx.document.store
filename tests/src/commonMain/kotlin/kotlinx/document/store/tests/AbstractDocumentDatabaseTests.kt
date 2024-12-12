@file:Suppress("FunctionName")

package kotlinx.document.store.tests

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestResult
import kotlinx.document.store.getObjectCollection
import kotlin.test.assertEquals

public abstract class AbstractDocumentDatabaseTests(store: DataStoreProvider) : BaseTest(store) {
    public companion object {
        public const val TEST_NAME: String = "gets_all_collection_names"
    }

    @Test
    public fun getsAllCollectionNames(): TestResult =
        runDatabaseTest(TEST_NAME) { db ->
            db.getObjectCollection<TestUser>("test")
            db.getObjectCollection<TestUser>("test2")

            assertEquals(
                expected = listOf("test", "test2"),
                actual = db.getAllCollectionNames().toList(),
                message = "Database should have 2 collections",
            )
        }
}
