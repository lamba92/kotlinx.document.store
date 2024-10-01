package kotlinx.document.database.tests

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.document.database.DataStore
import kotlinx.document.database.KotlinxDocumentDatabase
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class BaseTest(store: DataStore) : DatabaseDeleter {
    val db = KotlinxDocumentDatabase(store)

    protected fun runDatabaseTest(
        context: CoroutineContext = EmptyCoroutineContext,
        timeout: Duration = 60.seconds,
        testBody: suspend CoroutineScope.() -> Unit,
    ) = runTest(context, timeout) {
        try {
            coroutineScope(testBody)
        } finally {
            db.close()
            deleteDatabase()
        }
    }
}

interface DatabaseDeleter {
    suspend fun deleteDatabase()
}
