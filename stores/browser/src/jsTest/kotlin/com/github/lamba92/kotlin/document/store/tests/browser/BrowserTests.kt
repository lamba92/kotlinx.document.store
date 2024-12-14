@file:Suppress("unused")

package com.github.lamba92.kotlin.document.store.tests.browser

import com.github.lamba92.kotlin.document.store.core.DataStore
import com.github.lamba92.kotlin.document.store.stores.browser.BrowserStore
import com.github.lamba92.kotlin.document.store.tests.AbstractDeleteTests
import com.github.lamba92.kotlin.document.store.tests.AbstractDocumentDatabaseTests
import com.github.lamba92.kotlin.document.store.tests.AbstractFindTests
import com.github.lamba92.kotlin.document.store.tests.AbstractIndexTests
import com.github.lamba92.kotlin.document.store.tests.AbstractInsertTests
import com.github.lamba92.kotlin.document.store.tests.AbstractObjectCollectionTests
import com.github.lamba92.kotlin.document.store.tests.AbstractUpdateTests
import com.github.lamba92.kotlin.document.store.tests.DataStoreProvider
import kotlinx.coroutines.await

class BrowserDeleteTests : AbstractDeleteTests(BrowserStoreProvider)

class BrowserDocumentDatabaseTests : AbstractDocumentDatabaseTests(BrowserStoreProvider)

class BrowserIndexTests : AbstractIndexTests(BrowserStoreProvider)

class BrowserInsertTests : AbstractInsertTests(BrowserStoreProvider)

class BrowserUpdateTests : AbstractUpdateTests(BrowserStoreProvider)

class BrowserFindTests : AbstractFindTests(BrowserStoreProvider)

class BrowserObjectCollectionTests : AbstractObjectCollectionTests(BrowserStoreProvider)

object BrowserStoreProvider : DataStoreProvider {
    override suspend fun deleteDatabase(testName: String) {
        keyval.clear().await()
    }

    override fun provide(testName: String): DataStore = BrowserStore
}
