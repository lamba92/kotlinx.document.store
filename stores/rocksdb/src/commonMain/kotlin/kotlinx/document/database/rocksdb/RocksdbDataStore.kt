package kotlinx.document.database.rocksdb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.document.database.DataStore
import kotlinx.document.database.PersistentMap
import maryk.rocksdb.Options
import maryk.rocksdb.RocksDB
import maryk.rocksdb.openRocksDB
import maryk.rocksdb.use
import org.rocksdb.WriteOptions
import java.nio.file.Path
import kotlin.io.path.absolutePathString

public class RocksdbDataStore(
    internal val delegate: RocksDB,
    override val commitStrategy: DataStore.CommitStrategy,
) : DataStore {
    private val scope by lazy { CoroutineScope(SupervisorJob()) }

    init {
        if (commitStrategy is DataStore.CommitStrategy.Periodic) {
            scope.launch {
                while (true) {
                    delay(commitStrategy.interval)
                    commit()
                }
            }
        }
    }

    public companion object {
        public fun open(
            path: Path,
            commitStrategy: DataStore.CommitStrategy,
        ): RocksdbDataStore = open(Options().setCreateIfMissing(true), path, commitStrategy)

        public fun open(
            options: Options,
            path: Path,
            commitStrategy: DataStore.CommitStrategy,
        ): RocksdbDataStore {
            val datastore =
                RocksdbDataStore(
                    delegate = openRocksDB(options = options, path = path.absolutePathString()),
                    commitStrategy = commitStrategy,
                )
            WriteOptions().apply {
                when (commitStrategy) {
                    is DataStore.CommitStrategy.OnChange -> {
                        setDisableWAL(false) // Enable Write Ahead Logging (auto commit)
                        setSync(true)
                    }

                    is DataStore.CommitStrategy.Periodic -> {
                        setDisableWAL(true) // Disable Write Ahead Logging (manual commit)
                        setSync(false)
                    }
                }
            }
            return datastore
        }
    }

    override suspend fun getMap(name: String): PersistentMap<String, String> = RocksdbPersistentMap(delegate, name)

    override suspend fun deleteMap(name: String) {
        delegate.deletePrefix(name)
    }

    override suspend fun commit() {
        withContext(Dispatchers.IO) { delegate.flushWal(true) }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            delegate.flushWal(true)
            delegate.close()
        }
        if (commitStrategy is DataStore.CommitStrategy.Periodic) {
            scope.cancel()
        }
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
