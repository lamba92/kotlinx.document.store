package kotlinx.document.database.tests.rocksdb

import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import kotlinx.cinterop.toKStringFromUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import platform.posix.DT_DIR
import platform.posix.closedir
import platform.posix.getenv
import platform.posix.opendir
import platform.posix.readdir
import platform.posix.remove
import platform.posix.rmdir

actual val DB_PATH: String
    get() = getenv("DB_PATH")?.toKStringFromUtf8() ?: error("DB_PATH not set")

actual suspend fun Path.deleteRecursively() =
    withContext(Dispatchers.IO) {
        deleteFolderRecursively(this@deleteRecursively.toString())
    }

fun deleteFolderRecursively(path: String) {
    // Create a memory scope to manage memory allocation and deallocation automatically
    memScoped {
        // Open the directory specified by the path
        val dir = opendir(path) ?: return@memScoped // If the directory can't be opened, exit the scope
        println("Deleting folder $path")
        try {
            while (true) {
                // Read the next entry in the directory
                val entry = readdir(dir) ?: break // If there are no more entries, exit the loop
                // Get the name of the entry
                val name = entry.pointed.d_name.toKString()
                // Skip the special entries "." and ".." which refer to the current and parent directories
                if (name == "." || name == "..") continue

                // Construct the full path of the entry
                val fullPath = "$path/$name"
                // Check if the entry is a directory
                if (entry.pointed.d_type.toInt() == DT_DIR) {
                    // If it's a directory, call this function recursively to delete its contents
                    deleteFolderRecursively(fullPath)
                } else {
                    // If it's a file, delete it
                    println("Deleting $name")
                    remove(fullPath)
                }
            }
        } finally {
            // Close the directory to free resources
            closedir(dir)
        }
        // After all contents have been processed, remove the directory itself
        rmdir(path)
        println("Deleted folder $path")
    }
}
