# kotlin.document.store

Fast NoSql document store for Kotlin Multiplatform, inspired by [Nitrite-java](https://github.com/nitrite/nitrite-java) and [MongoDB](https://github.com/mongodb/mongo).

With support for typed and schemaless data, lets you work with JSON objects easily, leveraging `kotlin.serialization` for fast and simple object serialization/deserialization.

Originally created for [JetBrains/package-search-intellij-plugin](https://github.com/JetBrains/package-search-intellij-plugin) for a fast and reliable offline cache, evolved for all KMP developers 🚀

Some key highlights:
- **Multiplatform**: Works on Kotlin/JVM, Kotlin/JS, and ALL Kotlin/Native platforms (excepts wasm).
- **Typed and Schemaless Storage**: Use strongly-typed data models using [kotlin.serialization](https://github.com/kotlin/kotlin.serialization) or raw JSON depending on your needs.
- **Simple APIs**: Built with a developer-friendly, coroutine-based API.
- **Indexing Support**: Create and manage indexes for efficient querying of data.
- **Thread-Safe and Asynchronous**: Built to handle concurrent read/write operations safely, with coroutine-based APIs for non-blocking interactions.
- **Extensible**: Easily extend functionality by plugging in custom serializers or storage backends.

Whether you're building desktop, web, or backend applications, `kotlin.document.store` provides a unified, intuitive way to manage structured or unstructured data across platforms.

1. [Overview](#kotlindocumentstore)
2. [Supported Platforms](#supported-platforms)
3. [Quickstart](#quickstart)
    - [Android and JVM](#android-and-jvm)
        - [Dependency Setup](#dependency-setup)
            - [Gradle Setup](#gradle-setup)
            - [Using Version Catalog](#using-version-catalog)
            - [In your Activity](#in-your-android-activity)
            - [For JVM](#for-jvm)
    - [JS Browser](#js-browser)
        - [Dependency Setup](#dependency-setup-1)
            - [Gradle Setup](#gradle-setup-1)
            - [Using Version Catalog](#using-version-catalog-1)
            - [Anywhere in your code](#anywhere-in-your-code)
    - [Typed collections](#typed-collections)
    - [JSON collections](#json-collections)
4. [Advanced usage](#advanced-usage)
    - [Indexes](#indexes)
        - [Index Selector](#index-selector)
        - [Array Indexing](#array-indexing)
    - [ID Field](#id-field)
5. [Testing](#testing)
    - [DataStore Test Setup](#datastore-test-setup)
    - [Available Test Implementations](#available-test-implementations)

# Supported Platforms
There are three main implementations of the `DataStore` interface:
- **LevelDB**: For all Kotlin platforms (excluding JS and Wasm), using [kotlin-leveldb](https://github.com/lamba92/kotlin-leveldb) key-value store.
  - JVM:
    - Windows: arm64, x64
    - Linux: arm64, x64
    - macOs: arm64, x64
  - JS
  - Native (Linux, macOS, Windows, iOS, Android, Android native, watchOS, tvOS)
- **Browser**: For browser-based applications, using the browser's [IndexedDB](https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API) storage.
  - JS
- **MVStore**: For JVM-based applications, using the [H2 Database Engine](https://www.h2database.com/html/main.html) MVStore. Recommended only for IntelliJ Plugin development.
  - JVM

google/leveldb is licensed under [BSD-3-Clause license](https://github.com/google/leveldb/blob/main/LICENSE), all rights reserved to the original authors.

The modules `core` and `test` are common to all platforms and contain the main interfaces and tests for the library and they support also `wasmWasi`.

# Quickstart

## Android and JVM

### Dependency Setup
Import the library to your project, see the latest version in the [Releases](https://github.com/lamba92/kotlin.document.store/releases) page:

#### Gradle Setup
```kotlin
dependencies {
    implementation("com.github.lamba92:kotlin-document-store-leveldb:{latest_version}")
}
```
#### Using Version Catalog
Alternatively with the provided version catalog:
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("kotlinDocumentStore") {
            from("com.github.lamba92:kotlin-document-store-version-catalog:{latest_version}")
        }
    }
}

// build.gradle.kts
dependencies {
  implementation(kotlinDocumentStore.leveldb)
}
```

#### In your Android Activity

```kotlin
class MyActivity : CompactActivity() {
    
    override fun onCreate(): String {
        val store = context.getLevelDBStore()
        val db = KotlinDocumentStore(store)
        // your stuff...
    }
    
}
```

#### For JVM

```kotlin
fun main() {
    val store = LevelDBStore.open("path/to/db")
    val db = KotlinDocumentStore(store)
    // your stuff...
}
```

## JS Browser

### Dependency Setup
Import the library to your project, see the latest version in the [Releases](https://github.com/lamba92/kotlin.document.store/releases) page:

#### Gradle Setup
```kotlin
dependencies {
    implementation("com.github.lamba92:kotlin-document-store-browser:{latest_version}")
}
```
#### Using Version Catalog
Alternatively with the provided version catalog:
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("kotlinDocumentStore") {
            from("com.github.lamba92:kotlin-document-store-version-catalog:{latest_version}")
        }
    }
}

// build.gradle.kts
dependencies {
  implementation(kotlinDocumentStore.browser)
}
```

#### Anywhere in your code

```kotlin
val db = KotlinDocumentStore(BrowserStore)
```

---

### Typed collections

Use `ObjectCollection` to store and manipulate strongly typed objects:

```kotlin
@Serializable
data class User(
    // id declaration is optional, but if declared make sure
    // to make it `Long? = null` to handle insertions where id is not provided
    @SerialName("_id") val id: Long? = null, 
    val name: String, 
    val age: Int
)

// ...

val documentStore = KotlinDocumentStore(aDataStore)

// Retrieve a typed collection
val userCollection = documentStore.getObjectCollection<User>("users")

// Insert a user
val user = userCollection.insert(User(name = "John Smith", age = 30))
println("Inserted User: $user") // will also print the generated id

val id = requireNotNull(user.id) { "IMPOSSIBAH!" }
println("User ID: $id")

// Query by name
val queriedUser = userCollection.find("name", "John Smith").firstOrNull()
println("Queried User: $queriedUser")

// Close the store
documentStore.close()
```

---

### JSON collections

Use `JsonCollection` to store and manipulate raw JSON objects:

```kotlin
val documentStore = KotlinDocumentStore(aDataStore)

// Create or fetch a collection
val collection = documentStore.getJsonCollection("users")

// Insert a document
val json = buildJsonObject { // kotlinx.serialization json APIs
    put("name", "Jane Doe")
    put("age", 25)
}
collection.insert(json)

// Retrieve the inserted document
val allUsers = collection.iterateAll().toList()
println("Users: $allUsers")

// Clean up
documentStore.close()
```
---

# Advanced usage

## Indexes

Any collection, JSON or typed, can have indexes created on them for faster querying:

```kotlin
val documentStore = kotlinDocumentStore(aDataStore)

val collection = documentStore.getJsonCollection("users")

// Create an index
collection.createIndex("name")

// Insert documents
val json1 = buildJsonObject {
    put("name", "Alice")
    put("age", 28)
}
val json2 = buildJsonObject {
    put("name", "Bob")
    put("age", 34)
}
collection.insert(json1)
collection.insert(json2)

// Use the index to query
val results = collection.find("name", "Alice").toList()

println("Search Results: $results")
```

In the example above, we create an index on the `name` field of the `users` collection. We then insert two documents into the collection and query for documents where the `name` field is `Alice`. The query uses the index to find the documents faster.

### Index Selector

Indexes can be created using JSON selectors to index nested fields. For example, consider the following JSON document:

```json
{
  "name": "Alice",
  "address": {
    "city": "New York",
    "zip": 10001
  }
}
```

To create an index on the `city` field in the `address` object, we can use a JSON selector:

```kotlin
val documentStore = kotlinDocumentStore(aDataStore)
val collection = documentStore.getJsonCollection("users")
collection.createIndex("address.city")
```

Now, an index is created on the `city` field in the `address` object. We can query for documents where the `city` field is, for example, `New York`:

```kotlin
val results = collection.find("address.city", "New York").toList()
```

##### Array Indexing

Indexes can also be created on array fields. For example, consider the following JSON document:

```json
{
  "name": "Alice",
  "tags": ["tag1", "tag2", "tag3"]
}
```

To create an index on the `tags` array field, we can use a JSON selector:

```kotlin
val documentStore = kotlinDocumentStore(aDataStore)
val collection = documentStore.getJsonCollection("users")
collection.createIndex("tags.$3")
```

Now, an index is created on the third element of the `tags` array field. We can query for documents where the third element of the `tags` array is, for example, `tag3`:

```kotlin
val results = collection.find("tags.$3", "tag3").toList()
```

## ID field

The ID field name is `_id` and cannot be changed in the representation inside the database. The ID has to be of type `Long` and is autogenerated if not provided when inserting a document.

When using typed collections, the ID field is optional in the data class, but it has to be of type `Long?` and nullable. Dor example:

```kotlin
@Serializable
data class User(
    val _id: Long? = null,
    val name: String,
    val age: Int
)
```

It is possible to change the name of the ID field in the data class using the `@SerialName` annotation, but the actual field name in the database will always be `_id`:
    
```kotlin
@Serializable
data class User(
    @SerialName("_id") val id: Long? = null,
    val name: String,
    val age: Int
)
```

# Testing

## DataStore Test Setup
To test your own implementation of [DataStore](core/src/commonMain/kotlin/com/github/lamba92/kotlin/document/store/core/DataStore.kt), 
you can use the provided module `kotlin-document-store-test`:

```kotlin
// build.gradle.kts

// Kotlin/JVM
dependencies {
    testImplementation("com.github.lamba92:kotlin-document-store-test:{latest_version}")
}

// Kotlin/JS or Kotlin/Multiplatform
kotlin {
    sourceSets {
        commonTest {
            dependencies {
                implementation("com.github.lamba92:kotlin-document-store-test:{latest_version}")
            }
        }
    }
}
```
## Available Test Implementations
Classes of tests are provided and only the implementation of `DataStore` is needed to run them. See test implementation for:
- [MVDataStore](stores/mvstore/src/test/kotlin/com/github/lamba92/kotlin/document/store/tests/stores/mvstore/MVStoreTests.kt)
- [LevelDBStore](stores/leveldb/src/commonTest/kotlin/com/github/lamba92/kotlin/document/store/tests/stores/leveldb/LeveldbTests.kt)
- [BrowserStore](stores/browser/src/jsTest/kotlin/com/github/lamba92/kotlin/document/store/tests/stores/browser/BrowserTests.kt)
