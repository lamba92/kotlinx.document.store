package com.github.lamba92.kotlin.db

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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.h2.mvstore.MVMap
import org.h2.mvstore.MVStore

class JsonCollection(
    override val name: String,
    override val json: Json,
    private val mutex: Mutex,
    private val store: MVStore,
) : KotlinxDbCollection {

    private val indexesMap: MVMap<String, List<String>> by lazy { store.openMap("indexes") }
    private val genIdMap: MVMap<String, Long> by lazy { store.openMap(KotlinxDb.ID_GEN_MAP_NAME) }
    private val collection: MVMap<Long, String> by lazy { store.openMap(name) }

    private suspend fun generateId() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val newId = genIdMap.getOrDefault(name, Long.MIN_VALUE)
            genIdMap[name] = newId
            newId + 1
        }
    }

    private suspend fun hasIndex(selector: String) = withContext(Dispatchers.IO) {
        indexesMap[name]?.contains(selector) ?: false
    }

    private suspend fun getIndex(field: String): MVMap<String?, Set<Long>> =
        withContext(Dispatchers.IO) { store.openMap("$name.$field") }

    fun iterateAll() = collection
        .asSequence()
        .asFlow()
        .flowOn(Dispatchers.IO)
        .map { json.parseToJsonElement(it.value).jsonObject }

    override suspend fun createIndex(selector: String) = mutex.withLock {
        if (hasIndex(selector)) return@withLock

        val index = getIndex(selector)

        val query = selector.split(".")

        iterateAll()
            .mapNotNull {
                val id = it.id ?: return@mapNotNull null
                when (val value = it.select(query)) {
                    is JsonObjectSelectionResult.Found -> value.value to id
                    JsonObjectSelectionResult.NotFound -> return@mapNotNull null
                    JsonObjectSelectionResult.Null -> null to id
                }
            }
            .chunked(10)
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

    suspend fun findById(id: Long): JsonObject? {
        return json.decodeFromString(
            string = withContext(Dispatchers.IO) { collection[id] } ?: return null
        )
    }

    private suspend fun findUsingIndex(field: String, value: String?) =
        getIndex(field)[value]
            ?.asFlow()
            ?.mapNotNull { findById(it) }
            ?: emptyFlow()

    suspend fun find(selector: String, value: String?): Flow<JsonObject> = when {
        hasIndex(selector) -> findUsingIndex(selector, value)
        else -> iterateAll().filter {
            when (val selection = it.select(selector)) {
                is JsonObjectSelectionResult.Found -> selection.value == value
                JsonObjectSelectionResult.NotFound -> false
                JsonObjectSelectionResult.Null -> when (value) {
                    null -> true
                    else -> false
                }
            }
        }
    }

    override suspend fun removeById(id: Long) = mutex.withLock {
        val jsonString = withContext(Dispatchers.IO) { collection.remove(id) } ?: return@withLock
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject

        indexesMap[name]
            ?.asSequence()
            ?.forEach { fieldSelector ->
                val fieldValue = when (val objectSelectionResult = jsonObject.select(fieldSelector)) {
                    is JsonObjectSelectionResult.Found -> objectSelectionResult.value
                    JsonObjectSelectionResult.NotFound -> return@forEach
                    JsonObjectSelectionResult.Null -> null
                }

                val index = getIndex(fieldSelector)
                index[fieldValue] = index[fieldValue]?.minus(id) ?: emptySet()
            }

    }

    suspend fun insert(value: JsonObject) {
        val id = value.id ?: generateId()

        val jsonString = json.encodeToString(value.copy(id))

        withContext(Dispatchers.IO) {
            mutex.withLock {
                collection[id] = jsonString
                indexesMap[name]
                    ?.asSequence()
                    ?.forEach { fieldSelector ->
                        val fieldValue = when (val objectSelectionResult = value.select(fieldSelector)) {
                            is JsonObjectSelectionResult.Found -> objectSelectionResult.value
                            JsonObjectSelectionResult.NotFound -> return@forEach
                            JsonObjectSelectionResult.Null -> null
                        }

                        val index = getIndex(fieldSelector)
                        index[fieldValue] = index[fieldValue]?.plus(id) ?: setOf(id)
                    }
            }
        }
    }

    override val size
        get() = collection.sizeAsLong()
}