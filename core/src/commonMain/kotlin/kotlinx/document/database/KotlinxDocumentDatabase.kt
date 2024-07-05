package kotlinx.document.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.document.database.KotlinxDocumentDatabase.Companion.ID_PROPERTY_NAME
import kotlinx.document.database.maps.asCollectionMap
import kotlinx.document.database.maps.asIdGenerator
import kotlinx.document.database.maps.asIndexOfIndexes
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import kotlin.jvm.JvmName

public class KotlinxDocumentDatabase internal constructor(
    private val store: DataStore,
    private val json: Json,
) : AutoCloseable by store {
    public companion object {
        public const val ID_GEN_MAP_NAME: String = "id_gen"
        public const val ID_PROPERTY_NAME: String = "_id"
        public const val INDEXES_MAP_NAME: String = "indexes"
        public const val COLLECTIONS: String = "collections"
    }

    private val mutex = Mutex()
    private val mutexMap = mutableMapOf<String, Mutex>()

    public suspend fun getJsonCollection(name: String): JsonCollection {
        store.getMap(COLLECTIONS).put(name, "")
        return JsonCollection(
            name = name,
            json = json,
            mutex = mutex.withLock { mutexMap.getOrPut(name) { Mutex() } },
            store = store,
            indexMap = store.getMap(INDEXES_MAP_NAME).asIndexOfIndexes(),
            genIdMap = store.getMap(ID_GEN_MAP_NAME).asIdGenerator(),
            collection = store.getMap(name).asCollectionMap(),
        )
    }

    public suspend fun deleteCollection(name: String) {
        store.deleteMap(name)
        store.getMap(COLLECTIONS).remove(name)
    }

    public suspend fun getAllCollections(): Flow<JsonCollection> = getAllCollectionNames().map { getJsonCollection(it) }

    public suspend fun getAllCollectionNames(): Flow<String> =
        store.getMap(COLLECTIONS)
            .entries()
            .map { it.key }

    public suspend fun databaseDetails(): List<CollectionDetails> =
        store.getMap(COLLECTIONS)
            .entries()
            .map { getJsonCollection(it.key).details() }
            .toList()
}

@Serializable
public data class CollectionDetails(
    val idGeneratorState: Long,
    val indexes: List<Map<String?, Set<Long>>>,
)

@JvmName("toMapEntry")
internal suspend fun <K, V> Flow<Map.Entry<K, V>>.toMap() =
    buildMap {
        collect {
            put(it.key, it.value)
        }
    }

public suspend inline fun <reified T : Any> KotlinxDocumentDatabase.getObjectCollection(name: String): ObjectCollection<T> =
    getJsonCollection(name).toObjectCollection<T>()

public inline fun <reified T : Any> JsonCollection.toObjectCollection(): ObjectCollection<T> =
    ObjectCollection(
        jsonCollection = this,
        serializer = json.serializersModule.serializer(),
    )

internal fun JsonObject.copy(id: Long) = JsonObject(toMutableMap().also { it[ID_PROPERTY_NAME] = JsonPrimitive(id) })
