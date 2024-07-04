package kotlin.kotlinx.document.database.rocksdb

import kotlinx.io.files.Path
import platform.posix.remove

actual fun Path.deleteIfExists() = remove(toString()) == 0
