package kotlinx.document.database

import kotlinx.serialization.json.Json

public interface KotlinxDatabaseCollection {
    public suspend fun size(): Long

    public val name: String
    public val json: Json

    public suspend fun createIndex(selector: String)

    public suspend fun removeById(id: Long)

    public suspend fun dropIndex(selector: String)

    public suspend fun getAllIndexNames(): List<String>

    public suspend fun getIndex(selector: String): Map<String?, Set<Long>>?

    public suspend fun clear()

    public suspend fun details(): CollectionDetails
}
