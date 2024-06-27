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

        collection.insert(TestUser("mario", 20, birthDate = 0L))

        assertEquals(
            expected = 1,
            actual = collection.asFlow().count(),
            message = "Collection should have 1 element"
        )

        val adultMarios = collection.find("name", "mario")
            .filter { it.isAdult == true }

    }
}

@Serializable
data class TestUser(
    val name: String,
    val age: Int,
    val isAdult: Boolean? = null,
    val birthDate: Long,
    val addresses: List<Address>? = null
)

@Serializable
data class Address(
    val street: String,
    val number: Int
)
