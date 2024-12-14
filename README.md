# kotlin.document.store

Fast NoSql document store for Kotlin Multiplatform projects.

With support for typed and schemaless data, this library enables you to work with JSON objects easily while leveraging `kotlin.serialization` for seamless object serialization/deserialization.

Some key highlights:
- **Multiplatform**: Works on Kotlin/JVM, Kotlin/JS, and ALL Kotlin/Native platforms (excepts wasm).
- **Typed and Schemaless Storage**: Use strongly-typed data models using [kotlin.serialization](https://github.com/kotlin/kotlin.serialization) or raw JSON depending on your needs.
- **Simple APIs**: Built with a developer-friendly, coroutine-based API.
- **Indexing Support**: Create and manage indexes for efficient querying of data.
- **Thread-Safe and Asynchronous**: Built to handle concurrent read/write operations safely, with coroutine-based APIs for non-blocking interactions.
- **Extensible**: Easily extend functionality by plugging in custom serializers or storage backends.

Whether you're building desktop, web, or backend applications, `kotlin.document.store` provides a unified, intuitive way to manage structured or unstructured data across platforms.

# Supported Platforms
There are three main implementations of the `DataStore` interface:
- **MVStore**: For JVM-based applications, using the [H2 Database Engine](https://www.h2database.com/html/main.html) MVStore.
  - JVM
- **LevelDB**: For all Kotlin platforms (excluding JS and Wasm), using [LevelDB](https://github.com/google/leveldb) key-value store.
  - JVM:
    - Windows: arm64, x64
    - Linux: arm64, x64
    - macOs: arm64, x64
  - JS
  - Native (Linux, macOS, Windows, iOS, Android, Android native, watchOS, tvOS)
- **Browser**: For browser-based applications, using the browser's [IndexedDB](https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API) storage.
  - JS

The modules `core` and `test` are common to all platforms and contain the main interfaces and tests for the library and they support also `wasmWasi`.

# Quickstart

Import the library to your project, see the latest version in the [Releases](https://github.com/lamba92/kotlin.document.store/releases) page:

```kotlin
// build.gradle.kts

// Kotlin/JVM
dependencies {
    implementation("com.github.lamba92:kotlin-document-store-mvstore:{latest_version}")
    implementation("com.github.lamba92:kotlin-document-store-leveldb:{latest_version}")
}

// Kotlin/JS
kotlin {
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation("com.github.lamba92:kotlin-document-store-browser:{latest_version}")
            }
        }
    }
}

// Kotlin/Multiplatform (excluding wasm and js)
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.github.lamba92:kotlin-document-store-leveldb:{latest_version}")
            }
        }
    }
}
```

Alternatively with the provided version catalog:
```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    versionCatalogs {
        create("kotlinDocumentStore") {
            from("com.github.lamba92:kotlin-document-store-version-catalog:{latest_version}")
        }
    }
}

// Kotlin/JVM
dependencies {
  implementation(kotlinDocumentStore.mvstore)
  // or
  implementation(kotlinDocumentStore.leveldb)
}

// Kotlin/JS
kotlin {
  sourceSets {
    val jsMain by getting {
      dependencies {
        implementation(kotlinDocumentStore.browser)
      }
    }
  }
}

// Kotlin/Multiplatform (excluding wasm and js)
kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlinDocumentStore.leveldb)
      }
    }
  }
}
```

To get started using `kotlin.document.store`, follow this guide:

---

### 1. Initialize a Persistent DataStore

The library provides platform-specific implementations for persistent data stores. Initialize one depending on your target platform:

- **JVM using MVStore**:

```kotlin
fun main() {
    val store = MVDataStore.open("data.mv.db")
    println("Persistent MVStore Database Initialized!")
    store.close() // Clean up when done
}
```

- **All Kotlin platforms using LevelDB**:

```kotlin
fun main() {
    val store = LevelDBStore.open("a/folder/leveldb")
    println("Persistent LevelDB Database Initialized!")
    store.close() // Clean up when done
}
```

- **Browser with IndexedDB**:

```kotlin
suspend fun main() {
    val store = BrowserStore
    println("Browser IndexedDB Initialized!")
    // No explicit close needed in the browser environment
}
```

---

### 2. Create and Use Collections

Once the `DataStore` has been initialized, retrieve and manipulate JSON-based collections:

```kotlin
suspend fun main() {
    // Initialize the datastore (using MVStore as an example)
    val mvStore = MVDataStore.open("data.mv.db")
    val documentStore = kotlinDocumentStore(mvStore)

    // Create or fetch a collection
    val collection = documentStore.getJsonCollection("users")

    // Insert a document
    val json = buildJsonObject {
        put("name", "Jane Doe")
        put("age", 25)
    }
    collection.insert(json)

    // Retrieve the inserted document
    val allUsers = collection.iterateAll().toList()
    println("Users: $allUsers")

    // Clean up
    documentStore.close()
}
```

---

### 3. Typed Objects with Serialization

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

suspend fun main() {
    // Initialize the datastore and Json serializer
    val mvStore = MVDataStore.open("data.mv.db")
    val documentStore = kotlinDocumentStore(mvStore)

    // Retrieve a typed collection
    val userCollection = documentStore.getObjectCollection<User>("users")

    // Insert a user
    val user = userCollection.insert(User(name = "John Smith", age = 30))
    println("Inserted User: $user")

    val id = requireNotNull(user.id) { "IMPOSSIBAH!" }
    println("User ID: $id")
    
    // Query by name
    val queriedUser = userCollection.find("name", "John Smith").firstOrNull()
    println("Queried User: $queriedUser")

    // Close the store
    documentStore.close()
}
```

---

### 4. Advanced: Indexing and Querying

Speed up queries by using indexes:

```kotlin
suspend fun main() {
    val mvStore = MVDataStore.open("data.mv.db")
    val documentStore = kotlinDocumentStore(mvStore)

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
    documentStore.close()
}
```

# Testing

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

Classes of tests are provided and only the implementation of `DataStore` is needed to run them. See test implementation for:
- [MVDataStore](stores/mvstore/src/test/kotlin/com/github/lamba92/kotlin/document/store/tests/stores/mvstore/MVStoreTests.kt)
- [LevelDBStore](stores/leveldb/src/commonTest/kotlin/com/github/lamba92/kotlin/document/store/tests/stores/leveldb/LeveldbTests.kt)
- [BrowserStore](stores/browser/src/jsTest/kotlin/com/github/lamba92/kotlin/document/store/tests/stores/browser/BrowserTests.kt)