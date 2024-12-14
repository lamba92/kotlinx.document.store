package com.github.lamba92.kotlin.document.store.tests.stores.leveldb

import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.wcstr
import kotlinx.io.files.Path
import platform.posix.strerror
import platform.windows.CreateDirectoryW
import platform.windows.FILE_ATTRIBUTE_DIRECTORY
import platform.windows.FOF_NOCONFIRMATION
import platform.windows.FOF_NOERRORUI
import platform.windows.FOF_SILENT
import platform.windows.FO_DELETE
import platform.windows.GetFileAttributesW
import platform.windows.GetLastError
import platform.windows.INVALID_FILE_ATTRIBUTES
import platform.windows.SHFILEOPSTRUCTW
import platform.windows.SHFileOperationW

actual fun deleteFolderRecursively(path: String) {
    memScoped {
        val attributes = GetFileAttributesW(path)
        if (attributes == INVALID_FILE_ATTRIBUTES || attributes and FILE_ATTRIBUTE_DIRECTORY.toUInt() == 0u) {
            return
        }

        // Proceed to delete the folder
        val shFileOp = alloc<SHFILEOPSTRUCTW>()

        shFileOp.hwnd = null
        shFileOp.wFunc = FO_DELETE.toUInt()
        shFileOp.pFrom = path.wcstr.ptr
        shFileOp.pTo = null
        shFileOp.fFlags = (FOF_NOCONFIRMATION or FOF_SILENT or FOF_NOERRORUI).toUShort()

        val result = SHFileOperationW(shFileOp.ptr)
        if (result != 0) {
            val errorMessage = strerror(result)?.toKString() ?: "Unknown error"
            error("Error deleting folder: $errorMessage (code: $result)")
        }
    }
}

actual fun Path.createDirectories(): Path {
    val parts = toString().split("\\")
    var currentPath = ""

    for (part in parts) {
        if (part.isEmpty()) continue // Skip empty parts
        currentPath += if (currentPath.isEmpty()) part else "\\$part"

        // Check if the directory exists
        val attributes = GetFileAttributesW(currentPath)
        val directoryExists = attributes != INVALID_FILE_ATTRIBUTES && (attributes and FILE_ATTRIBUTE_DIRECTORY.convert()) != 0u

        if (!directoryExists) {
            // Create the directory
            if (CreateDirectoryW(currentPath, null) == 0) { // Failed to create directory
                val error = GetLastError()
                error("Failed to create directory: $currentPath, Error: $error")
            }
        }
    }
    return this
}

actual fun Path.resolve(path: String): Path = Path(toString() + "\\" + path)
