package kotlin.kotlinx.document.database.rocksdb

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.document.database.DataStore
import kotlinx.document.database.PersistentMap
import kotlinx.document.database.UpdateResult
import kotlinx.io.files.Path
import maryk.rocksdb.RocksDB
import maryk.rocksdb.openRocksDB

class RocksdbDataStore(val databasePath: Path) : DataStore {

    override suspend fun getMap(name: String): PersistentMap<String, String> =
        RocksdbPersistentMap(openRocksDB(databasePath.resolve("$name.db").name))


    override suspend fun deleteMap(name: String) {
        databasePath.resolve("$name.db").deleteIfExists()
    }

}

expect fun Path.deleteIfExists(): Boolean

fun Path.resolve(name: String) = Path(this, name)

class RocksdbPersistentMap(private val delegate: RocksDB) : PersistentMap<String, String> {

    private val mutex = Mutex()

    override suspend fun get(key: String): String? =
        delegate[key.encodeToByteArray()]?.decodeToString()

    override suspend fun put(key: String, value: String): String? = mutex.withLock {
        val previous = get(key)
        delegate.put(key.encodeToByteArray(), value.encodeToByteArray())
        previous
    }

    override suspend fun remove(key: String): String? = mutex.withLock {
        val previous = get(key)
        delegate.delete(key.encodeToByteArray())
        previous
    }

    override suspend fun containsKey(key: String): Boolean =
        delegate[key.encodeToByteArray()] != null

    override suspend fun clear() = mutex.withLock {
//        delegate.
    }

    override suspend fun size(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun entries(): Flow<Map.Entry<String, String>> {
        TODO("Not yet implemented")
    }

    override suspend fun getOrPut(key: String, defaultValue: () -> String): String {
        TODO("Not yet implemented")
    }

    override suspend fun update(key: String, value: String, updater: (String) -> String): UpdateResult<String> {
        TODO("Not yet implemented")
    }

}