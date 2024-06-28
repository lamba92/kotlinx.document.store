package com.github.lamba92.kotlin.db

import kotlinx.serialization.json.Json

interface KotlinxDbCollection {
    suspend fun size(): Long
    val name: String
    val json: Json
    suspend fun createIndex(selector: String)
    suspend fun removeById(id: Long)

    suspend fun dropIndex(selector: String)
    suspend fun getAllIndexNames(): List<String>
    suspend fun getIndex(selector: String): Map<String?, Set<Long>>?

    suspend fun clear()

    suspend fun details(): CollectionDetails
}