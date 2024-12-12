package kotlinx.document.store

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Abstract implementation of the [DataStore] interface that provides common utilities for
 * managing thread-safe operations using mutex locks for named maps. This class simplifies
 * the creation and management of persistent data storage mechanisms by handling mutex-based
 * synchronization.
 *
 * There are two types of mutex locks used in this class:
 * - A global mutex lock that is used to synchronize access to the map of mutexes.
 * - A per-map mutex lock that is used to synchronize access to a specific named map.
 *
 * To use a per-map mutex, the global mutex lock is acquired first to ensure that the map of
 * mutexes is accessed in a thread-safe manner.
 */
public abstract class AbstractDataStore : DataStore {
    /**
     * A utility class for managing scoped mutex locks identified by unique names. This class is designed
     * to provide thread-safe locking for operations associated with specific names, and it ensures that
     * locks are appropriately cleaned up after use.
     *
     * Each mutex lock is identified by a unique string name. If a mutex lock for a given name does not
     * exist, it is created automatically when accessed using the `getMutex` method.
     */
    public class MutexLockedScope(private val mutexMap: MutableMap<String, Mutex>) {
        /**
         * Retrieves a mutex lock associated with the given name. If no mutex exists for the
         * specified name, a new one is created and stored in the map.
         *
         * @param name The unique identifier for the mutex lock.
         * @return The mutex lock associated with the given name.
         */
        public fun getMutex(name: String): Mutex = mutexMap.getOrPut(name) { Mutex() }

        /**
         * Acquires a lock on the mutex associated with the given name, executes the provided block of code,
         * and then removes the mutex from the map, ensuring cleanup after execution.
         *
         * This method is designed to safely manage concurrent access and to ensure the associated
         * mutex is removed after usage, regardless of whether the execution was successful or failed.
         *
         * @param mutexName The unique identifier for the mutex lock.
         * @param block The code to execute within the locked section.
         * @return The result of the block of code execution.
         */
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

    /**
     * Executes a block of code within the context of a store-level lock, ensuring thread-safe access
     * to shared resources or operations performed using the provided `MutexLockedScope`.
     *
     * @param block A suspending block of code to execute, which receives a `MutexLockedScope` for managing
     * specific mutex locks during execution.
     * @return The result produced by the execution of the provided block.
     */
    protected suspend fun <T> withStoreLock(block: suspend MutexLockedScope.() -> T): T =
        mutexMapLock.withLock { block(MutexLockedScope(mutexMap)) }

    override suspend fun close() {
    }
}
