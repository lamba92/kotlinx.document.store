package com.github.lamba92.kotlin.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class ObjectCollection<T : Any>(
    private val serializer: KSerializer<T>,
    val jsonCollection: JsonCollection
) : KotlinxDbCollection by jsonCollection {

    suspend fun find(selector: String, value: String?): Flow<T> = jsonCollection.find(selector, value)
        .map { json.decodeFromJsonElement(serializer, it) }

    suspend fun insert(value: T) {
        val jsonObject = json.encodeToJsonElement(serializer, value)
        if (jsonObject !is JsonObject) {
            val s = when (jsonObject) {
                is JsonArray -> "an array-like object"
                is JsonPrimitive -> "a primitive"
                JsonNull -> "null"
                else -> "an unknown type"
            }
            error("Expected an object but got $s")
        }
        jsonCollection.insert(jsonObject)
    }

    suspend fun findById(id: Long): T? =
        jsonCollection.findById(id)
            ?.let { json.decodeFromJsonElement(serializer, it) }

    fun iterateAll() = jsonCollection.iterateAll()
        .map { json.decodeFromJsonElement(serializer, it) }

}


