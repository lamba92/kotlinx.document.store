package com.github.lamba92.kotlin.document.store.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer

/**
 * `ObjectCollection` provides an abstraction for managing a collection of @[Serializable] objects
 * using [JsonCollection] as a backing data store.
 *
 * @param T The type of objects stored in the collection. Must be a non-null type.
 * @param serializer The serializer used for encoding and decoding objects of type `T`.
 * @param jsonCollection The underlying JSON-based collection for storing serialized objects.
 */
public class ObjectCollection<T : Any>(
    private val serializer: KSerializer<T>,
    public val jsonCollection: JsonCollection,
) : KotlinDocumentStoreCollection by jsonCollection {
    /**
     * Searches for objects in the collection that match the specified selector and value.
     * If an index exists for the provided selector, it uses the index for optimized lookup.
     * Otherwise, it iterates through all entries in the collection to find matches.
     *
     * @param selector A dot-separated string representing the field to search within the JSON objects.
     * @param value The value of the field to match in the JSON objects.
     * @param valueSerializer The serializer for the value type [K].
     * @return A [Flow]<[T]> representing the JSON objects that meet the search criteria.
     */
    public suspend fun <K> find(
        selector: String,
        value: K,
        valueSerializer: KSerializer<K>,
    ): Flow<T> =
        jsonCollection.find(selector, json.encodeToJsonElement(valueSerializer, value))
            .map { json.decodeFromJsonElement(serializer, it) }

    /**
     * Inserts the given [value] into the collection.
     *
     * If the object does not have an `_id` field, a new unique identifier is generated
     * and added to it before insertion. The `_id` field is always of type `Long`.
     *
     * When deserializing the object, if the `_id` field is not present in the class
     * definition, it is ignored.
     *
     * It is recommended to not provide or declare in the class an `_id` field when inserting a new object and let
     * the method generate it automatically.
     *
     * The annotation @[SerialName] can be used to map the `_id` to a different name in the class definition.
     * For example:
     * ```kotlin
     * @Serializable
     * data class User(
     *    @SerialName("_id") val id: Long? = null,
     *    val name: String
     * )
     *```
     *
     * @param value The JSON object to be inserted into the collection.
     *              It may include a unique `_id` field. If not provided, an ID will be auto-generated.
     * @return The JSON object after insertion, enriched with an `_id` field if it was not initially provided.
     */
    public suspend fun insert(value: T): T {
        val jsonObject = json.encodeToJsonElement(serializer, value)
        if (jsonObject !is JsonObject) {
            val s =
                when (jsonObject) {
                    is JsonArray -> "an array-like object"
                    is JsonPrimitive -> "a primitive"
                    JsonNull -> "null"
                    else -> "an unknown type"
                }
            error("Expected an object but got $s")
        }
        return json.decodeFromJsonElement(serializer, jsonCollection.insert(jsonObject))
    }

    /**
     * Removes an object from the collection by its unique identifier.
     *
     * @param id The unique identifier of the object to be removed.
     * @return The removed object as a [T], or `null` if no object
     *         with the specified ID was found.
     */
    public suspend fun removeById(id: Long): T? =
        jsonCollection.removeById(id)
            ?.let { json.decodeFromJsonElement(serializer, it) }

    /**
     * Retrieves an object from the persistent collection by its unique identifier.
     *
     * This method decodes the stored JSON representation into a [T], or returns `null`
     * if no entry with the specified [id] exists in the collection.
     *
     * **NOTE**: the ID property is always named `_id` in the JSON object and is of type `Long`.
     * If no ID is provided during [insert], the ID will be generated automatically and added
     * to the object automatically.
     *
     * @param id The unique identifier of the JSON object to be retrieved.
     * @return The decoded [JsonObject] if found, or `null` if no entry exists with the given `id`.
     */
    public suspend fun findById(id: Long): T? =
        jsonCollection.findById(id)
            ?.let { json.decodeFromJsonElement(serializer, it) }

    /**
     * Iterates through all entries in the persistent collection
     * @return A [Flow]<[T]> of all entries in the collection.
     */
    public fun iterateAll(): Flow<T> =
        jsonCollection.iterateAll()
            .map { json.decodeFromJsonElement(serializer, it) }

    /**
     * Updates an object in the collection by its unique identifier.
     *
     * @param id The unique identifier of the entity to update.
     * @param update A suspend function that takes the current state of the entity as a [T]
     * and returns the updated state as a [T]. The function is executed within the collection's lock.
     * @return `true` if the object was updated successfully, `false` otherwise.
     */
    public suspend fun updateById(
        id: Long,
        update: suspend (T) -> T,
    ) {
        jsonCollection.updateById(id) {
            val decodeFromJsonElement = json.decodeFromJsonElement(serializer, it)
            val newItem = update(decodeFromJsonElement)
            json.encodeToJsonElement(serializer, newItem).jsonObject
        }
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
        update: suspend (T) -> T,
    ): Boolean =
        jsonCollection.updateWhere(fieldSelector, fieldValue) {
            val decodeFromJsonElement = json.decodeFromJsonElement(serializer, it)
            val newItem = update(decodeFromJsonElement)
            json.encodeToJsonElement(serializer, newItem).jsonObject
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
        update: T,
    ): Boolean =
        jsonCollection.updateWhere(
            fieldSelector = fieldSelector,
            fieldValue = fieldValue,
            upsert = upsert,
            update = json.encodeToJsonElement(serializer, update).jsonObject,
        )
}

