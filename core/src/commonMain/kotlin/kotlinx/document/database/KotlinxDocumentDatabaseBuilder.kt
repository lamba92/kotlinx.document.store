package kotlinx.document.database

import kotlinx.serialization.json.Json

class KotlinxDocumentDatabaseBuilder {
    var json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    var store: kotlinx.document.database.DataStore? = null

    fun build(): KotlinxDocumentDatabase = KotlinxDocumentDatabase(
        store = store ?: error("Store must be provided"),
        json = Json(json) {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    )
}

inline fun KotlinxDocumentDatabase(block: KotlinxDocumentDatabaseBuilder.() -> Unit) =
    KotlinxDocumentDatabaseBuilder().apply(block).build()

fun KotlinxDocumentDatabase(store: kotlinx.document.database.DataStore) = KotlinxDocumentDatabase { this.store = store}
