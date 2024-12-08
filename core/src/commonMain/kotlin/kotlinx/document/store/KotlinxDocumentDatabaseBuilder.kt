@file:Suppress("RedundantSuppression")

package kotlinx.document.store

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

public class KotlinxDocumentDatabaseBuilder {
    public var serializersModule: SerializersModule? = null
    public var store: DataStore? = null

    public fun build(): KotlinxDocumentStore =
        KotlinxDocumentStore(
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
public inline fun KotlinxDocumentStore(block: KotlinxDocumentDatabaseBuilder.() -> Unit): KotlinxDocumentStore =
    KotlinxDocumentDatabaseBuilder().apply(block).build()

@Suppress("FunctionName")
public fun KotlinxDocumentStore(store: DataStore): KotlinxDocumentStore = KotlinxDocumentStore { this.store = store }