/**
 * Searches for objects in the collection that match the specified selector and value.
 * If an index exists for the provided selector, it uses the index for optimized lookup.
 * Otherwise, it iterates through all entries in the collection to find matches.
 *
 * @param selector A dot-separated string representing the field to search within the JSON objects.
 * @param value The value of the field to match in the JSON objects.
 * @return A [Flow]<[T]> representing the JSON objects that meet the search criteria.
 */
public suspend inline fun <reified K, T : Any> ObjectCollection<T>.find(
    selector: String,
    value: K,
): Flow<T> = find(selector, value, json.serializersModule.serializer<K>())

/**
 * Updates documents in the collection that match the specified condition.
 *
 * @param fieldSelector The name of the field used to match the documents to be updated.
 * @param fieldValue The value of the field to be matched for the update operation.
 * @param fieldValueSerializer The serializer for the value type [K].
 * @param upsert If `true`, inserts the update as a new document if no matching document is found.
 * @param update The JsonObject representing the updates to be applied to the matching documents.
 * @return `true` if at least one document was updated, `false` otherwise.
 */
public suspend inline fun <reified K, T : Any> ObjectCollection<T>.updateWhere(
    fieldSelector: String,
    fieldValue: K,
    fieldValueSerializer: KSerializer<K>,
    upsert: Boolean,
    update: T,
): Boolean =
    updateWhere(
        fieldSelector = fieldSelector,
        fieldValue = json.encodeToJsonElement(fieldValueSerializer, fieldValue),
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
public suspend inline fun <reified K, T : Any> ObjectCollection<T>.updateWhere(
    fieldSelector: String,
    fieldValue: K,
    upsert: Boolean,
    update: T,
): Boolean =
    updateWhere(
        fieldSelector = fieldSelector,
        fieldValue = fieldValue,
        fieldValueSerializer = json.serializersModule.serializer(),
        upsert = upsert,
        update = update,
    )

/**
 * Updates an object in the collection that matches the specified condition.
 *
 * @param fieldSelector The selector for the field to match in the JSON objects.
 * @param fieldValue The value to match for the specified selector.
 * @param fieldValueSerializer The serializer for the value type [K].
 * @param update A suspend function to specify how the matched object should be updated.
 * The function is executed within the collection's lock.
 * @return `true` if the update operation was successful, `false` otherwise.
 */
public suspend inline fun <reified K, T : Any> ObjectCollection<T>.updateWhere(
    fieldSelector: String,
    fieldValue: K,
    fieldValueSerializer: KSerializer<K>,
    noinline update: suspend (T) -> T,
): Boolean =
    updateWhere(
        fieldSelector = fieldSelector,
        fieldValue = json.encodeToJsonElement(fieldValueSerializer, fieldValue),
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
public suspend inline fun <reified K, T : Any> ObjectCollection<T>.updateWhere(
    fieldSelector: String,
    fieldValue: K,
    noinline update: suspend (T) -> T,
): Boolean =
    updateWhere(
        fieldSelector = fieldSelector,
        fieldValue = fieldValue,
        fieldValueSerializer = json.serializersModule.serializer(),
        update = update,
    )
