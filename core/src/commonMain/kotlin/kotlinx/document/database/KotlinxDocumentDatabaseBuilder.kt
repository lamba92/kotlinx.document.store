@file:Suppress("RedundantSuppression")

package kotlinx.document.database

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

public class KotlinxDocumentDatabaseBuilder {
    public var serializersModule: SerializersModule? = null
    public var store: DataStore? = null

    public fun build(): KotlinxDocumentDatabase =
        KotlinxDocumentDatabase(
            store = store ?: error("Store must be provided"),
            json =
                Json {
                    this@KotlinxDocumentDatabaseBuilder.serializersModule?.let { serializersModule = it }
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
