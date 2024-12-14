package com.github.lamba92.kotlin.document.store.tests.stores.leveldb

import androidx.test.platform.app.InstrumentationRegistry

actual val DB_PATH: String
    get() =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .filesDir
            .resolve("testdb")
            .absolutePath
