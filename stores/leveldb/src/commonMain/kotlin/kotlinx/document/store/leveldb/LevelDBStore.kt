package kotlinx.document.store.leveldb

import com.github.lamba92.leveldb.LevelDB
import com.github.lamba92.leveldb.LevelDBOptions
import com.github.lamba92.leveldb.batch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.document.store.AbstractDataStore
import kotlinx.document.store.PersistentMap
import kotlinx.io.files.Path

public class LevelDBStore(private val delegate: LevelDB) : AbstractDataStore() {
    public companion object {
        public fun open(
            path: Path,
            options: LevelDBOptions = LevelDBOptions.DEFAULT,
        ): LevelDBStore = open(path.toString(), options)

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
