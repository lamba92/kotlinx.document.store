package com.github.lamba92.kotlin.document.store.samples.ktor.server

import com.github.lamba92.kotlin.document.store.core.KotlinDocumentStore
import com.github.lamba92.kotlin.document.store.core.getObjectCollection
import com.github.lamba92.kotlin.document.store.samples.User
import com.github.lamba92.kotlin.document.store.stores.leveldb.LevelDBStore
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.coroutineScope

suspend fun main() {
    val dbPath = System.getenv("DB_PATH") ?: error("DB_PATH environment variable not set")
    coroutineScope {
        val db = KotlinDocumentStore(LevelDBStore.open(dbPath))
        val userCollection = db.getObjectCollection<User>("users")

        userCollection.createIndex("name")

        val server =
            embeddedServer(CIO, port = 8080) {
                UserCRUDServer(userCollection)
            }

        server.start()
    }
}
