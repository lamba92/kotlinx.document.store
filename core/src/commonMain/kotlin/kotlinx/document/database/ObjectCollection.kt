package kotlinx.document.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

public class ObjectCollection<T : Any>(
    private val serializer: KSerializer<T>,
    public val jsonCollection: JsonCollection,
) : KotlinxDatabaseCollection by jsonCollection {
    public suspend fun find(
        selector: String,
        value: String?,
    ): Flow<T> =
        jsonCollection.find(selector, value)
            .map { json.decodeFromJsonElement(serializer, it) }

    public suspend fun insert(value: T) {
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
        jsonCollection.insert(jsonObject)
    }

    public suspend fun findById(id: Long): T? =
        jsonCollection.findById(id)
            ?.let { json.decodeFromJsonElement(serializer, it) }

    public fun iterateAll(): Flow<T> =
        jsonCollection.iterateAll()
            .map { json.decodeFromJsonElement(serializer, it) }
}
