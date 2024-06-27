package com.github.lamba92.kotlin.db

import com.github.lamba92.kotlin.db.KotlinxDb.Companion.ID_PROPERTY_NAME
import java.nio.file.Path
import kotlin.annotation.AnnotationTarget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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
        return KotlinxDb(
            store = store,
            bufferSize = bufferSize,
            json = Json(json) { ignoreUnknownKeys = true }
        )
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
    var bufferSizeCounter = 0

    suspend fun commit() = withContext(Dispatchers.IO) {
        store.commit()
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        store.close()
    }
}

inline fun <reified T : Any> KotlinxDb.getCollection(name: String): DatabaseCollection<T> {
    val dbServices = DatabaseServices {
        if (++bufferSizeCounter >= bufferSize) {
            withContext(Dispatchers.IO) { store.commit() }
            bufferSizeCounter = 0
        }
    }

    return DatabaseCollection(
        name = name,
        services = dbServices,
        serializer = json.serializersModule.serializer<T>(),
        json = json,
        store = store
    )
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
    private val serializer: KSerializer<T>,
    private val json: Json,
    private val store: MVStore
) {

    private val mutex = Mutex()
    private val map: MVMap<Long, String> by lazy { store.openMap(name) }
    private val indexesMap: MVMap<String, Boolean> by lazy { store.openMap("$name.indexes") }
    private val genIdMap: MVMap<String, Long> by lazy { store.openMap(KotlinxDb.ID_GEN_MAP_NAME) }


    // field example: "address.street"
    //                "address.number"
    //                "addresses.$0.street"
    suspend fun createIndex(selector: String, unique: Boolean) = mutex.withLock {
        val index = withContext(Dispatchers.IO) {
            indexesMap[selector] = unique
            store.openMap<String?, Set<Long>>("$name.$selector")
        }
        val filedSegments = selector.split(".")
        asJsonElementFlow()
            .mapNotNull {
                val id = it.getValue(ID_PROPERTY_NAME).jsonPrimitive.long
                when (val value = it.getValueFromSegments(filedSegments)) {
                    is JsonObjectSelectionResult.Found -> value.value to id
                    JsonObjectSelectionResult.NotFound -> return@mapNotNull null
                    JsonObjectSelectionResult.Null -> null to id
                }
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
                index[fieldValue] = index[fieldValue]?.plus(ids) ?: ids.toSet()
            }
    }

    suspend fun find(field: String, value: String): Flow<T> {
        val hasIndex = withContext(Dispatchers.IO) {
            store.hasMap("$name.$field")
        }
        return when {
            hasIndex -> findUsingIndex(field, value)
            else -> asJsonElementFlow()
                .filter { it[field]?.jsonPrimitive?.contentOrNull == value }
                .map { json.decodeFromJsonElement(serializer, it) }
        }
    }

    private suspend fun findUsingIndex(field: String, value: String): Flow<T> {
        val index = withContext(Dispatchers.IO) {
            store.openMap<String?, Set<Long>>("$name.$field")
        }
        return index[value]
            ?.asFlow()
            ?.mapNotNull { findById(it) }
            ?: emptyFlow()
    }

    suspend fun insert(value: T) {
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
            ?: withContext(Dispatchers.IO) {
                mutex.withLock {
                    val newId = genIdMap.getOrDefault(name, Long.MIN_VALUE)
                    genIdMap[name] = newId + 1
                    newId
                }
            }

        val jsonString = json.encodeToString(jsonObject.copy(id))

        withContext(Dispatchers.IO) {
            map[id] = jsonString
            mutex.withLock {
                indexesMap
                    .asSequence()
                    .forEach { (fieldSelector, _) -> // ENFORCE UNIQUE INDEXES TODO
                        val filedSegments = fieldSelector.split(".")
                        val objectSelectionResult = jsonObject.getValueFromSegments(filedSegments)
                        val fieldValue = when (objectSelectionResult) {
                            is JsonObjectSelectionResult.Found -> objectSelectionResult.value
                            JsonObjectSelectionResult.NotFound -> return@forEach
                            JsonObjectSelectionResult.Null -> null
                        }

                        val index: MVMap<String, Set<Long>> = store.openMap("$name.$fieldSelector")
                        val ids = index[fieldValue]?.plus(id) ?: setOf(id)
                        index[fieldValue] = ids
                    }
            }
        }

        services.commitIfNecessary()
    }

    suspend fun findById(id: Long): T? {
        return json.decodeFromString(
            deserializer = serializer,
            string = withContext(Dispatchers.IO) { map[id] } ?: return null
        )
    }

    fun asJsonElementFlow() = map
        .asSequence()
        .asFlow()
        .flowOn(Dispatchers.IO)
        .map { json.parseToJsonElement(it.value).jsonObject }

    fun asFlow() = asJsonElementFlow()
        .map { json.decodeFromJsonElement(serializer, it) }

}

private fun JsonObject.copy(id: Long) =
    JsonObject(toMutableMap().also { it[ID_PROPERTY_NAME] = JsonPrimitive(id.toLong()) })

fun interface DatabaseServices {
    suspend fun commitIfNecessary()
}
