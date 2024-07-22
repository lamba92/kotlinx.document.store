package kotlinx.document.database.samples.ktor.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("_id") val id: Long? = null,
    val name: String,
    val age: Int
)

@Serializable
data class Page<T>(
    val content: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Long
)