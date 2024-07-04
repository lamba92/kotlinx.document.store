package kotlinx.document.database.mvstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.document.database.DataStore
import kotlinx.document.database.PersistentMap
import org.h2.mvstore.MVStore
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

public class MVDataStore(
    private val delegate: MVStore,
) : DataStore {
    public companion object {
        public fun open(path: Path): MVDataStore = MVStore.open(path.absolutePathString()).asDataStore()

        public fun open(path: String): MVDataStore = open(Path(path))
    }

    override suspend fun getMap(name: String): PersistentMap<String, String> =
        MVPersistentMap(
            delegate = withContext(Dispatchers.IO) { delegate.openMap(name) },
        )

    override suspend fun deleteMap(name: String) {
        withContext(Dispatchers.IO) { delegate.removeMap(name) }
    }

    override fun close(): Unit = delegate.close()
}

public fun MVStore.asDataStore(): MVDataStore = MVDataStore(this)

public fun MVStore.Builder.openDataStore(): MVDataStore = MVDataStore(open())
