package kotlinx.document.database.samples.ktor.server

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlinx.coroutines.coroutineScope
import kotlinx.document.database.KotlinxDocumentDatabase
import kotlinx.document.database.getObjectCollection
import kotlinx.document.database.rocksdb.RocksdbDataStore

suspend fun main(): Unit = coroutineScope {
    val path = System.getenv("DB_PATH")
        ?: Path("./server.db")
            .createDirectories()
            .absolutePathString()

    val db = KotlinxDocumentDatabase(RocksdbDataStore.open(path))
    val userCollection = db.getObjectCollection<User>("users")
    userCollection.createIndex("name")

    val server = embeddedServer(CIO, port = 8080) {
        UserCRUDServer(userCollection)
    }

    server.start()
}
