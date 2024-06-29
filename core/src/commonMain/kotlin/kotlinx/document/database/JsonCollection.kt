package kotlinx.document.database

import com.github.lamba92.kotlin.db.maps.CollectionMap
import com.github.lamba92.kotlin.db.maps.IdGenerator
import com.github.lamba92.kotlin.db.maps.IndexOfIndexes
import com.github.lamba92.kotlin.db.maps.asIndex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class JsonCollection(
    override val name: String,
    override val json: Json,
    private val mutex: Mutex,
    private val store: kotlinx.document.database.DataStore,
    private val indexMap: IndexOfIndexes,
    private val genIdMap: IdGenerator,
    private val collection: CollectionMap,
) : kotlinx.document.database.KotlinxDbCollection {

    private suspend fun generateId() = genIdMap.update(name, 0L) { it + 1L }.newValue

    private suspend fun _getIndex(field: String) = when {
        !hasIndex(field) -> null
        else -> getIndexMap(field)
    }


    private suspend fun getIndexMap(field: String) =
        store.getMap("$name.$field").asIndex()

    private suspend fun hasIndex(field: String) =
        indexMap.get(name)?.contains(field) ?: false

    fun iterateAll() = collection
        .entries()
        .map { json.parseToJsonElement(it.value).jsonObject }

    override suspend fun createIndex(selector: String) {
        if (hasIndex(selector)) return

        val index = getIndexMap(selector)
        indexMap.update(name, listOf(selector)) { it + selector }

        val query = selector.split(".")
        mutex.withLock(this) {
            iterateAll()
                .mapNotNull {
                    val id = it.id ?: return@mapNotNull null
                    when (val value = it.select(query)) {
                        is kotlinx.document.database.JsonObjectSelectionResult.Found -> value.value to id
                        _root_ide_package_.kotlinx.document.database.JsonObjectSelectionResult.NotFound -> return@mapNotNull null
                        _root_ide_package_.kotlinx.document.database.JsonObjectSelectionResult.Null -> null to id
                    }
                }
                .chunked(10)
                .map {
                    it.groupBy(
                        keySelector = { it.first },
                        valueTransform = { it.second }
                    ).mapValues { it.value.toSet() }
                }
                .flatMapConcat { it.entries.asFlow() }
                .collect { (fieldValue, ids) ->
                    index.update(fieldValue, ids) { it + ids }
                }
        }
    }

    suspend fun findById(id: Long): JsonObject? {
        return json.decodeFromString(
            string = collection.get(id) ?: return null
        )
    }

    private suspend fun findUsingIndex(field: String, value: String?) =
        _getIndex(field)
            ?.get(value)
            ?.asFlow()
            ?.mapNotNull { findById(it) }

    suspend fun find(selector: String, value: String?): Flow<JsonObject> =
        findUsingIndex(selector, value)
            ?: iterateAll().filter {
                when (val selection = it.select(selector)) {
                    is _root_ide_package_.kotlinx.document.database.JsonObjectSelectionResult.Found -> selection.value == value
                    _root_ide_package_.kotlinx.document.database.JsonObjectSelectionResult.NotFound -> false
                    _root_ide_package_.kotlinx.document.database.JsonObjectSelectionResult.Null -> when (value) {
                        null -> true
                        else -> false
                    }
                }
            }


    override suspend fun removeById(id: Long) = mutex.withLock(this) {
        val jsonString = collection.remove(id) ?: return@withLock
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject

        indexMap.get(name)
            ?.asSequence()
            ?.forEach { fieldSelector ->
                val fieldValue = when (val objectSelectionResult = jsonObject.select(fieldSelector)) {
                    is _root_ide_package_.kotlinx.document.database.JsonObjectSelectionResult.Found -> objectSelectionResult.value
                    _root_ide_package_.kotlinx.document.database.JsonObjectSelectionResult.NotFound -> return@forEach
                    _root_ide_package_.kotlinx.document.database.JsonObjectSelectionResult.Null -> null
                }
                _getIndex(fieldSelector)?.update(fieldValue, emptySet()) { it - id }
            }

    }

    suspend fun insert(value: JsonObject) {
        val id = value.id ?: generateId()
        val jsonString = json.encodeToString(value.copy(id))

        mutex.withLock(this) {
            collection.put(id, jsonString)
            indexMap.get(name)
                ?.asSequence()
                ?.forEach { fieldSelector ->
                    val fieldValue = when (val objectSelectionResult = value.select(fieldSelector)) {
                        is _root_ide_package_.kotlinx.document.database.JsonObjectSelectionResult.Found -> objectSelectionResult.value
                        _root_ide_package_.kotlinx.document.database.JsonObjectSelectionResult.NotFound -> return@forEach
                        _root_ide_package_.kotlinx.document.database.JsonObjectSelectionResult.Null -> null
                    }

                    _getIndex(fieldSelector)?.update(fieldValue, setOf(id)) { it + id }
                }
        }
    }

    override suspend fun size() = collection.size()

    override suspend fun clear() = collection.clear()

    override suspend fun getAllIndexNames() = indexMap.get(name) ?: emptyList()

    override suspend fun getIndex(selector: String): Map<String?, Set<Long>>? =
        _getIndex(selector)
            ?.entries()
            ?.toMap()

    override suspend fun dropIndex(selector: String): Unit = mutex.withLock(this) {
        store.deleteMap("$name.$selector")
        indexMap.update(name, emptyList()) { it - selector }
    }

    override suspend fun details() = _root_ide_package_.kotlinx.document.database.CollectionDetails(
        idGeneratorState = genIdMap.get(name) ?: 0L,
        indexes = indexMap.get(name)?.mapNotNull { getIndex(it) } ?: emptyList()
    )
}

private val JsonObject.id
    get() = get(_root_ide_package_.kotlinx.document.database.KotlinxDocumentDatabase.Companion.ID_PROPERTY_NAME)?.jsonPrimitive?.contentOrNull?.toLong()