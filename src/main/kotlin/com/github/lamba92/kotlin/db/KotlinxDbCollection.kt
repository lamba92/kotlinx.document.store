package com.github.lamba92.kotlin.db

import kotlinx.serialization.json.Json

interface KotlinxDbCollection {
    val size: Long
    val name: String
    val json: Json
    suspend fun createIndex(selector: String)
    suspend fun removeById(id: Long)
}