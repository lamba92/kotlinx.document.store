package kotlinx.document.store.tests.leveldb

actual val DB_PATH: String
    get() = System.getenv("DB_PATH") ?: error("DB_PATH not set")
