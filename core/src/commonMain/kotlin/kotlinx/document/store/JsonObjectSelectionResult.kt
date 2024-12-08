package kotlinx.document.store

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

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

public fun JsonObject.select(query: String): JsonElement? = select(query.split("."))
