package kotlinx.document.database

import kotlinx.serialization.json.Json

public class KotlinxDocumentDatabaseBuilder {
    public var json: Json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

    public var store: DataStore? = null

    public fun build(): KotlinxDocumentDatabase =
        KotlinxDocumentDatabase(
            store = store ?: error("Store must be provided"),
            json =
                Json(json) {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                },
        )
}

@Suppress("FunctionName")
public inline fun KotlinxDocumentDatabase(block: KotlinxDocumentDatabaseBuilder.() -> Unit): KotlinxDocumentDatabase =
    KotlinxDocumentDatabaseBuilder().apply(block).build()

@Suppress("FunctionName")
public fun KotlinxDocumentDatabase(store: DataStore): KotlinxDocumentDatabase = KotlinxDocumentDatabase { this.store = store }
