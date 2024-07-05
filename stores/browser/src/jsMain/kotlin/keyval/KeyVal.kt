@file:JsModule("idb-keyval")
@file:JsNonModule
@file:Suppress("unused")

package keyval

import kotlin.js.Promise

external interface IDBRequest<T> {
    var oncomplete: (() -> Unit)?
    var onsuccess: (() -> Unit)?
    var onabort: (() -> Unit)?
    var onerror: (() -> Unit)?
    val result: T
    val error: dynamic
}

external interface IDBTransaction {
    var oncomplete: (() -> Unit)?
    var onsuccess: (() -> Unit)?
    var onabort: (() -> Unit)?
    var onerror: (() -> Unit)?
    val result: dynamic
    val error: dynamic
}

external interface IDBObjectStore {
    fun put(
        value: Any,
        key: String,
    ): IDBRequest<Any>

    fun get(key: String): IDBRequest<Any>

    fun delete(key: String): IDBRequest<Any>

    fun clear(): IDBRequest<Any>

    fun openCursor(): IDBRequest<IDBCursorWithValue>

    fun getAll(): IDBRequest<Array<Any>>

    fun getAllKeys(): IDBRequest<Array<String>>

    val transaction: IDBTransaction
}

external interface IDBCursorWithValue {
    val key: String
    val value: String

    @JsName("continue")
    fun next()
}

external interface UseStore {
    operator fun invoke(
        txMode: String,
        callback: (store: IDBObjectStore) -> Any,
    ): Promise<Any>
}

external fun promisifyRequest(request: dynamic): Promise<Any>

external fun createStore(
    dbName: String,
    storeName: String,
): UseStore

external fun get(
    key: String,
    customStore: UseStore = definedExternally,
): Promise<String?>

external fun set(
    key: String,
    value: Any,
    customStore: UseStore = definedExternally,
): Promise<Unit>

external fun setMany(
    entries: Array<Array<Any>>,
    customStore: UseStore = definedExternally,
): Promise<Unit>

external fun getMany(
    keys: Array<String>,
    customStore: UseStore = definedExternally,
): Promise<Array<Any>>

external fun update(
    key: String,
    updater: (oldValue: Any?) -> Any,
    customStore: UseStore = definedExternally,
): Promise<Unit>

external fun del(
    key: String,
    customStore: UseStore = definedExternally,
): Promise<Unit>

external fun delMany(
    keys: Array<String>,
    customStore: UseStore = definedExternally,
): Promise<Unit>

external fun clear(customStore: UseStore = definedExternally): Promise<Unit>

external fun keys(customStore: UseStore = definedExternally): Promise<Array<String>>

external fun values(customStore: UseStore = definedExternally): Promise<Array<String>>

external fun entries(customStore: UseStore = definedExternally): Promise<Array<Array<String>>>
