package kotlinx.document.store

/**
 * Provides persistent, suspendable operations
 * for managing named maps. A `DataStore` allows creation, retrieval, and deletion of
 * named persistent maps, each of type [PersistentMap].
 *
 */
public interface DataStore : SuspendCloseable {
    /**
     * Retrieves an existing persistent map or creates a new one with the specified name.
     *
     * @param name The name of the persistent map to retrieve or create.
     * @return A [PersistentMap] instance associated with the specified name.
     */
    public suspend fun getMap(name: String): PersistentMap<String, String>

    /**
     * Deletes an existing persistent map identified by the specified name.
     * This operation removes all the data on disk as well.
     *
     * @param name The name of the persistent map to be deleted.
     */
    public suspend fun deleteMap(name: String)
}
