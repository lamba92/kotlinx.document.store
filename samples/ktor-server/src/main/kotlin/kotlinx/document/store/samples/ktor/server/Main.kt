package kotlinx.document.store.samples.ktor.server

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.coroutineScope
import kotlinx.document.store.core.getObjectCollection
import kotlinx.document.store.samples.User
import kotlinx.document.store.stores.leveldb.LevelDBStore

suspend fun main() {
    val dbPath = System.getenv("DB_PATH") ?: error("DB_PATH environment variable not set")
    coroutineScope {
        val db = kotlinx.document.store.core.KotlinxDocumentStore(LevelDBStore.open(dbPath))
        val userCollection = db.getObjectCollection<User>("users")

        userCollection.createIndex("name")

        val server =
            embeddedServer(CIO, port = 8080) {
                UserCRUDServer(userCollection)
            }

        server.start()
    }
}
