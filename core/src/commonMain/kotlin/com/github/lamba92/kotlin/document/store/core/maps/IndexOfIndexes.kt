package com.github.lamba92.kotlin.document.store.core.maps

import com.github.lamba92.kotlin.document.store.core.PersistentMap
import com.github.lamba92.kotlin.document.store.core.SerializableEntry
import com.github.lamba92.kotlin.document.store.core.UpdateResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Converts a `PersistentMap<String, String>` into an `IndexOfIndexes` instance.
 *
 * This function wraps the given `PersistentMap` into an `IndexOfIndexes`, allowing
 * the caller to interact with the underlying data using a transformed interface.
 *
 * @return An `IndexOfIndexes` instance wrapping the original `PersistentMap`.
 */
public fun PersistentMap<String, String>.asIndexOfIndexes(): IndexOfIndexes = IndexOfIndexes(this)

private fun String.split() = Json.decodeFromString<List<String>>(this)

private fun List<String>.join() = Json.encodeToString(this)

/**
 * A persistent map implementation that transforms and stores values as serialized lists of strings.
 *
 * `IndexOfIndexes` acts as a wrapper around a `PersistentMap` with `String` keys and `String` values.
 * This class provides an interface to work with the underlying map where the values are serialized lists
 * of strings. It handles encoding and decoding of the list data format, allowing users to interact with
 * the data as `List<String>` rather than serialized strings.
 *
 * @property delegate The underlying `PersistentMap` instance used for storage and retrieval operations.
 */
public class IndexOfIndexes(private val delegate: PersistentMap<String, String>) : PersistentMap<String, List<String>> {
    override suspend fun clear(): Unit = delegate.clear()

    override suspend fun size(): Long = delegate.size()

    override suspend fun isEmpty(): Boolean = delegate.isEmpty()

    override fun close() {
        delegate.close()
    }

    override suspend fun remove(key: String): List<String>? = delegate.remove(key)?.split()

    override suspend fun containsKey(key: String): Boolean = delegate.containsKey(key)

    override suspend fun get(key: String): List<String>? = delegate.get(key)?.split()

    override suspend fun put(
        key: String,
        value: List<String>,
    ): List<String>? = delegate.put(key, value.join())?.split()

    override suspend fun update(
        key: String,
        value: List<String>,
        updater: (List<String>) -> List<String>,
    ): UpdateResult<List<String>> =
        delegate.update(
            key = key,
            value = value.join(),
            updater = { updater(it.split()).join() },
        ).let { UpdateResult(it.oldValue?.split(), it.newValue.split()) }

    override suspend fun getOrPut(
        key: String,
        defaultValue: () -> List<String>,
    ): List<String> =
        delegate.getOrPut(
            key = key,
            defaultValue = { defaultValue().join() },
        ).split()

    override fun entries(): Flow<Map.Entry<String, List<String>>> =
        delegate.entries()
            .map { SerializableEntry(it.key, it.value.split()) }
}
