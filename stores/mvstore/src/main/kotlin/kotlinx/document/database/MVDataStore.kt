package kotlinx.document.database

import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.h2.mvstore.MVStore


class MVDataStore(
    private val delegate: MVStore,
) : DataStore {

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

    override fun close() = delegate.close()

}

fun MVStore.asDataStore() = MVDataStore(this)
fun MVStore.Builder.openDataStore() = MVDataStore(open())
