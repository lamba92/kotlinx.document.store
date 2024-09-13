package kotlinx.document.database.rocksdb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.document.database.PersistentMap
import kotlinx.document.database.SerializableEntry
import kotlinx.document.database.UpdateResult
import maryk.rocksdb.RocksDB
import maryk.rocksdb.use

public class RocksdbPersistentMap(
    private val delegate: RocksDB,
    private val prefix: String,
) : PersistentMap<String, String> {
    private val mutex = Mutex()

    private fun String.prefixed() = "${prefix}_$this".encodeToByteArray()

    override suspend fun get(key: String): String? =
        withContext(Dispatchers.IO) {
            delegate[key.prefixed()]?.decodeToString()
        }

    override suspend fun put(
        key: String,
        value: String,
    ): String? = mutex.withLock { unsafePut(key, value) }

    private suspend fun unsafePut(
        key: String,
        value: String,
    ): String? =
        withContext(Dispatchers.IO) {
            val prefixed = key.prefixed()
            val previous = delegate[prefixed]?.decodeToString()
            delegate.put(prefixed, value.encodeToByteArray())
            previous
        }

    override suspend fun remove(key: String): String? =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val prefixed = key.prefixed()
                val previous = delegate[prefixed]?.decodeToString()
                delegate.delete(prefixed)
                previous
            }
        }

    override suspend fun containsKey(key: String): Boolean = get(key) != null

    override suspend fun clear(): Unit = delegate.deletePrefix(prefix)

    override suspend fun size(): Long =
        mutex.withLock {
            var count = 0L
            withContext(Dispatchers.IO) {
                delegate.newIterator().use {
                    it.seek(prefix.encodeToByteArray())
                    while (it.isValid() && it.key().decodeToString().startsWith(prefix)) {
                        count++
                        it.next()
                    }
                }
                count
            }
        }

    override suspend fun isEmpty(): Boolean = size() == 0L

    override fun entries(fromIndex: Long): Flow<Map.Entry<String, String>> =
        flow {
            delegate.newIterator().use {
                it.seek("${prefix}_".encodeToByteArray())
                var dropped = 0L
                while (it.isValid()) {
                    // check if we need to skip some entries
                    if (dropped++ < fromIndex) {
                        it.next()
                        continue
                    }

                    val key = it.key().decodeToString()
                    when {
                        key.startsWith(prefix) ->
                            emit(
                                SerializableEntry(
                                    key = key.removePrefix("${prefix}_"),
                                    value = it.value().decodeToString(),
                                ),
                            )

                        else -> break
                    }
                    it.next()
                }
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun getOrPut(
        key: String,
        defaultValue: () -> String,
    ): String =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                delegate[key.prefixed()]?.decodeToString() ?: defaultValue().also { unsafePut(key, it) }
            }
        }

    override suspend fun update(
        key: String,
        value: String,
        updater: (String) -> String,
    ): UpdateResult<String> =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val previous = delegate[key.prefixed()]?.decodeToString()
                val newValue = previous?.let(updater) ?: value
                unsafePut(key, newValue)
                UpdateResult(previous, newValue)
            }
        }
}
