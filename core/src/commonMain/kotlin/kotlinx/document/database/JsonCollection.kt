package kotlinx.document.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.document.database.maps.Collection
import kotlinx.document.database.maps.IdGenerator
import kotlinx.document.database.maps.IndexOfIndexes
import kotlinx.document.database.maps.asIndex
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

public class JsonCollection(
    override val name: String,
    override val json: Json,
    private val mutex: Mutex,
    private val store: DataStore,
    private val indexMap: IndexOfIndexes,
    private val genIdMap: IdGenerator,
    private val collection: Collection,
) : KotlinxDatabaseCollection {
    private suspend fun generateId() = genIdMap.update(name, 0L) { it + 1L }.newValue

    private suspend fun getIndexInternal(field: String) =
        when {
            !hasIndex(field) -> null
            else -> getIndexMap(field)
        }

    private suspend fun getIndexMap(field: String) = store.getMap("index:$name:$field").asIndex()

    private suspend fun hasIndex(field: String) = indexMap.get(name)?.contains(field) ?: false

    public fun iterateAll(): Flow<JsonObject> =
        collection
            .entries()
            .map { json.parseToJsonElement(it.value).jsonObject }

    override suspend fun createIndex(selector: String): Unit =
        mutex.withLock {
            if (hasIndex(selector)) return

            val index = getIndexMap(selector)
            indexMap.update(name, listOf(selector)) { it + selector }

            val query =
                selector
                    .split(".")
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
                        valueTransform = { it.second },
                    ).mapValues { it.value.toSet() }
                }
                .flatMapConcat { it.entries.asFlow() }
                .collect { (fieldValue, ids) ->
                    index.update(fieldValue, ids) { it + ids }
                }
        }

    public suspend fun findById(id: Long): JsonObject? {
        return json.decodeFromString(
            string = collection.get(id) ?: return null,
        )
    }

    private suspend fun findUsingIndex(
        field: String,
        value: String?,
    ) = getIndexInternal(field)
        ?.get(value)
        ?.asFlow()
        ?.mapNotNull { findById(it) }

    public suspend fun find(
        selector: String,
        value: String?,
    ): Flow<JsonObject> =
        findUsingIndex(selector, value)
            ?: iterateAll().filter {
                when (val selection = it.select(selector)) {
                    is JsonObjectSelectionResult.Found -> selection.value == value
                    JsonObjectSelectionResult.NotFound -> false
                    JsonObjectSelectionResult.Null ->
                        when (value) {
                            null -> true
                            else -> false
                        }
                }
            }

    override suspend fun removeById(id: Long): Unit =
        mutex.withLock(this) {
            val jsonString = collection.remove(id) ?: return@withLock
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject

            indexMap.get(name)
                ?.asSequence()
                ?.forEach { fieldSelector ->
                    val fieldValue =
                        when (val objectSelectionResult = jsonObject.select(fieldSelector)) {
                            is JsonObjectSelectionResult.Found -> objectSelectionResult.value
                            JsonObjectSelectionResult.NotFound -> return@forEach
                            JsonObjectSelectionResult.Null -> null
                        }
                    getIndexInternal(fieldSelector)?.update(fieldValue, emptySet()) { it - id }
                }
        }

    public suspend fun insert(value: JsonObject) {
        val id = value.id ?: generateId()
        val jsonString = json.encodeToString(value.copy(id))

        mutex.withLock(this) {
            collection.put(id, jsonString)
            indexMap.get(name)?.forEach { fieldSelector ->
                val fieldValue =
                    when (val objectSelectionResult = value.select(fieldSelector)) {
                        is JsonObjectSelectionResult.Found -> objectSelectionResult.value
                        JsonObjectSelectionResult.NotFound -> return@forEach
                        JsonObjectSelectionResult.Null -> null
                    }

                getIndexInternal(fieldSelector)?.update(fieldValue, setOf(id)) { it + id }
            }
        }
    }

    override suspend fun size(): Long = collection.size()

    override suspend fun clear(): Unit = collection.clear()

    override suspend fun getAllIndexNames(): List<String> = indexMap.get(name) ?: emptyList()

    override suspend fun getIndex(selector: String): Map<String?, Set<Long>>? =
        getIndexInternal(selector)
            ?.entries()
            ?.toMap()

    override suspend fun dropIndex(selector: String): Unit =
        mutex.withLock(this) {
            store.deleteMap("$name.$selector")
            indexMap.update(name, emptyList()) { it - selector }
        }

    override suspend fun details(): CollectionDetails =
        CollectionDetails(
            idGeneratorState = genIdMap.get(name) ?: 0L,
            indexes = indexMap.get(name)?.mapNotNull { getIndex(it) } ?: emptyList(),
        )
}

private val JsonObject.id
    get() = get(KotlinxDocumentDatabase.ID_PROPERTY_NAME)?.jsonPrimitive?.contentOrNull?.toLong()
