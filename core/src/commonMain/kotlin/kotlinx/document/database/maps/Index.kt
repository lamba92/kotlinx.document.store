package kotlinx.document.database.maps

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.document.database.PersistentMap
import kotlinx.document.database.SerializableEntry
import kotlinx.document.database.UpdateResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

private fun String.split() = Json.decodeFromString<Set<Long>>(this)

private fun Set<Long>.join() = Json.encodeToString(this)

public fun PersistentMap<String, String>.asIndex(): Index = Index(this)

public class Index(
    private val delegate: PersistentMap<String, String>,
) : PersistentMap<JsonElement, Set<Long>> {
    public companion object {
        private val json =
            Json {
                encodeDefaults = false
                prettyPrint = false
            }

        private fun JsonElement.asString(): String =
            when (this) {
                is JsonPrimitive -> toString()
                else -> json.encodeToString(this)
            }
    }

    override suspend fun get(key: JsonElement): Set<Long>? = delegate.get(key.asString())?.split()

    override suspend fun put(
        key: JsonElement,
        value: Set<Long>,
    ): Set<Long>? =
        delegate.put(key.asString(), value.join())
            ?.split()

    override suspend fun remove(key: JsonElement): Set<Long>? = delegate.remove(key.asString())?.split()

    override suspend fun containsKey(key: JsonElement): Boolean = delegate.containsKey(key.asString())

    override suspend fun clear(): Unit = delegate.clear()

    override suspend fun size(): Long = delegate.size()

    override suspend fun isEmpty(): Boolean = delegate.isEmpty()

    override fun entries(fromIndex: Long): Flow<Map.Entry<JsonElement, Set<Long>>> =
        delegate.entries(fromIndex)
            .map { SerializableEntry(json.decodeFromString(it.key), it.value.split()) }

    override fun close() {
        delegate.close()
    }

    override suspend fun getOrPut(
        key: JsonElement,
        defaultValue: () -> Set<Long>,
    ): Set<Long> =
        delegate.getOrPut(
            key = key.asString(),
            defaultValue = { defaultValue().join() },
        ).split()

    override suspend fun update(
        key: JsonElement,
        value: Set<Long>,
        updater: (Set<Long>) -> Set<Long>,
    ): UpdateResult<Set<Long>> =
        delegate.update(
            key = key.asString(),
            value = value.join(),
            updater = { updater(it.split()).join() },
        ).let { UpdateResult(it.oldValue?.split(), it.newValue.split()) }
}
