package com.github.lamba92.kotlin.db

import java.nio.file.Path
import kotlinx.serialization.json.Json
import org.h2.mvstore.MVStore

class KotlinxDbBuilder {
    var filePath: Path? = null

    var json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    var cacheSize = 1.megabytes
    var autoCommitBufferSize = 1.megabytes

    fun build(): KotlinxDb {
        val path = filePath ?: error("File path must be provided")
        val store = MVStore.Builder()
            .fileName(path.toString())
            .autoCommitBufferSize(autoCommitBufferSize.megabytes.toInt())
            .cacheSize(cacheSize.megabytes.toInt())
            .open()
        return KotlinxDb(
            store = store,
            json = Json(json) { ignoreUnknownKeys = true }
        )
    }
}

fun kotlinxDb(block: KotlinxDbBuilder.() -> Unit) = KotlinxDbBuilder().apply(block).build()
