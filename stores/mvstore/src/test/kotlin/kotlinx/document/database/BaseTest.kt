package kotlinx.document.database

import kotlin.io.path.deleteIfExists
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class BaseTest(store: kotlinx.document.database.DataStore) {

    val db = KotlinxDocumentDatabase(store)

    @BeforeEach
    fun deleteDb() = runTest {
        dbPath.deleteIfExists()
    }

    @AfterEach
    fun closeDb() = db.close()
}