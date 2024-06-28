package com.github.lamba92.kotlin.db

interface DataStore : AutoCloseable {

    suspend fun getMap(name: String): PersistentMap<String, String>
    suspend fun deleteMap(name: String)

}