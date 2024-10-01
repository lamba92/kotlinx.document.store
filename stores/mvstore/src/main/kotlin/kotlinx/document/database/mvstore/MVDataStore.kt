package kotlinx.document.database.mvstore

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.document.database.DataStore
import kotlinx.document.database.PersistentMap
import org.h2.mvstore.MVStore
import java.nio.file.Path
import kotlin.io.path.absolutePathString

public class MVDataStore private constructor(
    internal val delegate: MVStore,
    override val commitStrategy: DataStore.CommitStrategy,
) : DataStore {
    private val scope by lazy { CoroutineScope(SupervisorJob()) }

    init {
        if (commitStrategy is DataStore.CommitStrategy.Periodic) {
            scope.launch {
                while (true) {
                    delay(commitStrategy.interval)
                    commit()
                }
            }
        }
    }

    public companion object {
        public fun open(
            path: Path,
            commitStrategy: DataStore.CommitStrategy,
        ): MVDataStore =
            MVStore
                .Builder()
                .fileName(path.absolutePathString())
                .autoCommitDisabled()
                .open()
                .let {
                    MVDataStore(
                        delegate = it,
                        commitStrategy = commitStrategy,
                    )
                }
    }

    override suspend fun getMap(name: String): PersistentMap<String, String> =
        MVPersistentMap(
            delegate = withContext(Dispatchers.IO) { delegate.openMap(name) },
            commitFunction = if (commitStrategy is DataStore.CommitStrategy.OnChange) ::commit else null,
        )

    override suspend fun deleteMap(name: String) {
        withContext(Dispatchers.IO) { delegate.removeMap(name) }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            delegate.close()
        }
        if (commitStrategy is DataStore.CommitStrategy.Periodic) {
            scope.cancel()
        }
    }

    override suspend fun commit() {
        withContext(Dispatchers.IO) {
            delegate.commit()
            delegate.sync()
        }
    }
}
