package com.github.lamba92.kotlin.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.serializer
import org.h2.mvstore.MVMap
import org.h2.mvstore.MVStore

class Db private constructor(
    val store: MVStore,
    val bufferSize: Int = DEFAULT_BUFFER_SIZE
) {

    companion object {
        const val ID_GEN_MAP_NAME = "id_gen"
        const val DEFAULT_BUFFER_SIZE = 10
        const val ID_PROPERTY_NAME = "_id"
    }

    @InternalDbApi
    val idMutexMap = mutableMapOf<String, Mutex>()

    @InternalDbApi
    var bufferSizeCounter = 0

    inline fun <reified T : Any> getCollection(name: String): DatabaseCollection<T> {
        val map: MVMap<Long, String> = store.openMap(name)
        val idGenMap: MVMap<String, Long> = store.openMap(ID_GEN_MAP_NAME)

        val dbServices = object : DatabaseServices {
            override suspend fun generateId(): Long = withContext(Dispatchers.IO) {
                idMutexMap.getOrPut(name) { Mutex() }.withLock {
                    val id = idGenMap.getOrDefault(name, 0)
                    idGenMap[name] = id + 1
                    id
                }
            }

            override suspend fun commitIfNecessary() {
                if (++bufferSizeCounter >= bufferSize) {
                    withContext(Dispatchers.IO) { store.commit() }
                    bufferSizeCounter = 0
                }
            }
        }

        return DatabaseCollection(dbServices, map, serializer<T>(), Json)
    }


    suspend fun close() = withContext(Dispatchers.IO) {
        store.close()
    }
}

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is internal to the library and should not be used outside of it."
)
annotation class InternalDbApi

class DatabaseCollection<T : Any>(
    private val services: DatabaseServices,
    private val map: MVMap<Long, String>,
    private val serializer: KSerializer<T>,
    private val json: Json
) {

    suspend fun insertOne(value: T)  {
        val jsonObject = json.encodeToJsonElement(serializer, value)
        if (jsonObject !is JsonObject) {
            val s = when (jsonObject) {
                is JsonArray -> "an array-like object"
                is JsonPrimitive -> "a primitive"
                JsonNull -> "null"
                else -> "an unknown type"
            }
            error("Expected and object but got $s")
        }
        val id = jsonObject[Db.ID_PROPERTY_NAME]?.jsonPrimitive?.long ?: services.generateId()
        val jsonString = json.encodeToString(jsonObject.copy(id))
        withContext(Dispatchers.IO) {
            map[id] = jsonString
        }
        services.commitIfNecessary()
    }

    suspend fun findById(id: Long): T? {
        return json.decodeFromString(
            deserializer = serializer,
            string = withContext(Dispatchers.IO) { map[id] } ?: return null
        )
    }

    fun asFlow() = map
        .asSequence()
        .asFlow()
        .flowOn(Dispatchers.IO)
        .map { json.decodeFromString(serializer, it.value) }

}

private fun JsonObject.copy(id: Long) =
    JsonObject(toMutableMap().also { it[Db.ID_PROPERTY_NAME] = JsonPrimitive(id) })

interface DatabaseServices {
    suspend fun generateId(): Long
    suspend fun commitIfNecessary()
}