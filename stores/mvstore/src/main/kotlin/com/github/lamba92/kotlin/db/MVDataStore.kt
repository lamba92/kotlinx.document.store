package com.github.lamba92.kotlin.db

import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.h2.mvstore.MVStore


class MVDataStore(
    private val delegate: MVStore,
) : DataStore, AutoCloseable by delegate {

    companion object {
        fun open(path: Path) = MVStore.open(path.absolutePathString()).asDataStore()
    }

    override suspend fun getMap(
        name: String,
    ): PersistentMap<String, String> = MVPersistentMap(
        delegate = withContext(Dispatchers.IO) { delegate.openMap(name) },
    )

    override suspend fun deleteMap(name: String) {
        withContext(Dispatchers.IO) { delegate.removeMap(name) }
    }

    suspend fun getAllMaps(): Flow<Pair<String, Map<Any, Any>>> = withContext(Dispatchers.IO) {
        delegate.mapNames
            .asFlow()
            .map { it to delegate.openMap(it) }
    }
}

fun MVStore.asDataStore() = MVDataStore(this)
fun MVStore.Builder.openDataStore() = MVDataStore(open())
