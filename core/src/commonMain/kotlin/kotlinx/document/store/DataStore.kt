package kotlinx.document.store

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public interface DataStore : SuspendCloseable {
    public suspend fun getMap(name: String): PersistentMap<String, String>

    public suspend fun deleteMap(name: String)
}

public abstract class AbstractDataStore : DataStore {
    public class MutexLockedScope(private val mutexMap: MutableMap<String, Mutex>) {
        public fun getMutex(name: String): Mutex = mutexMap.getOrPut(name) { Mutex() }

        public suspend fun <T> lockAndRemoveMutex(
            mutexName: String,
            block: suspend () -> T,
        ): T {
            val mutex = getMutex(mutexName)
            return mutex.withLock {
                try {
                    block()
                } finally {
                    mutexMap.remove(mutexName)
                }
            }
        }
    }

    private val mutexMap: MutableMap<String, Mutex> = mutableMapOf()
    private val mutexMapLock: Mutex = Mutex()

    protected suspend fun <T> withStoreLock(block: suspend MutexLockedScope.() -> T): T =
        mutexMapLock.withLock { block(MutexLockedScope(mutexMap)) }

    override suspend fun close() {
    }
}

public fun interface SuspendCloseable {
    public suspend fun close()
}

public suspend fun <T, R : SuspendCloseable> R.use(block: suspend (R) -> T): T {
    return try {
        block(this)
    } finally {
        close()
    }
}
