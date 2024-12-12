@file:OptIn(BetaInteropApi::class)

package kotlinx.document.store.tests.stores.leveldb

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.io.files.Path
import platform.Foundation.NSError
import platform.Foundation.NSFileManager

actual fun deleteFolderRecursively(path: String): Unit =
    memScoped {
        val fileManager = NSFileManager.defaultManager
        val isDirectoryBoolean = alloc<BooleanVar>()
        isDirectoryBoolean.value = true
        if (!fileManager.fileExistsAtPath(path, isDirectoryBoolean.ptr)) {
            return
        }
        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
        val success = fileManager.removeItemAtPath(path, error = errorPtr.ptr)
        val nsError = errorPtr.value
        if (!success && nsError != null) {
            error("Error deleting folder: ${nsError.localizedDescription}")
        }
    }

actual fun Path.createDirectories(): Path {
    memScoped {
        val fileManager = NSFileManager.defaultManager()
        val errorPtr = alloc<ObjCObjectVar<NSError?>>()

        fileManager.createDirectoryAtPath(
            path = this@createDirectories.toString(),
            withIntermediateDirectories = true,
            attributes = null,
            error = errorPtr.ptr,
        )

        val nsError = errorPtr.value
        if (nsError != null) error(nsError.localizedDescription)
    }
    return this
}

actual fun Path.resolve(path: String): Path = Path(toString() + "/" + path)
