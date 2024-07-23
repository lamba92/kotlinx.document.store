package kotlinx.document.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

public class ObjectCollection<T : Any>(
    private val serializer: KSerializer<T>,
    public val jsonCollection: JsonCollection,
) : KotlinxDatabaseCollection by jsonCollection {
    public suspend fun find(
        selector: String,
        value: JsonPrimitive,
    ): Flow<T> =
        jsonCollection.find(selector, value)
            .map { json.decodeFromJsonElement(serializer, it) }

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

    public suspend fun removeById(id: Long): T? =
        jsonCollection.removeById(id)
            ?.let { json.decodeFromJsonElement(serializer, it) }

    public suspend fun findById(id: Long): T? =
        jsonCollection.findById(id)
            ?.let { json.decodeFromJsonElement(serializer, it) }

    public fun iterateAll(fromIndex: Long = 0L): Flow<T> =
        jsonCollection.iterateAll(fromIndex)
            .map { json.decodeFromJsonElement(serializer, it) }

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

    public suspend fun updateWhere(
        filedSelector: String,
        fieldValue: JsonElement,
        update: suspend (T) -> T,
    ): Boolean =
        jsonCollection.updateWhere(filedSelector, fieldValue) {
            val decodeFromJsonElement = json.decodeFromJsonElement(serializer, it)
            val newItem = update(decodeFromJsonElement)
            json.encodeToJsonElement(serializer, newItem).jsonObject
        }

    public suspend fun updateWhere(
        fieldSelector: String,
        fieldValue: JsonPrimitive,
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
