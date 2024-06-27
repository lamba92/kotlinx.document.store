package com.github.lamba92.kotlin.db

import com.github.lamba92.kotlin.db.KotlinxDb.Companion.ID_PROPERTY_NAME
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import org.h2.mvstore.MVStore

class KotlinxDb internal constructor(
    private val store: MVStore,
    private val json: Json
) {

    companion object {
        const val ID_GEN_MAP_NAME = "id_gen"
        const val ID_PROPERTY_NAME = "_id"
    }

    private val mutexMap = ConcurrentHashMap<String, Mutex>()

    suspend fun commit() = withContext(Dispatchers.IO) {
        store.commit()
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        store.close()
    }

    fun getJsonCollection(name: String) =
        JsonCollection(
            name = name,
            json = json,
            mutex = mutexMap.getOrPut(name) { Mutex() },
            store = store,
        )

}

inline fun <reified T : Any> KotlinxDb.getCollection(name: String) =
    getJsonCollection(name).toObjectCollection<T>()

inline fun <reified T : Any> JsonCollection.toObjectCollection() =
    ObjectCollection<T>(
        jsonCollection = this,
        serializer = json.serializersModule.serializer()
    )

internal fun JsonObject.copy(id: Long) =
    JsonObject(toMutableMap().also { it[ID_PROPERTY_NAME] = JsonPrimitive(id) })
