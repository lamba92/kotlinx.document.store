package com.github.lamba92.kotlin.document.store.stores.mvstore

import com.github.lamba92.kotlin.document.store.core.DataStore
import com.github.lamba92.kotlin.document.store.core.PersistentMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.h2.mvstore.MVStore
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createParentDirectories

/**
 * Implementation of the [DataStore] using [MVStore] from H2 Database.
 *
 * This class provides persistent and thread-safe storage for handling named maps.
 * Each map is represented by a [PersistentMap] and stored within the underlying `MVStore` instance.
 *
 * The `MVDataStore` class supports creation, retrieval, and deletion of named persistent maps
 * and leverages coroutines for suspendable operations to maintain thread safety.
 */
public class MVDataStore(private val delegate: MVStore) : DataStore {
    public companion object {
        /**
         * Opens an `MVDataStore` at the specified string path. Intermediary directories
         * are created if they do not exist. The database will be created at the specified path as
         * a file.
         *
         * @param path A string representing the path to the `MVStore` file.
         * @return A new instance of [MVDataStore] backed by the file at the specified path.
         */
        public fun open(path: String): MVDataStore = open(Path(path))

        /**
         * Opens an `MVDataStore` at the specified string path. Intermediary directories
         * are created if they do not exist. The database will be created at the specified path as
         * a file.
         *
         * @param path A [Path] representing the path to the `MVStore` file.
         * @return A new instance of [MVDataStore] backed by the file at the specified path.
         */
        public fun open(path: Path): MVDataStore =
            MVStore
                .Builder()
                .fileName(path.createParentDirectories().absolutePathString())
                .open()
                .let { MVDataStore(it) }
    }

    override suspend fun getMap(name: String): PersistentMap<String, String> =
        MVPersistentMap(delegate = withContext(Dispatchers.IO) { delegate.openMap(name) })

    override suspend fun deleteMap(name: String) {
        withContext(Dispatchers.IO) { delegate.removeMap(name) }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            delegate.close()
        }
    }
}
