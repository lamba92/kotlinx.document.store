package com.github.lamba92.kotlin.document.store.core.maps

import com.github.lamba92.kotlin.document.store.core.PersistentMap
import com.github.lamba92.kotlin.document.store.core.SerializableEntry
import com.github.lamba92.kotlin.document.store.core.UpdateResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

private fun String.split() = Json.decodeFromString<Set<Long>>(this)

private fun Set<Long>.join() = Json.encodeToString(this)

/**
 * Converts the given `PersistentMap` with `String` keys and values into an `Index`,
 * which provides additional functionality for working with `JsonElement` keys
 * and `Set<Long>` values.
 *
 * @return An instance of `Index` backed by the current `PersistentMap`.
 */
public fun PersistentMap<String, String>.asIndex(): Index = Index(this)

/**
 * A persistent map implementation that bridges `JsonElement` keys and `Set<Long>` values
 * to an underlying `PersistentMap` which stores `String` keys and their associated values.
 *
 * This class provides an abstraction where `JsonElement` is serialized into a string representation
 * for storage and deserialized back upon retrieval, while `Set<Long>` values are encoded and decoded
 * as JSON strings.
 *
 * @param delegate The underlying `PersistentMap` for storing serialized key-value pairs.
 */
public class Index(
    private val delegate: PersistentMap<String, String>,
) : PersistentMap<JsonElement, Set<Long>> {
    public companion object {
        private val json =
            Json {
                encodeDefaults = false
                prettyPrint = false
            }

        private fun JsonElement.asString(): String =
            when (this) {
                is JsonPrimitive -> toString()
                else -> json.encodeToString(this)
            }
    }

    override suspend fun get(key: JsonElement): Set<Long>? = delegate.get(key.asString())?.split()

    override suspend fun put(
        key: JsonElement,
        value: Set<Long>,
    ): Set<Long>? =
        delegate.put(key.asString(), value.join())
            ?.split()

    override suspend fun remove(key: JsonElement): Set<Long>? = delegate.remove(key.asString())?.split()

    override suspend fun containsKey(key: JsonElement): Boolean = delegate.containsKey(key.asString())

    override suspend fun clear(): Unit = delegate.clear()

    override suspend fun size(): Long = delegate.size()

    override suspend fun isEmpty(): Boolean = delegate.isEmpty()

    override fun entries(): Flow<Map.Entry<JsonElement, Set<Long>>> =
        delegate.entries()
            .map { SerializableEntry(json.decodeFromString(it.key), it.value.split()) }

    override fun close() {
        delegate.close()
    }

    override suspend fun getOrPut(
        key: JsonElement,
        defaultValue: () -> Set<Long>,
    ): Set<Long> =
        delegate.getOrPut(
            key = key.asString(),
            defaultValue = { defaultValue().join() },
        ).split()

    override suspend fun update(
        key: JsonElement,
        default: Set<Long>,
        updater: (Set<Long>) -> Set<Long>,
    ): UpdateResult<Set<Long>> =
        delegate.update(
            key = key.asString(),
            value = default.join(),
            updater = { updater(it.split()).join() },
        ).let { UpdateResult(it.oldValue?.split(), it.newValue.split()) }
}
