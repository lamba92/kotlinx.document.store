package com.github.lamba92.kotlin.db

import kotlin.io.path.deleteIfExists
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class BaseTest(store: DataStore) {

    val db = KotlinxDocumentDatabase(store)

    @BeforeEach
    fun deleteDb() = runTest {
        dbPath.deleteIfExists()
    }

    @AfterEach
    fun closeDb() = db.close()
}