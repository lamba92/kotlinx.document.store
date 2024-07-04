package kotlinx.document.database.mvstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.document.database.DataStore
import kotlinx.document.database.PersistentMap
import org.h2.mvstore.MVStore
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class MVDataStore(
    private val delegate: MVStore,
) : DataStore {
    companion object {
        fun open(path: Path) = MVStore.open(path.absolutePathString()).asDataStore()

        fun open(path: String) = open(Path(path))
    }

    override suspend fun getMap(name: String): PersistentMap<String, String> =
        MVPersistentMap(
            delegate = withContext(Dispatchers.IO) { delegate.openMap(name) },
        )

    override suspend fun deleteMap(name: String) {
        withContext(Dispatchers.IO) { delegate.removeMap(name) }
    }

    override fun close() = delegate.close()
}

fun MVStore.asDataStore() = MVDataStore(this)

fun MVStore.Builder.openDataStore() = MVDataStore(open())
