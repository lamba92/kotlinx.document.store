@file:JsModule("idb-keyval")
@file:JsNonModule

package keyval

import kotlin.js.Promise

external class Store(dbName: String = definedExternally, storeName: String = definedExternally) {
    val storeName: String
}

external fun get(key: String, store: Store = definedExternally): Promise<String>
external fun set(key: String, value: String, store: Store = definedExternally): Promise<Unit>
external fun del(key: String, store: Store = definedExternally): Promise<Unit>
external fun clear(store: Store = definedExternally): Promise<Unit>
external fun keys(store: Store = definedExternally): Promise<Array<String>>
