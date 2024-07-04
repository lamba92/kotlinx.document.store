package kotlinx.document.database

interface DataStore : AutoCloseable {
    suspend fun getMap(name: String): PersistentMap<String, String>

    suspend fun deleteMap(name: String)

    override fun close() {}
}
