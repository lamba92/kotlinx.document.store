package kotlinx.document.store.mvstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.document.store.DataStore
import kotlinx.document.store.PersistentMap
import org.h2.mvstore.MVStore
import kotlin.io.path.absolutePathString
import java.nio.file.Path as JavaNioPath
import kotlin.io.path.Path as JavaNioPath
import kotlinx.io.files.Path as KotlinxIoPath

public class MVDataStore(private val delegate: MVStore) : DataStore {
    public companion object {
        public fun open(path: String): MVDataStore = open(JavaNioPath(path))

        public fun open(path: KotlinxIoPath): MVDataStore = open(path.toString())

        public fun open(path: JavaNioPath): MVDataStore =
            MVStore
                .Builder()
                .fileName(path.absolutePathString())
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
