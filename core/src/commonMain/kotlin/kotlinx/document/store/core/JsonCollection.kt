package kotlinx.document.store.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.document.store.core.maps.IdGenerator
import kotlinx.document.store.core.maps.Index
import kotlinx.document.store.core.maps.IndexOfIndexes
import kotlinx.document.store.core.maps.PersistentCollection
import kotlinx.document.store.core.maps.asIndex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

/**
 * A collection of [JsonElement] saved in a [DataStore]. This interface provides
 * various operations to manage the lifecycle and contents of a collection, such as
 * creating and dropping indexes, fetching collection details, and performing basic
 * CRUD operations.
 *
 * @property name The name of the collection in the [DataStore].
 *                This property uniquely identifies a collection and is used for performing
 *                operations such as indexing, querying, or managing its lifecycle.
 * @property json The [Json] instance used to serialize and deserialize JSON data.
 * @property mutex [Mutex] used to synchronize access to the collection.
 * It is shared across all collections in the [DataStore] with the same [name], ensuring
 * that only one operation can be performed on the collection at a time.
 * @property store The [DataStore] where the collection is stored.
 * @property indexMap A [PersistentMap] of indexes for the collection. Is implemented by [IndexOfIndexes].
 * @property genIdMap A [PersistentMap] with the last used ID of all collections. Is implemented by [IdGenerator].
 * @property persistentCollection The [PersistentMap] of JSON strings stored in the [DataStore].
 * Is implemented by [PersistentCollection].
 */
