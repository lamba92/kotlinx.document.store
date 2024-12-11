package kotlinx.document.store.tests.leveldb

import androidx.test.platform.app.InstrumentationRegistry

actual val DB_PATH: String
    get() =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .filesDir
            .resolve("testdb")
            .absolutePath
