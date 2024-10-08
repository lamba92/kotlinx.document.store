package kotlinx.document.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

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

    private suspend fun getIndexMap(fieldSelector: String) = store.getMap("index:$name:$fieldSelector").asIndex()

    private suspend fun hasIndex(field: String) = indexMap.get(name)?.contains(field) ?: false

    public fun iterateAll(fromIndex: Long = 0L): Flow<JsonObject> =
        collection
            .entries(fromIndex)
            .map { json.parseToJsonElement(it.value).jsonObject }

    override suspend fun createIndex(selector: String): Unit =
        mutex.withLock {
            if (hasIndex(selector)) return

            val index = getIndexMap(selector)
            indexMap.update(name, listOf(selector)) { it + selector }

            val query = selector.split(".")

            iterateAll()
                .mapNotNull {
                    val id = it.id ?: return@mapNotNull null
                    val selectResult = it.select(query) ?: return@mapNotNull null
                    selectResult to id
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
        value: JsonElement,
    ) = getIndexInternal(field)
        ?.get(value)
        ?.asFlow()
        ?.mapNotNull { findById(it) }

    public suspend fun find(
        selector: String,
        value: JsonElement,
    ): Flow<JsonObject> =
        findUsingIndex(selector, value)
            ?: iterateAll().filter { it.select(selector) == value }

    public suspend fun removeById(id: Long): JsonObject? = mutex.withLock { removeByIdUnsafe(id) }

    private suspend fun removeByIdUnsafe(id: Long): JsonObject? {
        val jsonString = collection.remove(id) ?: return null
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject

        indexMap.get(name)
            ?.asSequence()
            ?.forEach { fieldSelector ->
                getIndexInternal(fieldSelector)
                    ?.update(
                        key = jsonObject.select(fieldSelector) ?: return@forEach,
                        value = emptySet(),
                        updater = { it - id },
                    )
            }
        return jsonObject
    }

    public suspend fun insert(value: JsonObject): JsonObject = mutex.withLock { insertUnsafe(value) }

    private suspend fun insertUnsafe(value: JsonObject): JsonObject {
        val id = value.id ?: generateId()
        val jsonObjectWithId = value.copy(id)
        val jsonString = json.encodeToString(jsonObjectWithId)

        collection.put(id, jsonString)
        indexMap.get(name)
            ?.forEach { fieldSelector ->
                getIndexInternal(fieldSelector)
                    ?.update(
                        key = jsonObjectWithId.select(fieldSelector) ?: return@forEach,
                        value = setOf(id),
                        updater = { it + id },
                    )
            }

        return jsonObjectWithId
    }

    override suspend fun size(): Long = collection.size()

    override suspend fun clear(): Unit = collection.clear()

    override suspend fun getAllIndexNames(): List<String> = indexMap.get(name) ?: emptyList()

    override suspend fun getIndex(selector: String): Map<JsonElement, Set<Long>>? =
        getIndexInternal(selector)
            ?.entries()
            ?.toMap()

    override suspend fun dropIndex(selector: String): Unit =
        mutex.withLock {
            store.deleteMap("$name.$selector")
            indexMap.update(name, emptyList()) { it - selector }
        }

    override suspend fun details(): CollectionDetails =
        CollectionDetails(
            idGeneratorState = genIdMap.get(name) ?: 0L,
            indexes =
                indexMap.get(name)?.mapNotNull { selector ->
                    getIndex(selector)
                        ?.entries
                        ?.toMap()
                        ?.let { selector to it }
                }
                    ?.toMap()
                    ?: emptyMap(),
        )

    public suspend fun updateById(
        id: Long,
        update: suspend (JsonObject) -> JsonObject,
    ): Boolean = mutex.withLock { updateByIdUnsafe(id, update) }

    private suspend fun updateByIdUnsafe(
        id: Long,
        update: suspend (JsonObject) -> JsonObject?,
    ): Boolean {
        val item = collection.get(id) ?: return false
        val jsonObject = json.decodeFromString<JsonObject>(item)
        val newItem = update(jsonObject)?.copy(id) ?: return false
        insertUnsafe(newItem)
        return true
    }

    public suspend fun updateWhere(
        fieldSelector: String,
        fieldValue: JsonElement,
        update: suspend (JsonObject) -> JsonObject,
    ): Boolean = mutex.withLock { updateWhereUnsafe(fieldSelector, fieldValue, update) }

    private suspend fun JsonCollection.updateWhereUnsafe(
        fieldSelector: String,
        fieldValue: JsonElement,
        update: suspend (JsonObject) -> JsonObject,
    ): Boolean {
        val index = getIndexInternal(fieldSelector)
        var found = false
        if (index != null) {
            val ids = index.get(fieldValue)
            ids?.forEach { found = found || updateByIdUnsafe(it, update) }
            return found
        }

        iterateAll()
            .filter { it.select(fieldSelector) == fieldValue }
            .collect { item ->
                found = true
                updateByIdUnsafe(item.id ?: return@collect, update)
            }
        return found
    }

    public suspend fun updateWhere(
        fieldSelector: String,
        fieldValue: JsonElement,
        upsert: Boolean,
        update: JsonObject,
    ): Boolean =
        mutex.withLock {
            val updated = updateWhereUnsafe(fieldSelector, fieldValue) { update }
            if (!updated && upsert) {
                insertUnsafe(update)
                return true
            }
            updated
        }

    override suspend fun removeWhere(
        fieldSelector: String,
        fieldValue: JsonElement,
    ): Unit =
        mutex.withLock {
            val ids =
                getIndexInternal(fieldSelector)
                    ?.get(fieldValue)

            if (ids != null) {
                ids.forEach { removeByIdUnsafe(it) }
                return
            }

            iterateAll()
                .filter { it.select(fieldSelector) == fieldValue }
                .mapNotNull { it.id }
                .collect { removeByIdUnsafe(it) }
        }
}

public suspend inline fun <reified K> JsonCollection.updateWhere(
    fieldSelector: String,
    fieldValue: K,
    noinline update: suspend (JsonObject) -> JsonObject,
): Boolean =
    updateWhere(
        fieldSelector = fieldSelector,
        fieldValue =
            json.encodeToJsonElement(
                serializer = json.serializersModule.serializer<K>(),
                value = fieldValue,
            ),
        update = update,
    )

public suspend inline fun <reified K> JsonCollection.updateWhere(
    fieldSelector: String,
    fieldValue: K,
    upsert: Boolean,
    update: JsonObject,
): Boolean =
    updateWhere(
        fieldSelector = fieldSelector,
        fieldValue =
            json.encodeToJsonElement(
                serializer = json.serializersModule.serializer<K>(),
                value = fieldValue,
            ),
        upsert = upsert,
        update = update,
    )

public suspend inline fun <reified K> KotlinxDatabaseCollection.removeWhere(
    fieldSelector: String,
    fieldValue: K,
): Unit =
    removeWhere(
        fieldSelector = fieldSelector,
        fieldValue =
            json.encodeToJsonElement(
                serializer = json.serializersModule.serializer<K>(),
                value = fieldValue,
            ),
    )

private fun <K, V> Iterable<Map.Entry<K, V>>.toMap() = buildMap { this@toMap.forEach { put(it.key, it.value) } }

private val JsonObject.id: Long?
    get() = get(KotlinxDocumentDatabase.ID_PROPERTY_NAME)?.jsonPrimitive?.contentOrNull?.toLong()
