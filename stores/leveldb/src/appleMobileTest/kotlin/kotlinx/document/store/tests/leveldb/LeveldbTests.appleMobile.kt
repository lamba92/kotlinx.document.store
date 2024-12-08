package kotlinx.document.store.tests.leveldb

import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL

actual val DB_PATH: String
    get() =
        createTestFilePath()
            ?: error("Failed to create test file for database")

fun createTestFilePath(): String? {
    // Use the temporary directory for test files
    val tmpDir = NSURL.fileURLWithPath(NSTemporaryDirectory())

    // Create a subdirectory for organization, if desired
    val testDir =
        tmpDir.URLByAppendingPathComponent("testDir")
            ?: error("Failed to create test directory")
    NSFileManager.defaultManager.createDirectoryAtURL(
        url = testDir,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )

    // Return the absolute path as a String
    return testDir.path
}
