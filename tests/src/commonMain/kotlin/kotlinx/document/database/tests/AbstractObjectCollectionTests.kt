package kotlinx.document.database.tests

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.document.database.DataStore
import kotlinx.document.database.getObjectCollection
import kotlinx.document.database.tests.TestUser.Companion.Luigi
import kotlinx.document.database.tests.TestUser.Companion.Mario
import kotlinx.document.database.updateWhere
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.time.Duration.Companion.seconds

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

    @Test
    @JsName("Concurrent_modification")
    fun `Concurrent modification`() =
        runDatabaseTest {
            val collection = db.getObjectCollection<TestUser>("test")
            collection.insert(Mario)

            val mutex = Mutex(true)

            launch {
                collection.updateWhere(
                    TestUser::name.name,
                    Mario.name,
                ) {
                    mutex.lock()
                    it
                }
            }

            launch {
                delay(2.seconds)
                mutex.unlock()
            }

            collection.updateWhere(
                TestUser::name.name,
                Mario.name,
            ) {
                it
            }
        }
}
