package kotlinx.document.store.tests.stores.leveldb

import kotlinx.io.files.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.Path as JavaNioPath
import kotlinx.io.files.Path as KotlinPath

actual fun deleteFolderRecursively(path: String) {
    JavaNioPath(path).deleteRecursively()
}

actual fun KotlinPath.createDirectories(): KotlinPath {
    JavaNioPath(toString()).createDirectories()
    return this
}

actual fun Path.resolve(path: String): Path = KotlinPath(JavaNioPath(toString()).resolve(path).toString())
