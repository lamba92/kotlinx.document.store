package com.github.lamba92.kotlin.document.store.stores.leveldb

import com.github.lamba92.kotlin.document.store.core.PersistentMap
import com.github.lamba92.kotlin.document.store.core.SerializableEntry
import com.github.lamba92.kotlin.document.store.core.UpdateResult
import com.github.lamba92.leveldb.LevelDB
import com.github.lamba92.leveldb.batch
import com.github.lamba92.leveldb.resolve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A [PersistentMap] implementation backed by LevelDB.
 *
 * `LevelDBPersistentMap` provides a disk-based key-value store where each entry
 * is prefixed with a unique namespace (prefix) to distinguish multiple maps stored
 * in the same LevelDB instance.
 */
public class LevelDBPersistentMap(
    private val delegate: LevelDB,
    private val prefix: String,
    private val mutex: Mutex,
) : PersistentMap<String, String> {
    private fun String.prefixed() = "$prefix.$this"

    override suspend fun get(key: String): String? =
        withContext(Dispatchers.IO) {
            delegate.get(key.prefixed())
        }

    override suspend fun put(
        key: String,
        value: String,
    ): String? =
        mutex.withLock {
            val previousValue = delegate.get(key.prefixed())
            val previousSize = delegate.get("sizes.$prefix")?.toLong()
            delegate.batch {
                put(key.prefixed(), value)
                val nextSize =
                    when (previousSize) {
                        null -> "1"
                        else -> (previousSize + 1).toString()
                    }
                put("sizes.$prefix", nextSize)
            }
            previousValue
        }

    override suspend fun remove(key: String): String? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val prefixed = key.prefixed()
                val previous = delegate.get(prefixed)
                delegate.delete(prefixed)
                delegate.get("sizes.$prefix")
                    ?.toLong()
                    ?.let { delegate.put("sizes.$prefix", (it - 1).toString()) }
                previous
            }
        }

    override suspend fun containsKey(key: String): Boolean = get(key) != null

    override suspend fun clear(): Unit = delegate.deletePrefix(prefix)

    override suspend fun size(): Long =
        withContext(Dispatchers.IO) {
            delegate.get("sizes.$prefix")?.toLong() ?: 0L
        }

    override suspend fun isEmpty(): Boolean = size() == 0L

    override fun entries(): Flow<Map.Entry<String, String>> =
        flow {
            val sequence = delegate.scan("$prefix.")
            try {
                sequence.map { it.resolve() }
                    .takeWhile { (key, _) -> key.startsWith("$prefix.") }
                    .forEach { emit(SerializableEntry(it.key.removePrefix("$prefix."), it.value)) }
            } finally {
                withContext(NonCancellable) {
                    sequence.close()
                }
            }
        }

    override suspend fun getOrPut(
        key: String,
        defaultValue: () -> String,
    ): String =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                delegate.get(key.prefixed())
                    ?: defaultValue().also { delegate.put(key.prefixed(), it) }
            }
        }

    override suspend fun update(
        key: String,
        value: String,
        updater: (String) -> String,
    ): UpdateResult<String> =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val previous = delegate.get(key.prefixed())
                val newValue = previous?.let(updater) ?: value
                delegate.put(key.prefixed(), newValue)
                UpdateResult(previous, newValue)
            }
        }
}
