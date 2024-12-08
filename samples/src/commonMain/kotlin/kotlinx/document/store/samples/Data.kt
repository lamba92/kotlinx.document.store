@file:OptIn(ExperimentalJsExport::class)

package kotlinx.document.store.samples

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlinx.serialization.Serializable

@Serializable
@JsExport
data class User(
    val name: String,
    val age: Int,
)

@Serializable
@JsExport
data class Page<T>(
    val content: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalElements: Int,
    val totalPages: Int,
)
