package com.github.lamba92.kotlin.db

import java.lang.System.getenv
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class Test {

    val dbPath
        get() = Path(getenv("DB_PATH"))

    val db: KotlinxDb = kotlinxDb {
        filePath = dbPath
    }


    @BeforeEach
    fun deleteDb() = runTest {
        dbPath.deleteIfExists()
    }

    @AfterEach
    fun closeDb() = runTest {
        db.close()
    }

    @Test
    fun `Fails if collection type is primitive`() = runTest {
        val collection = db.getCollection<Long>("test")
        assertFails { collection.insert(1L) }
    }

    @Test
    fun `Fails if collection type is array-like`() = runTest {
        val collection = db.getCollection<List<Pair<Int, Int>>>("test")
        assertFails { collection.insert(listOf(1 to 1)) }
    }

    @Test
    fun insert() = runTest {
        val collection = db.getCollection<TestUser>("test")
        val testUser = TestUser(
            name = "mario",
            age = 20,
            birthDate = 0L,
            addresses = listOf(Address("street", 1))
        )

        collection.insert(testUser)

        assertEquals(
            expected = 1,
            actual = collection.iterateAll().count(),
            message = "Collection should have 1 element"
        )

    }
}

@Serializable
data class TestUser(
    val name: String,
    val age: Int,
    val isAdult: Boolean? = null,
    val birthDate: Long,
    val addresses: List<Address>? = null,
    @SerialName("_id") val id: Long? = null
)

@Serializable
data class Address(
    val street: String,
    val number: Int
)
