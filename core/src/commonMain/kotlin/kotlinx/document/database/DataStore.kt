package kotlinx.document.database

public interface DataStore : AutoCloseable {
    public suspend fun getMap(name: String): PersistentMap<String, String>

    public suspend fun deleteMap(name: String)

    override fun close() {}
}
