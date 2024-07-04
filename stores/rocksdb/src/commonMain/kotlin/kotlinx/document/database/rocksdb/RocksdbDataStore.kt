package kotlinx.document.database.rocksdb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.document.database.DataStore
import kotlinx.document.database.PersistentMap
import maryk.rocksdb.Options
import maryk.rocksdb.RocksDB
import maryk.rocksdb.openRocksDB
import maryk.rocksdb.use

public class RocksdbDataStore(private val delegate: RocksDB) : DataStore {

    public companion object {

        public fun open(path: String): RocksdbDataStore =
            RocksdbDataStore(openRocksDB(path))

        public fun open(options: Options, path: String): RocksdbDataStore =
            RocksdbDataStore(openRocksDB(options, path))

    }

    override suspend fun getMap(name: String): PersistentMap<String, String> =
        RocksdbPersistentMap(delegate, name)

    override suspend fun deleteMap(name: String) {
        delegate.deletePrefix(name)
    }

    override fun close() {
        delegate.flushWal(true)
        delegate.close()
    }
}

public suspend fun RocksDB.deletePrefix(prefix: String): Unit =
    withContext(Dispatchers.IO) {
        newIterator().use { iterator ->
            iterator.seek(prefix.encodeToByteArray())
            while (iterator.isValid()) {
                val key = iterator.key()
                val keyString = key.decodeToString()
                when {
                    keyString.startsWith(prefix) -> delete(key)
                    else -> break
                }
                iterator.next()
            }
        }
    }