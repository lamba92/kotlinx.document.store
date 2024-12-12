package kotlinx.document.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.document.store.maps.asCollectionMap
import kotlinx.document.store.maps.asIdGenerator
import kotlinx.document.store.maps.asIndexOfIndexes
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import kotlin.jvm.JvmName

/**
 * Manages collections of JSON documents stored persistently.
 *
 * This class provides functionality to create, retrieve, and delete JSON-based collections.
 * Each collection is identified by its name and stored persistently using an underlying [DataStore].
 * The class allows managing collections, performing operations on documents, and obtaining
 * metadata about the database.
 *
 * @param store The underlying persistent data store for managing collection data.
 * @param json A `Json` instance used for serialization and deserialization of documents.
 */
public class KotlinxDocumentStore internal constructor(
    public val store: DataStore,
    public val json: Json,
) : SuspendCloseable by store {
    public companion object {
        /**
         * The name of the internal map used to manage and track unique identifier generation.
         */
        public const val ID_GEN_MAP_NAME: String = "id_gen"

        /**
         * Constant representing the name of the default identifier property used in stored JSONs.
         * See [JsonCollection.insert]
         */
        public const val ID_PROPERTY_NAME: String = "_id"

        /**
         * The name of the internal map used for storing index metadata within the `KotlinxDocumentStore`.
         */
        public const val INDEXES_MAP_NAME: String = "indexes"

        /**
         * Name of the internal map used to store the names of all collections within the `KotlinxDocumentStore`.
         */
        public const val COLLECTIONS: String = "collections"
    }

    private val mutex = Mutex()
    private val mutexMap = mutableMapOf<String, Mutex>()

    /**
     * Retrieves a [JsonCollection] instance by the given collection name.
     * Initializes and persists necessary data related to the collection if it does not already exist in the store.
     *
     * @param name the name of the collection to retrieve.
     * @return a [JsonCollection] instance associated with the specified name.
     */
    public suspend fun getJsonCollection(name: String): JsonCollection {
        store.getMap(COLLECTIONS).put(name, "1")
        return JsonCollection(
            name = name,
            json = json,
            mutex = mutex.withLock { mutexMap.getOrPut(name) { Mutex() } },
            store = store,
            indexMap = store.getMap(INDEXES_MAP_NAME).asIndexOfIndexes(),
            genIdMap = store.getMap(ID_GEN_MAP_NAME).asIdGenerator(),
            persistentCollection = store.getMap(name).asCollectionMap(),
        )
    }

    /**
     * Deletes an existing collection by its name. This method removes all related data
     * from the underlying store and updates the internal collections map to reflect the deletion.
     *
     * **NOTE**: This operation is irreversible and deletes all data within the collection.
     * Also, the behavior of any ongoing operations on the collection after deletion is undefined.
     *
     * @param name The name of the collection to be deleted.
     */
    public suspend fun deleteCollection(name: String): Unit =
        mutex.withLock {
            val mapMutex = mutexMap[name]
            mapMutex?.lock()
            store.deleteMap(name)
            store.getMap(COLLECTIONS).remove(name)
            mapMutex?.unlock()
        }

    /**
     * Retrieves a flow of all collection names stored in the persistent data store.
     *
     * @return A [Flow] that emits the names of all collections as strings.
     */
    public suspend fun getAllCollectionNames(): Flow<String> =
        store.getMap(COLLECTIONS)
            .entries()
            .map { it.key }

    /**
     * Retrieves detailed metadata about all collections present in the database.
     *
     * This method fetches and combines the [CollectionDetails] of each collection stored in the
     * database, including information such as ID generator state and index configurations.
     *
     * @return A map where the keys are collection names as Strings, and the values
     * are [CollectionDetails] objects containing metadata for each collection.
     */
    public suspend fun databaseDetails(): Map<String, CollectionDetails> =
        store.getMap(COLLECTIONS)
            .entries()
            .map { it.key to getJsonCollection(it.key).details() }
            .toList()
            .toMap()
}

/**
 * Represents detailed information about a collection within a persistent data store.
 *
 * @property idGeneratorState The current state of the ID generator for the collection, used to
 * generate unique identifiers.
 * @property indexes A map of index selectors and the values are a map with JSON
 * element values as keys and sets of ids as values.
 */
@Serializable
public data class CollectionDetails(
    val idGeneratorState: Long,
    val indexes: Map<String, Map<JsonElement, Set<Long>>>,
)

/**
 * Returns a new map containing all key-value pairs from the given flow of entries.
 *
 * The returned map preserves the entry emission order of the original flow.
 * If any of two pairs would have the same key the last one gets added to the map.
 */
@JvmName("toMapEntry")
internal suspend fun <K, V> Flow<Map.Entry<K, V>>.toMap() =
    buildMap {
        collect {
            put(it.key, it.value)
        }
    }

/**
 * Retrieves an [ObjectCollection] for the specified collection name and type.
 *
 * This method initializes or retrieves the collection by its name, converts it
 * from a [JsonCollection] to an [ObjectCollection] for the given type [T], and returns it.
 * The type [T] must be a serializable class.
 *
 * @param name The name of the collection to retrieve.
 * @return An [ObjectCollection] of the specified type [T].
 */
public suspend inline fun <reified T : Any> KotlinxDocumentStore.getObjectCollection(name: String): ObjectCollection<T> =
    getJsonCollection(name).toObjectCollection<T>()

/**
 * Converts the current JsonCollection into an ObjectCollection of the specified type.
 *
 * @return an ObjectCollection containing objects of type T, constructed from the data
 * in the JsonCollection.
 */
public inline fun <reified T : Any> JsonCollection.toObjectCollection(): ObjectCollection<T> =
    ObjectCollection(
        jsonCollection = this,
        serializer = json.serializersModule.serializer(),
    )
