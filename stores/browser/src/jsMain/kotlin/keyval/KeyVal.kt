@file:JsModule("idb-keyval")
@file:JsNonModule

package keyval

import kotlin.js.Promise

public external class Store(dbName: String = definedExternally, storeName: String = definedExternally) {
    public val storeName: String
}

public external fun get(
    key: String,
    store: Store = definedExternally,
): Promise<String>

public external fun set(
    key: String,
    value: String,
    store: Store = definedExternally,
): Promise<Unit>

public external fun del(
    key: String,
    store: Store = definedExternally,
): Promise<Unit>

public external fun clear(store: Store = definedExternally): Promise<Unit>

public external fun keys(store: Store = definedExternally): Promise<Array<String>>
