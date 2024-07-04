package kotlinx.document.database

import kotlinx.serialization.Serializable

@Serializable
public data class SerializableEntry<K, V>(
    override val key: K,
    override val value: V
) : Map.Entry<K, V>
