@file:Suppress("RedundantSuppression")

package com.github.lamba92.kotlin.document.store.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/**
 * Builder class for constructing instances of `KotlinxDocumentStore`.
 */
public class KotlinxDocumentDatabaseBuilder {
    /**
     * Optional custom serializers module for JSON operations.
     */
    public var serializersModule: SerializersModule? = null

    /**
     * The data store instance which manages document persistence operations.
     */
    public var store: DataStore? = null

    /**
     * Builds and returns a `KotlinxDocumentStore` instance using the defined configurations.
     */
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
