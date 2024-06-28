package com.github.lamba92.kotlin.db

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class DocumentDatabaseTests : MVDataStoreTest() {
    @Test
    fun `gets all collection names`() = runTest {
        db.getObjectCollection<TestUser>("test")
        db.getObjectCollection<TestUser>("test2")

        assertEquals(
            expected = listOf("test", "test2"),
            actual = db.getAllCollectionNames().toList(),
            message = "Database should have 2 collections"
        )

    }
}