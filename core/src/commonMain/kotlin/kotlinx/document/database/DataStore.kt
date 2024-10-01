package kotlinx.document.database

import kotlinx.serialization.Serializable
import kotlin.time.Duration

public interface DataStore {
    public val commitStrategy: CommitStrategy

    @Serializable
    public sealed interface CommitStrategy {
        @Serializable
        public data class Periodic(val interval: Duration) : CommitStrategy

        @Serializable
        public data object OnChange : CommitStrategy
    }

    public suspend fun getMap(name: String): PersistentMap<String, String>

    public suspend fun deleteMap(name: String)

    public suspend fun close()

    public suspend fun commit()
}
