package com.github.lamba92.kotlin.db

import com.github.lamba92.kotlin.db.KotlinxDb.Companion.ID_PROPERTY_NAME
import java.nio.file.Path
import kotlin.annotation.AnnotationTarget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.serializer
import org.h2.mvstore.MVMap
import org.h2.mvstore.MVStore

class KotlinxDbBuilder {
    var filePath: Path? = null
    var json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    var cacheSize = 1.megabytes
    var bufferSize = KotlinxDb.DEFAULT_BUFFER_SIZE

    fun build(): KotlinxDb {
        val path = filePath ?: error("File path must be provided")
        val store = MVStore.Builder()
            .fileName(path.toString())
            .autoCommitBufferSize(Integer.MAX_VALUE)
            .cacheSize(cacheSize.megabytes.toInt())
            .open()
        return KotlinxDb(store, bufferSize, Json(json) { ignoreUnknownKeys = true })
    }
}

fun kotlinxDb(block: KotlinxDbBuilder.() -> Unit) = KotlinxDbBuilder().apply(block).build()


class KotlinxDb internal constructor(
    @InternalDbApi val store: MVStore,
    @InternalDbApi val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    @InternalDbApi val json: Json
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
        val map: MVMap<ULong, String> = store.openMap(name)
        val idGenMap: MVMap<String, ULong> = store.openMap(ID_GEN_MAP_NAME)

        val dbServices = object : DatabaseServices {
            override suspend fun generateId(): ULong = withContext(Dispatchers.IO) {
                idMutexMap.getOrPut(name) { Mutex() }.withLock {
                    val id = idGenMap.getOrDefault(name, 0u)
                    idGenMap[name] = id + 1u
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

        return DatabaseCollection(
            name = name,
            services = dbServices,
            map = map,
            serializer = json.serializersModule.serializer<T>(),
            json = json,
            store = store
        )
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        store.close()
    }
}

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is internal to the library and should not be used outside of it."
)
@Target(
    CLASS,
    FUNCTION,
    PROPERTY,
    PROPERTY_GETTER,
    PROPERTY_SETTER,
)
annotation class InternalDbApi

class DatabaseCollection<T : Any>(
    private val name: String,
    private val services: DatabaseServices,
    private val map: MVMap<ULong, String>,
    private val serializer: KSerializer<T>,
    private val json: Json,
    private val store: MVStore // ugly AF TODO remove or remove other map
) {

    // mutex? TODO
    suspend fun createIndex(field: String) {

        val index = withContext(Dispatchers.IO) {
            store.openMap<String?, Set<ULong>>("$name.$field")
        }

        asJsonElementFlow()
            .map {
                val id = it.getValue(ID_PROPERTY_NAME).jsonPrimitive.long.toULong()
                val value = it[field]?.jsonPrimitive?.contentOrNull
                value to id
            }
            .chunked(100)
            .map {
                it.groupBy(
                    keySelector = { it.first },
                    valueTransform = { it.second }
                )
            }
            .flatMapConcat { it.entries.asFlow() }
            .collect { (fieldValue, ids) ->
                // MUTEX! TODO
                index[fieldValue] = index[fieldValue]?.plus(ids) ?: ids.toSet()
            }
    }

    suspend fun find(field: String, value: String): List<T> {
        val hasIndex = withContext(Dispatchers.IO) {
            store.hasMap("$name.$field")
        }
        return when {
            hasIndex -> findUsingIndex(field, value)
            else -> asJsonElementFlow()
                .filter { it[field]?.jsonPrimitive?.contentOrNull == value }
                .map { json.decodeFromJsonElement(serializer, it) }
                .toList()
        }
    }

    private suspend fun findUsingIndex(field: String, value: String): List<T> {
        val index = withContext(Dispatchers.IO) {
            store.openMap<String?, Set<ULong>>("$name.$field")
        }
        return index[value]
            ?.asFlow()
            ?.mapNotNull { findById(it) }
            ?.toList()
            ?: emptyList()
    }

    suspend fun insertOne(value: T) {
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

        val id = jsonObject[ID_PROPERTY_NAME]
            ?.jsonPrimitive
            ?.long
            ?.toULong()
            ?: services.generateId()

        val jsonString = json.encodeToString(jsonObject.copy(id))

        withContext(Dispatchers.IO) {
            map[id] = jsonString
        }

        services.commitIfNecessary()
    }

    suspend fun findById(id: ULong): T? {
        return json.decodeFromString(
            deserializer = serializer,
            string = withContext(Dispatchers.IO) { map[id] } ?: return null
        )
    }

    private fun asJsonElementFlow() = map
        .asSequence()
        .asFlow()
        .flowOn(Dispatchers.IO)
        .map { json.parseToJsonElement(it.value).jsonObject }

    fun asFlow() = asJsonElementFlow()
        .map { json.decodeFromJsonElement(serializer, it) }

}

private fun JsonObject.copy(id: ULong) =
    JsonObject(toMutableMap().also { it[ID_PROPERTY_NAME] = JsonPrimitive(id.toLong()) })

interface DatabaseServices {
    suspend fun generateId(): ULong
    suspend fun commitIfNecessary()
}
