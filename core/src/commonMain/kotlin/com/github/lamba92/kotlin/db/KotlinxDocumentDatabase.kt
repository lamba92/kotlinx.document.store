package com.github.lamba92.kotlin.db

import com.github.lamba92.kotlin.db.KotlinxDocumentDatabase.Companion.ID_PROPERTY_NAME
import com.github.lamba92.kotlin.db.maps.asCollectionMap
import com.github.lamba92.kotlin.db.maps.asIdGenerator
import com.github.lamba92.kotlin.db.maps.asIndexOfIndexes
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer

class KotlinxDocumentDatabase internal constructor(
    private val store: DataStore,
    private val json: Json
) : AutoCloseable by store {

    companion object {
        const val ID_GEN_MAP_NAME = "id_gen"
        const val ID_PROPERTY_NAME = "_id"
        const val INDEXES_MAP_NAME = "indexes"
        const val COLLECTIONS = "collections"
    }

    private val mutexMap = ConcurrentHashMap<String, Mutex>()

    suspend fun getJsonCollection(name: String): JsonCollection {
        store.getMap(COLLECTIONS).put(name, "")
        return JsonCollection(
            name = name,
            json = json,
            mutex = mutexMap.getOrPut(name) { Mutex() },
            store = store,
            indexMap = store.getMap(INDEXES_MAP_NAME).asIndexOfIndexes(),
            genIdMap = store.getMap(ID_GEN_MAP_NAME).asIdGenerator(),
            collection = store.getMap(name).asCollectionMap()
        )
    }

    suspend fun deleteCollection(name: String) {
        store.deleteMap(name)
        store.getMap(COLLECTIONS).remove(name)
    }

    suspend fun getAllCollections() =
        getAllCollectionNames().map { getJsonCollection(it) }

    suspend fun getAllCollectionNames() =
        store.getMap(COLLECTIONS)
            .entries()
            .map { it.key }

    suspend fun databaseDetails() = store.getMap(COLLECTIONS)
        .entries()
        .map { getJsonCollection(it.key).details() }
        .toList()
}

@Serializable
data class CollectionDetails(
    val idGeneratorState: Long,
    val indexes: List<Map<String?, Set<Long>>>
)


@JvmName("toMapEntry")
internal suspend fun <K, V> Flow<Map.Entry<K, V>>.toMap() = buildMap {
    collect {
        put(it.key, it.value)
    }
}

suspend inline fun <reified T : Any> KotlinxDocumentDatabase.getObjectCollection(name: String) =
    getJsonCollection(name).toObjectCollection<T>()

inline fun <reified T : Any> JsonCollection.toObjectCollection() =
    ObjectCollection<T>(
        jsonCollection = this,
        serializer = json.serializersModule.serializer()
    )

internal fun JsonObject.copy(id: Long) =
    JsonObject(toMutableMap().also { it[ID_PROPERTY_NAME] = JsonPrimitive(id) })
