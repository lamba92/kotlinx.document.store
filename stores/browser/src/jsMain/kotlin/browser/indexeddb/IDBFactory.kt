package browser.indexeddb

external interface IDBFactory {
    fun open(name: String, version: Int = definedExternally): IDBOpenDBRequest
    fun deleteDatabase(name: String): IDBOpenDBRequest
    fun cmp(first: dynamic, second: dynamic): Int
}

external interface IDBOpenDBRequest : IDBRequest<IDBDatabase> {
    var onupgradeneeded: ((IDBVersionChangeEvent<IDBOpenDBRequest>) -> Unit)?
    var onsuccess: ((Event<IDBOpenDBRequest>) -> Unit)?
    var onerror: ((Event<IDBOpenDBRequest>) -> Unit)?
}

external interface IDBRequest<T> : EventTarget<T> {
    override val result: T
    val error: dynamic
    val source: IDBRequestSource?
    val transaction: IDBTransaction?
}

external interface IDBDatabase : EventTarget<IDBDatabase> {
    val name: String
    val version: Int
    val objectStoreNames: DOMStringList
    fun createObjectStore(name: String, options: IDBObjectStoreParameters = definedExternally): IDBObjectStore
    fun deleteObjectStore(name: String)
    fun transaction(storeNames: dynamic, mode: String = definedExternally): IDBTransaction
    fun close()
}

external interface IDBTransaction : EventTarget<IDBTransaction> {
    val db: IDBDatabase
    val mode: String
    val objectStoreNames: DOMStringList
    val error: dynamic
    fun objectStore(name: String): IDBObjectStore
    var oncomplete: ((Event<IDBTransaction>) -> Unit)?
    var onerror: ((Event<IDBTransaction>) -> Unit)?
    var onabort: ((Event<IDBTransaction>) -> Unit)?
    fun abort()
}

external interface IDBObjectStore : IDBRequestSource {
    val name: String
    val keyPath: dynamic
    val indexNames: DOMStringList
    val transaction: IDBTransaction
    fun put(value: dynamic, key: dynamic = definedExternally): IDBRequest<Any>
    fun add(value: dynamic, key: dynamic = definedExternally): IDBRequest<Any>
    fun delete(key: dynamic): IDBRequest<Any>
    fun clear(): IDBRequest<Any>
    fun get(key: dynamic): IDBRequest<Any>
    fun openCursor(range: IDBKeyRange = definedExternally, direction: String = definedExternally): IDBRequest<Any>
    fun createIndex(name: String, keyPath: dynamic, options: dynamic = definedExternally): IDBIndex
    fun index(name: String): IDBIndex
    fun deleteIndex(name: String)
}

external interface IDBIndex : IDBRequestSource {
    val name: String
    val objectStore: IDBObjectStore
    val keyPath: dynamic
    val multiEntry: Boolean
    val unique: Boolean
    fun get(key: dynamic): IDBRequest<Any>
    fun getKey(key: dynamic): IDBRequest<Any>
    fun openCursor(range: IDBKeyRange = definedExternally, direction: String = definedExternally): IDBRequest<Any>
    fun openKeyCursor(range: IDBKeyRange = definedExternally, direction: String = definedExternally): IDBRequest<Any>
}

external interface IDBObjectStoreParameters {
    val keyPath: dynamic /* String? */
    val autoIncrement: Boolean?
}

external interface IDBKeyRange {
    val lower: dynamic
    val upper: dynamic
    val lowerOpen: Boolean
    val upperOpen: Boolean
}

external interface DOMStringList {
    val length: Int
    fun item(index: Int): String?
    fun contains(string: String): Boolean
    fun forEach(callback: (String) -> Unit)
}

external interface EventTarget<T> {
    val result: T
}

external interface Event<T> {
    val type: String
    val target: T
}

external interface IDBVersionChangeEvent<T> : Event<T> {
    val oldVersion: Int
    val newVersion: Int?
}

external interface IDBRequestSource

@JsName("indexedDB")
external val indexedDB: IDBFactory
