@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package kotlinx.document.store.tests

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.document.store.DataStore
import kotlinx.document.store.KotlinxDocumentStore
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

public abstract class BaseTest(private val storeProvider: DataStoreProvider) : DatabaseDeleter {
    protected fun runDatabaseTest(
        testName: String,
        context: CoroutineContext = EmptyCoroutineContext,
        timeout: Duration = 1.minutes,
        testBody: suspend CoroutineScope.(db: KotlinxDocumentStore) -> Unit,
    ): TestResult =
        runTest(context, timeout) {
            deleteDatabase(testName)
            val store = storeProvider.provide(testName)
            val db = KotlinxDocumentStore(store)
            try {
                coroutineScope { testBody(db) }
            } finally {
                withContext(NonCancellable) {
                    db.close()
                }
            }
        }
}

@Target(AnnotationTarget.FUNCTION)
public expect annotation class Test()

public fun interface DataStoreProvider {
    public fun provide(testName: String): DataStore
}

public fun interface DatabaseDeleter {
    public suspend fun deleteDatabase(testName: String)
}
