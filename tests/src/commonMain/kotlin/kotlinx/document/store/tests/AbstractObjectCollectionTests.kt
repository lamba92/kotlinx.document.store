@file:Suppress("FunctionName")

package kotlinx.document.store.tests

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.TestResult
import kotlinx.document.store.getObjectCollection
import kotlinx.document.store.tests.TestUser.Companion.Luigi
import kotlinx.document.store.tests.TestUser.Companion.Mario
import kotlinx.document.store.updateWhere
import kotlin.test.assertFails
import kotlin.time.Duration.Companion.seconds

public abstract class AbstractObjectCollectionTests(store: DataStoreProvider) : BaseTest(store) {
    public companion object {
        public const val TEST_NAME_1: String = "gets_all_collection_names"
        public const val TEST_NAME_2: String = "fails_if_collection_type_is_not_serializable"
        public const val TEST_NAME_3: String = "fails_if_collection_type_is_primitive"
        public const val TEST_NAME_4: String = "fails_if_collection_type_is_array_like"
        public const val TEST_NAME_5: String = "concurrent_modification"
    }

    @Test
    public fun failsIfCollectionTypeIsNotSerializable(): TestResult =
        runDatabaseTest(TEST_NAME_1) { db ->
            assertFails {
                val collection = db.getObjectCollection<() -> Unit>("test")
                collection.insert { }
            }
        }

    @Test
    public fun failsIfCollectionTypeIsPrimitive(): TestResult =
        runDatabaseTest(TEST_NAME_2) { db ->
            val collection = db.getObjectCollection<Long>("test")
            assertFails { collection.insert(1L) }
        }

    @Test
    public fun failsIfCollectionTypeIsArrayLike(): TestResult =
        runDatabaseTest(TEST_NAME_3) { db ->
            val collection = db.getObjectCollection<List<TestUser>>("test")
            assertFails { collection.insert(listOf(Mario, Luigi)) }
        }

    @Test
    public fun concurrentModification(): TestResult =
        runDatabaseTest(TEST_NAME_4) { db ->
            val collection = db.getObjectCollection<TestUser>("test")
            collection.insert(Mario)

            val mutex = Mutex(true)

            launch {
                collection.updateWhere(
                    fieldSelector = TestUser::name.name,
                    fieldValue = Mario.name,
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
                fieldSelector = TestUser::name.name,
                fieldValue = Mario.name,
            ) {
                it
            }
        }
}
