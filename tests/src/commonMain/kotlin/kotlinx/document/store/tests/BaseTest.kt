@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package kotlinx.document.store.tests

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.document.store.core.DataStore
import kotlinx.document.store.core.KotlinxDocumentStore
import kotlinx.serialization.modules.SerializersModule
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Base class for creating database-related coroutine-based tests.
 *
 * @constructor Initializes the `BaseTest` with the specified `DataStoreProvider`.
 * @param storeProvider A provider to manage data store instances for testing.
 */
public abstract class BaseTest(private val storeProvider: DataStoreProvider) {
    /**
     * Runs a database-related coroutine-based test. It wraps [runTest] and provides a [KotlinxDocumentStore]
     * instance to the test body. The provided database will be cleaned up before the test starts.
     *
     * @param testName The name of the test database to be created and used during the test.
     * The database will be cleaned up before the test starts.
     * @param context The coroutine context in which the test will be executed
     * (default is [EmptyCoroutineContext]).
     * @param timeout The maximum duration for the test to execute
     * (default is 1 minute).
     * @param serializersModule The [SerializersModule] to be used by the test database.
     * @param testBody A suspendable function that acts as the test body.
     * It receives a [KotlinxDocumentStore] instance representing the test database.
     * @return A [TestResult] representing the result of the test execution.
     */
    protected fun runDatabaseTest(
        testName: String,
        context: CoroutineContext = EmptyCoroutineContext,
        timeout: Duration = 1.minutes,
        serializersModule: SerializersModule? = null,
        testBody: suspend CoroutineScope.(db: KotlinxDocumentStore) -> Unit,
    ): TestResult =
        runTest(context, timeout) {
            storeProvider.deleteDatabase(testName)
            val store = storeProvider.provide(testName)
            val db =
                KotlinxDocumentStore {
                    this.store = store
                    this.serializersModule = serializersModule
                }
            try {
                coroutineScope { testBody(db) }
            } finally {
                withContext(NonCancellable) {
                    db.close()
                }
            }
        }
}

/**
 * An annotation used to mark a test function. The `kotlin.test.Test` annotation is not available
 * because the Kotlin test standard library is not available for Android and Jvm targets at the same time.
 *
 * It typealiases to:
 * - `kotlin.test.Test` on JVM, Native and JS
 * - `org.junit.Test` on Android
 */
@Target(AnnotationTarget.FUNCTION)
public expect annotation class Test()

/**
 * Provides an interface for managing and interacting with [DataStore] instances.
 */
public interface DataStoreProvider {
    /**
     * Provides a [DataStore] instance for a given test name. This instance can be used
     * to perform various persistent operations, such as managing named maps.
     *
     * @param testName The name of the test for which the [DataStore] instance is to be provided.
     * @return A [DataStore] instance associated with the specified test name.
     */
    public fun provide(testName: String): DataStore

    /**
     * Deletes the database associated with the specified test name.
     *
     * @param testName The name of the test whose associated database will be deleted.
     */
    public suspend fun deleteDatabase(testName: String)
}
