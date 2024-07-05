package kotlinx.document.database.tests

import kotlinx.document.database.DataStore
import kotlinx.document.database.getObjectCollection
import kotlinx.document.database.tests.TestUser.Companion.Luigi
import kotlinx.document.database.tests.TestUser.Companion.Mario
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertFails

abstract class AbstractObjectCollectionTests(store: DataStore) : BaseTest(store) {
    @Test
    @JsName("gets_all_collection_names")
    fun `Fails if collection type is not serializable`() =
        runDatabaseTest {
            assertFails {
                val collection = db.getObjectCollection<() -> Unit>("test")
                collection.insert { }
            }
        }

    @Test
    @JsName("fails_if_collection_type_is_primitive")
    fun `Fails if collection type is primitive`() =
        runDatabaseTest {
            val collection = db.getObjectCollection<Long>("test")
            assertFails { collection.insert(1L) }
        }

    @Test
    @JsName("fails_if_collection_type_is_array_like")
    fun `Fails if collection type is array-like`() =
        runDatabaseTest {
            val collection = db.getObjectCollection<List<TestUser>>("test")
            assertFails { collection.insert(listOf(Mario, Luigi)) }
        }
}
