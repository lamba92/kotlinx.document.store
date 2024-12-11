package kotlinx.document.store.tests.leveldb

import kotlinx.cinterop.toKString

actual val DB_PATH: String
    get() = platform.posix.getenv("DB_PATH")?.toKString() ?: error("DB_PATH not set")
