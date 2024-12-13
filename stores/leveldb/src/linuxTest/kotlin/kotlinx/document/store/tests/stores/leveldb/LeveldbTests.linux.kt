package kotlinx.document.store.tests.stores.leveldb

import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.io.files.Path
import platform.posix.S_IFDIR
import platform.posix.S_IFMT
import platform.posix.S_IROTH
import platform.posix.S_IRWXG
import platform.posix.S_IRWXU
import platform.posix.S_IXOTH
import platform.posix.closedir
import platform.posix.lstat
import platform.posix.mkdir
import platform.posix.opendir
import platform.posix.readdir
import platform.posix.rmdir
import platform.posix.stat
import platform.posix.unlink

actual fun deleteFolderRecursively(path: String) {
//    println("Deleting folder recursively: $path")
    // Check if the path exists and is a directory
    memScoped {
        val statBuf = alloc<stat>()
        if (lstat(path, statBuf.ptr) != 0) {
//            println("Path does not exist: $path")
            return
        }

        if (statBuf.st_mode.toInt() and S_IFMT != S_IFDIR) {
            error("Path is not a directory: $path")
        }

        // Proceed to delete the directory recursively
        deleteFolderRecursivelyInternal(path)
    }
}

private fun MemScope.deleteFolderRecursivelyInternal(path: String) {
    val dir = opendir(path) ?: error("Failed to open directory: $path")
    try {
        while (true) {
            val entry = readdir(dir) ?: break
            val name = entry.pointed.d_name.toKString()
            if (name == "." || name == "..") continue
            val fullPath = "$path/$name"
            val statBuf = alloc<stat>()
            if (lstat(fullPath, statBuf.ptr) != 0) {
                error("Failed to stat file: $fullPath")
            }

            when (S_IFDIR) {
                statBuf.st_mode.toInt() and S_IFMT -> deleteFolderRecursivelyInternal(fullPath)
                else ->
                    if (unlink(fullPath) != 0) {
                        error("Failed to delete file: $fullPath")
                    }
            }
        }

        // Delete the directory itself
        if (rmdir(path) != 0) {
            error("Failed to delete directory: $path")
        }
    } finally {
        closedir(dir)
    }
}

actual fun Path.createDirectories(): Path {
    val pathString = toString()
    val parts = pathString.split("/")
    var currentPath =
        when {
            pathString.startsWith("/") -> "/"
            else -> ""
        }
    println("Parts: $parts")
    memScoped {
        val statBuf = alloc<stat>()
        for (part in parts) {
            if (part.isEmpty()) {
                continue // Skip empty parts
            }

            currentPath += part

            // Check if the directory exists
            val dirExists =
                stat(currentPath, statBuf.ptr) == 0 && (statBuf.st_mode.toInt() and S_IFDIR != 0)

            if (!dirExists) {
                // Create the directory
                if (mkdir(currentPath, (S_IRWXU or S_IRWXG or S_IROTH or S_IXOTH).convert()) != 0) {
                    error("Failed to create directory: $currentPath")
                }
            }

            currentPath += "/"
        }
    }
    return this
}

actual fun Path.resolve(path: String): Path = Path(toString() + "/" + path)
