package com.github.lamba92.kotlin.db

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * This function retrieves a value from a JSON object by following a list of segment strings.
 *
 * Each segment specifies the key in the current JSON object to follow, or if the current JSON object is an array,
 * specifies the index of the desired element by prefixing it with `$`.
 *
 * If any segment leads to a nonexistent key or array index in the JSON object, `NotFound` is returned.
 * If a segment leads to a `null` value in the JSON object, `Null` is returned.
 * Otherwise, if a segment leads to a primitive value in the JSON object, `Found` with the string content of the primitive
 * is returned.
 *
 * Here are some examples of how this function might be used:
 *
 * ```kotlin
 * val json = JsonObject(mapOf(
 *     "name" to JsonPrimitive("John"),
 *     "address" to JsonObject(mapOf(
 *         "street" to JsonPrimitive("Some Street"),
 *         "city" to JsonPrimitive("Some City")
 *     )),
 *     "phones" to JsonArray(listOf(JsonPrimitive("123"), JsonPrimitive("456")))
 * ))
 *
 * json.getValueFromSegments(listOf("name")) // Returns Found("John")
 * json.getValueFromSegments(listOf("address", "street")) // Returns Found("Some Street")
 * json.getValueFromSegments(listOf("address", "zip")) // Returns NotFound
 * json.getValueFromSegments(listOf("phones", "$1")) // Returns Found("456")
 * ```
 *
 * But also you can use it with a query string:
 *
 * ```kotlin
 * val jsonString = """
 * {
 *     "name": "John",
 *     "address": {
 *         "street": "Some Street",
 *         "city": "Some City"
 *     },
 *     "phones": ["123", "456"]
 * }
 * """
 * val json = parseJsonObject(jsonString)
 *
 * json.getValueFromSegments("name".split(".")) // Returns Found("John")
 * json.getValueFromSegments("address.street".split(".")) // Returns Found("Some Street")
 * json.getValueFromSegments("address.zip".split(".")) // Returns NotFound
 * json.getValueFromSegments("phones.$1".split(".")) // Returns Found("456")
 * ```
 *
 * @param [query] The list of strings defining the path to the desired value.
 * @return A [JsonObjectSelectionResult] representing the result of following the segments path:
 * - [JsonObjectSelectionResult].[Found](content: String) if the last valid key or index points to a primitive Json value.
 * The content of the found value is included in the `Found` result.
 * - [JsonObjectSelectionResult].[NotFound] if any segment does not correspond to a key in the JsonObject or an index in a JsonArray.
 * - [JsonObjectSelectionResult].[Null] if the last valid key or index points to a JsonNull.
 *
 */
fun JsonObject.select(query: List<String>): JsonObjectSelectionResult {

    val queue = query.toMutableList()
    var currentElement: JsonElement = this

    while (queue.isNotEmpty()) {
        val currentSegment: String = queue.removeFirst()
        when {
            currentSegment.startsWith("$") && currentElement is JsonArray -> {
                val index = currentSegment.removePrefix("$").toInt()
                currentElement = currentElement.getOrNull(index) ?: return JsonObjectSelectionResult.NotFound
            }

            currentElement is JsonObject -> currentElement = currentElement[currentSegment]
                ?: return JsonObjectSelectionResult.NotFound

            else -> return JsonObjectSelectionResult.NotFound
        }
    }

    return when (currentElement) {
        JsonNull -> JsonObjectSelectionResult.Null
        is JsonPrimitive -> JsonObjectSelectionResult.Found(currentElement.content)
        else -> JsonObjectSelectionResult.NotFound
    }
}

fun JsonObject.select(query: String): JsonObjectSelectionResult =
    select(query.split("."))

/**
 * `JsonObjectSelectionResult` is a sealed interface that represents the possible outcomes
 * when attempting to retrieve a value from a `JsonObject` using the `getValueFromSegments` function.
 *
 * It has three possible implementations representing three distinct outcomes:
 *
 * - `Found`: represents a successful retrieval of a primitive value from the `JsonObject`.
 *   Its `value` property contains the String representation of the found value.
 *
 * - `NotFound`: represents an unsuccessful retrieval attempt, generally because
 *   a particular segment doesn't match any key in the `JsonObject` or an index in a `JsonArray`.
 *
 * - `Null`: represents a situation where the last valid key or index points to a `JsonNull`.
 *
 * @see [select]
 */
sealed interface JsonObjectSelectionResult {
    /**
     * `Found` represents a successful retrieval of a value from the JSON object.
     * The `value` property contains the retrieved value as a string.
     */
    data class Found(val value: String) : JsonObjectSelectionResult

    /**
     * `NotFound` represents an unsuccessful retrieval attempt from the `JsonObject`,
     * indicating that a segment does not correspond to a key in the JsonObject or an index in a `JsonArray`.
     */
    data object NotFound : JsonObjectSelectionResult

    /**
     * `Null` represents a situation where the desired segment in the `JsonObject` or `JsonArray` is `null`.
     */
    data object Null : JsonObjectSelectionResult
}