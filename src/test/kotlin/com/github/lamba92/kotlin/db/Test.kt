package com.github.lamba92.kotlin.db

import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable

class Test {

    val db = kotlinxDb {
        filePath = Path("test.db")
    }

    @Test
    fun test() = runTest {
        val collection = db.getCollection<TestUser>("test")

        collection.insertOne(TestUser("mario", 20))

        assertEquals(
            expected = 1,
            actual = collection.asFlow().count(),
            message = "Collection should have 1 element"
        )

        val mario = collection.asFlow()
            .filter { it.name == "mario" }
            .first()
    }
}

@Serializable
data class TestUser(
    val name: String,
    val age: Int,
//    val address: Address? = null
)

@Serializable
data class Address(
    val street: String,
    val number: Int
)