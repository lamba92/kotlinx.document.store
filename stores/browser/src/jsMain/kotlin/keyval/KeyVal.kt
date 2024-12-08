@file:JsModule("idb-keyval")
@file:JsNonModule
@file:Suppress("unused")

package keyval

import kotlin.js.Promise

public external interface IDBRequest<T> {
    public var oncomplete: (() -> Unit)?
    public var onsuccess: (() -> Unit)?
    public var onabort: (() -> Unit)?
    public var onerror: (() -> Unit)?
    public val result: T
    public val error: dynamic
}

public external interface IDBTransaction {
    public var oncomplete: (() -> Unit)?
    public var onsuccess: (() -> Unit)?
    public var onabort: (() -> Unit)?
    public var onerror: (() -> Unit)?
    public val result: dynamic
    public val error: dynamic
}

public external interface IDBObjectStore {
    public fun put(
        value: Any,
        key: String,
    ): IDBRequest<Any>

    public fun get(key: String): IDBRequest<Any>

    public fun delete(key: String): IDBRequest<Any>

    public fun clear(): IDBRequest<Any>

    public fun openCursor(): IDBRequest<IDBCursorWithValue>

    public fun getAll(): IDBRequest<Array<Any>>

    public fun getAllKeys(): IDBRequest<Array<String>>

    public val transaction: IDBTransaction
}

public external interface IDBCursorWithValue {
    public val key: String
    public val value: String

    @JsName("continue")
    public fun next()
}

public external interface UseStore {
    public operator fun invoke(
        txMode: String,
        callback: (store: IDBObjectStore) -> Any,
    ): Promise<Any>
}

public external fun promisifyRequest(request: dynamic): Promise<Any>

public external fun createStore(
    dbName: String,
    storeName: String,
): UseStore

public external fun get(
    key: String,
    customStore: UseStore = definedExternally,
): Promise<String?>

public external fun set(
    key: String,
    value: Any,
    customStore: UseStore = definedExternally,
): Promise<Unit>

public external fun setMany(
    entries: Array<Array<Any>>,
    customStore: UseStore = definedExternally,
): Promise<Unit>

public external fun getMany(
    keys: Array<String>,
    customStore: UseStore = definedExternally,
): Promise<Array<Any>>

public external fun update(
    key: String,
    updater: (oldValue: Any?) -> Any,
    customStore: UseStore = definedExternally,
): Promise<Unit>

public external fun del(
    key: String,
    customStore: UseStore = definedExternally,
): Promise<Unit>

public external fun delMany(
    keys: Array<String>,
    customStore: UseStore = definedExternally,
): Promise<Unit>

public external fun clear(customStore: UseStore = definedExternally): Promise<Unit>

public external fun keys(customStore: UseStore = definedExternally): Promise<Array<String>>

public external fun values(customStore: UseStore = definedExternally): Promise<Array<String>>

public external fun entries(customStore: UseStore = definedExternally): Promise<Array<Array<String>>>
