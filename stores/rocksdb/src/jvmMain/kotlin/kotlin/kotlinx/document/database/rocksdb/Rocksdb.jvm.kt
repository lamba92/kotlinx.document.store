package kotlin.kotlinx.document.database.rocksdb

import kotlin.io.path.deleteIfExists
import java.nio.file.Path as JavaPath
import kotlin.io.path.Path as JavaPath
import kotlinx.io.files.Path as KotlinxPath

fun KotlinxPath.asJavaPath(): JavaPath = JavaPath(toString())

actual fun KotlinxPath.deleteIfExists() = asJavaPath().deleteIfExists()
