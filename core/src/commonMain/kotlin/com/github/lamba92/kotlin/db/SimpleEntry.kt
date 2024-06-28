package com.github.lamba92.kotlin.db

import kotlinx.serialization.Serializable

@Serializable
data class SimpleEntry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>