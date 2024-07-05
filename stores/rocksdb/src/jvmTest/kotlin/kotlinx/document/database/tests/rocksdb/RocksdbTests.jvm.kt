package kotlinx.document.database.tests.rocksdb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.path.deleteRecursively
import kotlin.io.path.Path as JavaPath
import kotlinx.io.files.Path as KotlinxPath

actual suspend fun KotlinxPath.deleteRecursively() =
    withContext(Dispatchers.IO) {
        JavaPath(this@deleteRecursively.toString()).deleteRecursively()
    }

actual val DB_PATH: String by System.getenv()