public class JsonCollection internal constructor(
    override val name: String,
    override val json: Json,
    private val mutex: Mutex,
    private val store: DataStore,
    private val indexMap: IndexOfIndexes,
    private val genIdMap: IdGenerator,
    private val persistentCollection: PersistentCollection,
) : KotlinxStoreCollection {
    /**
     * Generates a new unique identifier by incrementing the associated value within [genIdMap].
     *
     * This method operates on a [PersistentMap], using the key defined by the [name] property.
     * The value associated with this key is incremented atomically, ensuring that each call
     * to this method produces a unique result even in concurrent or asynchronous environments.
     *
     * @return The newly generated unique identifier as a `Long`.
     */
    private suspend fun generateId(): Long = genIdMap.update(name, 0L) { it + 1L }.newValue

    /**
     * Retrieves the index map for the specified field if it exists; otherwise, returns `null`.
     *
     * @param selector The selector for the field for which the index map is to be retrieved.
     * @return The index map for the specified field, or `null` if the field does not have an associated index.
     */
    private suspend fun getIndexOrNull(selector: String): Index? =
        when {
            !hasIndex(selector) -> null
            else -> getIndexMap(selector)
        }

    /**
     * Retrieves or creates an index map for the specified field selector.
     *
     * The index map is stored and accessed using [store], with
     * the name and field selector used to construct a unique key.
     *
     * @param fieldSelector The selector for the field selector for which the index map is to be retrieved or created.
     * @return The created or retrieved index map as an [Index] object.
     */
    private suspend fun getIndexMap(fieldSelector: String): Index = store.getMap("index:$name:$fieldSelector").asIndex()

    /**
     * Checks whether a specific field selector has an associated index in the collection.
     *
     * This method queries the [indexMap] using the collection's name to determine
     * if an index exists for the provided selector.
     *
     * @param selector The field selector for which to check the existence of an index.
     * @return `true` if an index exists for the given selector; otherwise, `false`.
     */
    private suspend fun hasIndex(selector: String): Boolean = indexMap.get(name)?.contains(selector) == true

    /**
     * Iterates through all entries in the persistent collection
     * @return A [Flow]<[JsonObject]> of all entries in the collection.
     */
    public fun iterateAll(): Flow<JsonObject> =
        persistentCollection
            .entries()
            .map { json.parseToJsonElement(it.value).jsonObject }

    /**
     * Creates an index for optimizing query performance on the collection.
     * This will speed up [find] operations that use the specified selector.
     * See [select] for more information on how to write selectors.
     *
     * For example, to create an index on a field named `key` in a JSON object:
     * ```kotlin
     * val json = Json.decodeFromString<JsonObject>("""
     * {
     *    "key": "value",
     *    "array": [1, 2, 3]
     * }
     *    """.trimIndent())
     * collection.createIndex("key")
     * ```
     * An index with all known values of the field `key` will be created.
     *
     * But also on an array indexes:
     * ```kotlin
     * collection.createIndex("array.$1")
     * ```
     * This will create an index on the second element of the array.
     *
     * If the key is missing in a JSON, it will not be indexed.
     *
     *
     * @param selector A dot separated string where each string specifies a key for a JsonObject or
     *                 an index (prefixed with '$') for a JsonArray to navigate through the JSON structure.
     */
    override suspend fun createIndex(selector: String): Unit =
        mutex.withLock {
            if (hasIndex(selector)) return

            val index = getIndexMap(selector)
            val query = selector.split(".")

            iterateAll()
                .mapNotNull {
                    val id = it.id ?: return@mapNotNull null
                    val selectResult = it.select(query) ?: return@mapNotNull null
                    selectResult to id
                }
                .chunked(100)
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
            indexMap.update(name, listOf(selector)) { it + selector }
        }

    /**
     * Retrieves a JSON object from the persistent collection by its unique identifier.
     *
     * This method decodes the stored JSON representation into a [JsonObject], or returns `null`
     * if no entry with the specified [id] exists in the collection.
     *
     * **NOTE**: the ID property is always named `_id` in the JSON object and is of type `Long`.
     * If no ID is provided during [insert], the ID will be generated automatically and added
     * to the object automatically.
     *
     * @param id The unique identifier of the JSON object to be retrieved.
     * @return The decoded [JsonObject] if found, or `null` if no entry exists with the given `id`.
     */
    public suspend fun findById(id: Long): JsonObject? {
        return json.decodeFromString(
            string = persistentCollection.get(id) ?: return null,
        )
    }

    /**
     * Retrieves and processes elements from the index associated with the specified field selector.
     * For elements matching the provided value, it fetches their detailed JSON object representation.
     *
     * @param selector The string identifying the field in the collection to use for the index lookup.
     * @param value The JSON value to search for in the specified field's index.
     */
    private suspend fun findUsingIndex(
        selector: String,
        value: JsonElement,
    ): Flow<JsonObject>? =
        getIndexOrNull(selector)
            ?.get(value)
            ?.asFlow()
            ?.mapNotNull { findById(it) }

    /**
     * Searches for JSON objects in the collection that match the specified selector and value.
     * If an index exists for the provided selector, it uses the index for optimized lookup.
     * Otherwise, it iterates through all entries in the collection to find matches.
     *
     * @param selector A dot-separated string representing the field to search within the JSON objects.
     * @param value The value of the field to match in the JSON objects.
     * @return A [Flow] of [JsonObject] representing the JSON objects that meet the search criteria.
     */
    public suspend fun find(
        selector: String,
        value: JsonElement,
    ): Flow<JsonObject> =
        findUsingIndex(selector, value)
            ?: iterateAll().filter { it.select(selector) == value }

    /**
     * Removes a JSON object from the collection by its unique identifier.
     *
     * @param id The unique identifier of the JSON object to be removed.
     * @return The removed JSON object as a [JsonObject], or `null` if no object
     *         with the specified ID was found.
     */
    public suspend fun removeById(id: Long): JsonObject? = mutex.withLock { removeByIdUnsafe(id) }

    /**
     * Removes a JSON object from the collection by its unique identifier, **without**
     * locking the collection.
     *
     * The method removes the specified entry from the persistent collection and parses it into a
     * JsonObject. It also updates any associated indices if the object contains indexed fields.
     * If the ID is not present in the collection, the method returns `null`.
     *
     * @param id The unique identifier of the JSON object to be removed.
     * @return The removed JSON object as a [JsonObject], or `null` if no object with the specified ID was found.
     */
    private suspend fun removeByIdUnsafe(id: Long): JsonObject? {
        val jsonString = persistentCollection.remove(id) ?: return null
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject

        indexMap.get(name)
            ?.asSequence()
            ?.forEach { fieldSelector ->
                getIndexOrNull(fieldSelector)
                    ?.update(
                        key = jsonObject.select(fieldSelector) ?: return@forEach,
                        default = emptySet(),
                        updater = { it - id },
                    )
            }
        return jsonObject
    }

    /**
     * Inserts the given JSON object into the collection.
     *
     * If the object does not have an `_id` field, a new unique identifier is generated
     * and added to it before insertion. The `_id` field is always of type `Long`.
     *
     * It is recommended to not provide an `_id` field when inserting a new object and let
     * the method generate it automatically.
     *
     * @param value The JSON object to be inserted into the collection.
     *              It may include a unique `_id` field. If not provided, an ID will be auto-generated.
     * @return The JSON object after insertion, enriched with an `_id` field if it was not initially provided.
     */
    public suspend fun insert(value: JsonObject): JsonObject = mutex.withLock { insertUnsafe(value) }

    /**
     * Inserts the given JSON object into the persistent collection without locking the collection,
     * **without** locking the collection.
     *
     * The method ensures the given JSON object contains a unique identifier (`_id`). If the `_id` field
     * is not present, a new identifier is generated and added to the object.
     * The updated object is then inserted into the persistent collection, and relevant indices
     * (if any exist) are updated to reflect the new addition.
     *
     * @param value The JSON object to be inserted. If it does not contain an `_id` field, one is generated automatically.
     * @return The JSON object after insertion, including the `_id` field if it was not initially provided.
     */
    private suspend fun insertUnsafe(value: JsonObject): JsonObject {
        val id = value.id ?: generateId()
        val jsonObjectWithId = value.copy(id)
        val jsonString = json.encodeToString(jsonObjectWithId)

        persistentCollection.put(id, jsonString)
        indexMap.get(name)
            ?.forEach { fieldSelector ->
                getIndexOrNull(fieldSelector)
                    ?.update(
                        key = jsonObjectWithId.select(fieldSelector) ?: return@forEach,
                        default = setOf(id),
                        updater = { it + id },
                    )
            }

        return jsonObjectWithId
    }

    /**
     * Retrieves the total number of elements in the collection.
     *
     * @return The total count of elements in the collection as a [Long].
     */
    override suspend fun size(): Long = persistentCollection.size()

    /**
     * Removes all elements from the collection, leaving it empty.
     * This operation is performed in a suspendable manner and affects the persistent storage.
     */
    override suspend fun clear(): Unit = persistentCollection.clear()

    /**
     * Retrieves a list of all index selectors present in the collection.
     *
     * @return A list of strings where each string represents an index selector.
     */
    override suspend fun getAllIndexNames(): List<String> = indexMap.get(name) ?: emptyList()

    /**
     * Retrieves an index mapping for the specified JSON selector.
     *
     * @param selector A dot-separated string used to navigate through the JSON structure. Each segment
     *                 specifies a key for a JsonObject or an index (prefixed with '$') for a JsonArray.
     * @return A map where keys are [JsonElement] values corresponding to the selector and values are sets
     *         of document identifiers ([Long]) associated with those JSON values. Returns null if no
     *         index exists for the given selector.
     */
    override suspend fun getIndex(selector: String): Map<JsonElement, Set<Long>>? =
        getIndexOrNull(selector)
            ?.entries()
            ?.toMap()

    /**
     * Drops an index from the collection. See [createIndex] for more information on indexes.
     *
     * @param selector A dot-separated string used to navigate through the JSON structure. Each segment
     *                 specifies a key for a JsonObject or an index (prefixed with '$') for a JsonArray.
     */
    override suspend fun dropIndex(selector: String): Unit =
        mutex.withLock {
            store.deleteMap("$name.$selector")
            indexMap.update(name, emptyList()) { it - selector }
        }

    /**
     * Retrieves detailed information about the collection, including the current state of the
     * ID generator and indexes associated with the collection.
     *
     * @return A [CollectionDetails] object containing metadata such as ID generator state and index details.
     */
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

    /**
     * Updates a JSON object in the collection by its unique identifier.
     *
     * @param id The unique identifier of the entity to update.
     * @param update A suspend function that takes the current state of the entity as a [JsonObject]
     * and returns the updated state as a [JsonObject]. The function is executed within the collection's lock.
     * @return `true` if the object was updated successfully, `false` otherwise.
     */
    public suspend fun updateById(
        id: Long,
        update: suspend (JsonObject) -> JsonObject,
    ): Boolean = mutex.withLock { updateByIdUnsafe(id, update) }

    /**
     * Updates an item in the persistent collection by its ID, applying the specified update function,
     * **without** locking the collection.
     *
     * It also updates any associated indices if the object contains indexed fields.
     *
     * @param id The unique identifier of the item to update.
     * @param update A suspendable function that takes the current JSON object of the item,
     * and returns an updated JSON object or null if no update is to be applied.
     * @return `true` if the update operation was successful, `false` otherwise.
     */
    private suspend fun updateByIdUnsafe(
        id: Long,
        update: suspend (JsonObject) -> JsonObject?,
    ): Boolean {
        val item = persistentCollection.get(id) ?: return false
        val jsonObject = json.decodeFromString<JsonObject>(item)
        val newItem = update(jsonObject)?.copy(id) ?: return false
        insertUnsafe(newItem)
        return true
    }

    /**
     * Updates an object in the collection that matches the specified condition.
     *
     * @param fieldSelector The selector for the field to match in the JSON objects.
     * @param fieldValue The value to match for the specified selector.
     * @param update A suspend function to specify how the matched object should be updated.
     * The function is executed within the collection's lock.
     * @return `true` if the update operation was successful, `false` otherwise.
     */
    public suspend fun updateWhere(
        fieldSelector: String,
        fieldValue: JsonElement,
        update: suspend (JsonObject) -> JsonObject,
    ): Boolean = mutex.withLock { updateWhereUnsafe(fieldSelector, fieldValue, update) }

    /**
     * Updates documents in the collection that match the specified field selector and value,
     * **without** locking the collection.
     *
     * @param fieldSelector The field name to filter documents for updating.
     * @param fieldValue The value to match against the field specified by the fieldSelector.
     * @param update A suspendable transformation function that receives the current document as a JsonObject
     *               and returns an updated JsonObject.
     * @return `true` if at least one document was updated, `false` otherwise.
     */
    private suspend fun JsonCollection.updateWhereUnsafe(
        fieldSelector: String,
        fieldValue: JsonElement,
        update: suspend (JsonObject) -> JsonObject,
    ): Boolean {
        val index = getIndexOrNull(fieldSelector)
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

    /**
     * Updates documents in the collection that match the specified condition.
     *
     * @param fieldSelector The name of the field used to match the documents to be updated.
     * @param fieldValue The value of the field to be matched for the update operation.
     * @param upsert If `true`, inserts the update as a new document if no matching document is found.
     * @param update The JsonObject representing the updates to be applied to the matching documents.
     * @return `true` if at least one document was updated, `false` otherwise.
     */
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

    /**
     * Removes all elements where the value of the specified field selector matches the provided value.
     * See [select] for more information on how to write selectors.
     *
     * @param fieldSelector A string representing the field within the JSON structure used to filter elements.
     *                       Must be a dot-separated string specifying keys for a JsonObject or indices (prefixed with '$') for a JsonArray.
     * @param fieldValue A [JsonElement] value used as the filter to identify which elements to remove.
     * @return `true` if any elements were removed, `false` otherwise.
     */
    override suspend fun removeWhere(
        fieldSelector: String,
        fieldValue: JsonElement,
    ): Boolean =
        mutex.withLock {
            val ids =
                getIndexOrNull(fieldSelector)
                    ?.get(fieldValue)

            if (ids != null) {
                ids.forEach { removeByIdUnsafe(it) }
                return true
            }
            var removed = false
            iterateAll()
                .filter { it.select(fieldSelector) == fieldValue }
                .mapNotNull { it.id }
                .collect { removed = removed || removeByIdUnsafe(it) != null }
            removed
        }
}

/**
 * Updates an object in the collection that matches the specified condition.
 *
 * @param fieldSelector The selector for the field to match in the JSON objects.
 * @param fieldValue The value to match for the specified selector.
 * @param fieldSerializer The serializer for the field value.
 * @param update A suspend function to specify how the matched object should be updated.
 * The function is executed within the collection's lock.
 * @return `true` if the update operation was successful, `false` otherwise.
 */
public suspend inline fun <reified K> JsonCollection.updateWhere(
    fieldSelector: String,
    fieldValue: K,
    fieldSerializer: KSerializer<K>,
    noinline update: suspend (JsonObject) -> JsonObject,
): Boolean =
    updateWhere(
        fieldSelector = fieldSelector,
        fieldValue = json.encodeToJsonElement(fieldSerializer, fieldValue),
        update = update,
    )

/**
 * Updates an object in the collection that matches the specified condition.
 *
 * @param fieldSelector The selector for the field to match in the JSON objects.
 * @param fieldValue The value to match for the specified selector.
 * @param update A suspend function to specify how the matched object should be updated.
 * The function is executed within the collection's lock.
 * @return `true` if the update operation was successful, `false` otherwise.
 */
public suspend inline fun <reified K> JsonCollection.updateWhere(
    fieldSelector: String,
    fieldValue: K,
    noinline update: suspend (JsonObject) -> JsonObject,
): Boolean =
    updateWhere(
        fieldSelector = fieldSelector,
        fieldValue = fieldValue,
        fieldSerializer = json.serializersModule.serializer(),
        update = update,
    )

/**
 * Updates documents in the collection that match the specified condition.
 *
 * @param fieldSelector The name of the field used to match the documents to be updated.
 * @param fieldValue The value of the field to be matched for the update operation.
 * @param fieldSerializer The serializer for the field value.
 * @param upsert If `true`, inserts the update as a new document if no matching document is found.
 * @param update The JsonObject representing the updates to be applied to the matching documents.
 * @return `true` if at least one document was updated, `false` otherwise.
 */
public suspend inline fun <reified K> JsonCollection.updateWhere(
    fieldSelector: String,
    fieldValue: K,
    fieldSerializer: KSerializer<K>,
    upsert: Boolean,
    update: JsonObject,
): Boolean =
    updateWhere(
        fieldSelector = fieldSelector,
        fieldValue = json.encodeToJsonElement(fieldSerializer, fieldValue),
        upsert = upsert,
        update = update,
    )

/**
 * Updates documents in the collection that match the specified condition.
 *
 * @param fieldSelector The name of the field used to match the documents to be updated.
 * @param fieldValue The value of the field to be matched for the update operation.
 * @param upsert If `true`, inserts the update as a new document if no matching document is found.
 * @param update The JsonObject representing the updates to be applied to the matching documents.
 * @return `true` if at least one document was updated, `false` otherwise.
 */
public suspend inline fun <reified K> JsonCollection.updateWhere(
    fieldSelector: String,
    fieldValue: K,
    upsert: Boolean,
    update: JsonObject,
): Boolean =
    updateWhere(
        fieldSelector = fieldSelector,
        fieldValue = fieldValue,
        fieldSerializer = json.serializersModule.serializer(),
        upsert = upsert,
        update = update,
    )

/**
 * Returns a new map containing all key-value pairs from the given iterable of entries.
 *
 * The returned map preserves the entry iteration order of the original iterable.
 * If any of two pairs would have the same key the last one gets added to the map.
 */
private fun <K, V> Iterable<Map.Entry<K, V>>.toMap() = buildMap { this@toMap.forEach { put(it.key, it.value) } }

/**
 * Extension property to retrieve the `id` field as a nullable [Long] from a [JsonObject].
 *
 * The `id` field is accessed using [KotlinxDocumentStore.ID_PROPERTY_NAME] property name
 * constant and is parsed from its JSON representation. Returns `null` if the field does
 * not.
 *
 * @throws [IllegalArgumentException] if the value of the `id` field is not a valid [Long].
 */
private val JsonObject.id: Long?
    get() = get(KotlinxDocumentStore.Companion.ID_PROPERTY_NAME)?.jsonPrimitive?.contentOrNull?.toLong()

/**
 * Creates a copy of the `JsonObject` with the specified ID value.
 *
 * @param id The new ID value to be set in the copied `JsonObject`.
 * @return A new `JsonObject` instance with the updated ID value.
 */
internal fun JsonObject.copy(id: Long) =
    JsonObject(toMutableMap().also { it[KotlinxDocumentStore.Companion.ID_PROPERTY_NAME] = JsonPrimitive(id) })
