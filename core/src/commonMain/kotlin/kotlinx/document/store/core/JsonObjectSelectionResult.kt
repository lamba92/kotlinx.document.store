package kotlinx.document.store.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Traverses a JSON object or array using a sequence of keys or array indices from the query list
 * and retrieves the corresponding JSON element. If any key or index is not found or invalid,
 * the function returns `null`.
 *
 * ```kotlin
 * val json = Json.decodeFromString<JsonObject>("""
 * {
 *    "key": "value",
 *    "array": [1, 2, 3]
 * }
 *    """.trimIndent())
 * json.select("key") // Returns a JsonPrimitive("value")
 * json.select("array.$1") // Returns a JsonPrimitive(2)
 * json.select("array.$5") // Returns null
 * ```
 *
 * @param query A dot separated string where each string specifies a key for a JsonObject or
 *              an index (prefixed with '$') for a JsonArray to navigate through the JSON structure.
 * @return The desired JsonElement if found, or null if the key or index does not exist in the JSON structure.
 */
public fun JsonObject.select(query: String): JsonElement? = select(query.split("."))

/**
 * Traverses a JSON object or array using a sequence of keys or array indices from the query list
 * and retrieves the corresponding JSON element. If any key or index is not found or invalid,
 * the function returns `null`.
 *
 * Examples:
 *
 * ```kotlin
 * val json = Json.decodeFromString<JsonObject>("""
 * {
 *    "key": "value",
 *    "array": [1, 2, 3]
 * }
 *    """.trimIndent())
 * json.select(listOf("key")) // Returns a JsonPrimitive("value")
 * json.select(listOf("array", "$1")) // Returns a JsonPrimitive(2)
 * json.select(listOf("array", "$5")) // Returns null
 * ```
 *
 * @param query A list of strings where each string specifies a key for a JsonObject or
 *              an index (prefixed with '$') for a JsonArray to navigate through the JSON structure.
 * @return The desired JsonElement if found, or null if the key or index does not exist in the JSON structure.
 */
public fun JsonObject.select(query: List<String>): JsonElement? {
    val queue = query.toMutableList()
    var currentElement: JsonElement = this

    while (queue.isNotEmpty()) {
        val currentSegment: String = queue.removeFirst()
        when {
            currentSegment.startsWith("$") && currentElement is JsonArray -> {
                val elementAtIndex = currentSegment.removePrefix("$").toInt()
                currentElement = currentElement.getOrNull(elementAtIndex) ?: return null
            }
            currentElement is JsonObject -> currentElement = currentElement[currentSegment] ?: return null
            else -> return null
        }
    }

    return currentElement
}
