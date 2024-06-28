package com.github.lamba92.kotlin.db

import com.github.lamba92.kotlin.db.TestUser.Companion.Luigi
import com.github.lamba92.kotlin.db.TestUser.Companion.Mario
import kotlin.test.Test
import kotlin.test.assertFails
import kotlinx.coroutines.test.runTest

class ObjectCollectionTests : MVDataStoreTest() {

    @Test
    fun `Fails if collection type is not serializable`() = runTest {
        val collection = db.getObjectCollection<() -> Unit>("test")
        assertFails { collection.insert { } }
    }

    @Test
    fun `Fails if collection type is primitive`() = runTest {
        val collection = db.getObjectCollection<Long>("test")
        assertFails { collection.insert(1L) }
    }

    @Test
    fun `Fails if collection type is array-like`() = runTest {
        val collection = db.getObjectCollection<List<TestUser>>("test")
        assertFails { collection.insert(listOf(Mario, Luigi)) }
    }
}

