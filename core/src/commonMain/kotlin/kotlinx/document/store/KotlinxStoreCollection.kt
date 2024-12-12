package kotlinx.document.store

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

/**
 * A collection of elements saved in a [DataStore]. This interface provides
 * various operations to manage the lifecycle and contents of a collection, such as
 * creating and dropping indexes, fetching collection details, and performing basic
 * CRUD operations.
 */
public interface KotlinxStoreCollection {
    /**
     * Retrieves the total number of elements in the collection.
     *
     * @return The total count of elements in the collection as a [Long].
     */
    public suspend fun size(): Long

    /**
     * The name of the collection in the [DataStore].
     * This property uniquely identifies a collection and is used for performing
     * operations such as indexing, querying, or managing its lifecycle.
     */
    public val name: String

    /**
     * The [Json] instance used to serialize and deserialize JSON data.
     */
    public val json: Json

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
    public suspend fun createIndex(selector: String)

    /**
     * Drops an index from the collection. See [createIndex] for more information on indexes.
     *
     * @param selector A dot-separated string used to navigate through the JSON structure. Each segment
     *                 specifies a key for a JsonObject or an index (prefixed with '$') for a JsonArray.
     */
    public suspend fun dropIndex(selector: String)

    /**
     * Retrieves a list of all index selectors present in the collection.
     *
     * @return A list of strings where each string represents an index selector.
     */
    public suspend fun getAllIndexNames(): List<String>

    /**
     * Retrieves an index mapping for the specified JSON selector.
     *
     * @param selector A dot-separated string used to navigate through the JSON structure. Each segment
     *                 specifies a key for a JsonObject or an index (prefixed with '$') for a JsonArray.
     * @return A map where keys are [JsonElement] values corresponding to the selector and values are sets
     *         of document identifiers ([Long]) associated with those JSON values. Returns null if no
     *         index exists for the given selector.
     */
    public suspend fun getIndex(selector: String): Map<JsonElement, Set<Long>>?

    /**
     * Removes all elements from the collection, leaving it empty.
     * This operation is performed in a suspendable manner and affects the persistent storage.
     */
    public suspend fun clear()

    /**
     * Retrieves detailed information about the collection, including the current state of the
     * ID generator and indexes associated with the collection.
     *
     * @return A [CollectionDetails] object containing metadata such as ID generator state and index details.
     */
    public suspend fun details(): CollectionDetails

    /**
     * Removes all elements where the value of the specified field selector matches the provided value.
     * See [select] for more information on how to write selectors.
     *
     * @param fieldSelector A string representing the field within the JSON structure used to filter elements.
     *                       Must be a dot-separated string specifying keys for a JsonObject or indices (prefixed with '$') for a JsonArray.
     * @param fieldValue A [JsonElement] value used as the filter to identify which elements to remove.
     * @return `true` if any elements were removed, `false` otherwise.
     */
    public suspend fun removeWhere(
        fieldSelector: String,
        fieldValue: JsonElement,
    ): Boolean
}

/**
 * Removes all elements where the value of the specified field selector matches the provided value.
 * See [select] for more information on how to write selectors.
 *
 * @param fieldSelector A string representing the field within the JSON structure used to filter elements.
 *                       Must be a dot-separated string specifying keys for a JsonObject or indices (prefixed with '$') for a JsonArray.
 * @param fieldSerializer Serializer for the field value.
 * @param fieldValue An object used as the filter to identify which elements to remove.
 * @return `true` if any elements were removed, `false` otherwise.
 */
public suspend inline fun <reified K> KotlinxStoreCollection.removeWhere(
    fieldSelector: String,
    fieldValue: K,
    fieldSerializer: KSerializer<K>,
): Boolean =
    removeWhere(
        fieldSelector = fieldSelector,
        fieldValue = json.encodeToJsonElement(fieldSerializer, fieldValue),
    )

/**
 * Removes all elements where the value of the specified field selector matches the provided value.
 * See [select] for more information on how to write selectors.
 *
 * @param fieldSelector A string representing the field within the JSON structure used to filter elements.
 *                       Must be a dot-separated string specifying keys for a JsonObject or indices (prefixed with '$') for a JsonArray.
 * @param fieldValue An object used as the filter to identify which elements to remove.
 * @return `true` if any elements were removed, `false` otherwise.
 */
public suspend inline fun <reified K> KotlinxStoreCollection.removeWhere(
    fieldSelector: String,
    fieldValue: K,
): Boolean =
    removeWhere(
        fieldSelector = fieldSelector,
        fieldValue = fieldValue,
        fieldSerializer = json.serializersModule.serializer(),
    )
