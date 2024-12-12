package kotlinx.document.store.tests.stores.leveldb

import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.S_IFDIR
import platform.posix.S_IFMT
import platform.posix.closedir
import platform.posix.lstat
import platform.posix.opendir
import platform.posix.readdir
import platform.posix.rmdir
import platform.posix.stat
import platform.posix.unlink

actual fun deleteFolderRecursively(path: String): Unit =
    memScoped {
        deleteFolderRecursively(path)
    }

private fun MemScope.deleteFolderRecursively(path: String) {
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
                statBuf.st_mode.toInt() and S_IFMT -> deleteFolderRecursively(fullPath)
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
