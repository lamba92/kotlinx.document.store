package kotlinx.document.database

import kotlinx.serialization.Serializable

@Serializable
data class SimpleEntry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>