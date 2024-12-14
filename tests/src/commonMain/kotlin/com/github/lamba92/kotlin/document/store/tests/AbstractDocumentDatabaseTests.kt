@file:Suppress("FunctionName")

package com.github.lamba92.kotlin.document.store.tests

import com.github.lamba92.kotlin.document.store.core.getObjectCollection
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestResult
import kotlin.test.assertEquals

/**
 * Abstract base class for testing document database-level operations within a document store.
 *
 * It extends [BaseTest] to validate behaviors such as retrieving the names of all collections
 * in the database. These tests ensure that the document database behaves correctly when working
 * with higher-level operations that involve multiple collections.
 *
 * This class is designed to be extended for specific implementations of `DataStoreProvider`
 * depending on the targeted Kotlin platform.
 */
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
