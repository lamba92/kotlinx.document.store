package kotlinx.document.store.stores.leveldb

import com.github.lamba92.leveldb.LevelDB
import com.github.lamba92.leveldb.LevelDBOptions
import com.github.lamba92.leveldb.batch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.document.store.core.AbstractDataStore
import kotlinx.document.store.core.DataStore
import kotlinx.document.store.core.PersistentMap

/**
 * A [LevelDB] implementation of the [DataStore] for persistent storage.
 *
 * `LevelDBStore` uses LevelDB to provide a disk-backed, reliable, and high-performance
 * key-value storage system for managing named maps. It supports creating, retrieving,
 * and deleting persistent maps, with each map uniquely identified by a prefix.
 *
 * This implementation ensures thread-safe access to data operations using synchronization
 * mechanisms while leveraging LevelDB's batch operations for efficient map management.
 *
 * It is particularly suited for use cases that require fast sequential reads/writes
 * and efficient use of disk storage.
 */
public class LevelDBStore(private val delegate: LevelDB) : AbstractDataStore() {
    public companion object {
        /**
         * Opens a new [LevelDBStore] instance at the specified path with the given options.
         *
         * Intermediate directories are **NOT** created if they do not exist. The database will be created
         * at the specified path as a directory.
         *
         * @param path The file system path where the LevelDB database will be created or accessed.
         *             Can be provided as a `String` or a `Path`.
         * @param options Optional [LevelDBOptions] to configure LevelDB (default is [LevelDBOptions.DEFAULT]).
         * @return A new `LevelDBStore` instance backed by the LevelDB database at the specified path.
         */
        public fun open(
            path: String,
            options: LevelDBOptions = LevelDBOptions.DEFAULT,
        ): LevelDBStore = LevelDBStore(LevelDB(path, options))
    }

    override suspend fun getMap(name: String): PersistentMap<String, String> =
        withStoreLock {
            LevelDBPersistentMap(
                delegate = delegate,
                prefix = name,
                mutex = getMutex(name),
            )
        }

    override suspend fun deleteMap(name: String): Unit =
        withStoreLock {
            lockAndRemoveMutex(name) { delegate.deletePrefix(name) }
        }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            delegate.close()
        }
    }
}

internal suspend fun LevelDB.deletePrefix(prefix: String) =
    withContext(Dispatchers.IO) {
        batch {
            val sequence = scan(prefix)
            sequence
                .map { it.key.value }
                .takeWhile { it.startsWith(prefix) }
                .forEach { delete(it) }
            delete("sizes.$prefix")
            sequence.close()
        }
    }
