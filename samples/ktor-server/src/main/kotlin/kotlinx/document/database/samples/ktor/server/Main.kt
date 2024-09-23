package kotlinx.document.database.samples.ktor.server

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.coroutineScope
import kotlinx.document.database.DataStore
import kotlinx.document.database.KotlinxDocumentDatabase
import kotlinx.document.database.getObjectCollection
import kotlinx.document.database.rocksdb.RocksdbDataStore
import kotlinx.document.database.samples.User
import kotlin.io.path.Path
import kotlin.io.path.createDirectories


suspend fun main(): Unit = coroutineScope {
    val path = Path(
        System.getenv("DB_PATH") ?: "./server.db"
    ).createDirectories()


    val db = KotlinxDocumentDatabase(
        RocksdbDataStore.open(
            path = path,
            commitStrategy = DataStore.CommitStrategy.OnChange
        )
    )
    val userCollection = db.getObjectCollection<User>("users")
    userCollection.createIndex("name")

    val server = embeddedServer(CIO, port = 8080) {
        UserCRUDServer(userCollection)
    }

    server.start()
}
