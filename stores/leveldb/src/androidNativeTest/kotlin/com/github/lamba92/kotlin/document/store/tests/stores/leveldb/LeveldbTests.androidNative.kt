package com.github.lamba92.kotlin.document.store.tests.stores.leveldb

import kotlinx.io.files.Path

actual val DB_PATH: String
    get() = error()

actual fun Path.createDirectories(): Path {
    error()
}

private fun error(): Nothing = error("Kotlin/Native has no tests suite for Android Native")

actual fun Path.resolve(path: String): Path {
    error()
}
