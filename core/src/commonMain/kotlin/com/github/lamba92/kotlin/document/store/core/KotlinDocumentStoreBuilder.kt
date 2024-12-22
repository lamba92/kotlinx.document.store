@file:Suppress("RedundantSuppression")

package com.github.lamba92.kotlin.document.store.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

@Deprecated("Old name, use new one", ReplaceWith("KotlinDocumentDatabaseBuilder"))
public typealias KotlinxDocumentDatabaseBuilder = KotlinDocumentDatabaseBuilder

@Deprecated("Old name, use new one", ReplaceWith("KotlinDocumentStore"))
public typealias KotlinxDocumentStore = KotlinDocumentStore

/**
 * Builder class for constructing instances of `KotlinxDocumentStore`.
 */
public class KotlinDocumentDatabaseBuilder {
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
    public fun build(): KotlinDocumentStore =
        KotlinDocumentStore(
            store = store ?: error("Store must be provided"),
            json =
                Json {
                    this@KotlinDocumentDatabaseBuilder.serializersModule?.let { serializersModule = it }
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                },
        )
}

@Suppress("FunctionName")
public inline fun KotlinDocumentStore(block: KotlinDocumentDatabaseBuilder.() -> Unit): KotlinDocumentStore =
    KotlinDocumentDatabaseBuilder().apply(block).build()

@Suppress("FunctionName")
public fun KotlinDocumentStore(store: DataStore): KotlinDocumentStore = KotlinDocumentStore { this.store = store }
