# Kotlin Multiplatform Embedded NoSql Document Database

Kotlinx.document.store is an abstraction on top of platform-specific key-value stores like RocksDB, MVStore, etc. It provides a simple API to store and retrieve JSON documents.

## Features

- **Multiplatform**: 
  - :heavy_check_mark: JVM (MVStore, RocksDB)
     - `com.github.lamba92:kotlinx-document-store-mvstore:1.0.0-SNAPSHOT`
     - `com.github.lamba92:kotlinx-document-store-rocksdb:1.0.0-SNAPSHOT`
  - :heavy_check_mark: JS/Browser (IndexedDB [idb-keyval](https://www.npmjs.com/package/idb-keyval))
    - `com.github.lamba92:kotlinx-document-store-browser:1.0.0-SNAPSHOT`
  - :hourglass: macOS (RockDB)
    - `com.github.lamba92:kotlinx-document-store-rocksdb:1.0.0-SNAPSHOT`
  - :hourglass: iOS (RockDB)
    - `com.github.lamba92:kotlinx-document-store-rocksdb:1.0.0-SNAPSHOT`
  - :x: watchOs
  - :x: tvOs
  - :x: Linux
  - :x: Windows

- **Simple**

```kotlin
// JVM
val dataStore: DataStore = MVStore.open("test.db").asDataStore()

// JS
val datataStore = IndexedDBStore

// common
val db = KotlinxDocumentDatabase {
    store = dataStore
}

@Serializable // kotlinx.serialization
data class User(val name: String, val age: Int)

val usersCollection: ObjectCollection<User> = db.getObjectCollection<User>("users")

usersCollection.createIndex("name")

val jhon = User("John", 30)

usersCollection.insert(jhon)

val aJhon: User = usersCollection.find("name", "John") // Flow<User>
  .filter { it.age > 20 }
  .first()
```

**Schemaless**

While one can work with typed objects, kotlinx.document.store also supports schemaless documents relying on kotlinx.serialization for serialization [JsonObject](https://github.com/Kotlin/kotlinx.serialization/blob/c75b46dee6216f600f2c94a0817f0f90fc8ed029/formats/json/commonMain/src/kotlinx/serialization/json/JsonElement.kt#L191)

```kotlin
val db = KotlinxDocumentDatabase {
    store = MVStore.open("test.db").asDataStore()
}

val jsonCollection: JsonCollection = db.getJsonCollection("users")

val jhon: JsonObject = jsonCollection.find("name", "John") // Flow<JsonObject>
  .filter { it["age"].jsonPrimitive.int > 20 }
  .first()

jsonCollection.insert(
  buildJsonObject {
    put("surname", "117")
    put("age", 30)
  }
)
```

Of course deserialization into object matters! Don't forget to update your data classes accordingly if you also access the same collection with typed objects.

## Installation

```kotlin
// settings.gradle.kts
dependecyResolutionManagement {
    repositories {
        maven("https://packages.jetbrains.team/maven/p/kpm/public")
    }
}

// build.gradle.kts Kotlin/Multiplatform
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.github.lamba92:kotlinx-document-store-core:1.0.0-SNAPSHOT")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("com.github.lamba92:kotlinx-document-store-mvstore:1.0.0-SNAPSHOT")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("com.github.lamba92:kotlinx-document-store-browser:1.0.0-SNAPSHOT")
            }
        }
    }
}

// build.gradle.kts Kotlin/JVM
dependencies {
    implementation("com.github.lamba92:kotlinx-document-store-mvstore:1.0.0-SNAPSHOT")
}
```

We also publish the [version catalog](./gradle/libs.versions.toml) for kotlinx.document.store, so you can use it in your project:

```kotlin
// settings.gradle.kts
dependecyResolutionManagement {
    repositories {
        maven("https://packages.jetbrains.team/maven/p/kpm/public")
    }
    versionCatalogs {
        create("kotlinxDocumentStore") { // you can name it as you like, it will change the name of the variable
            from("com.github.lamba92:kotlinx-document-store-version-catalog:1.0.0-SNAPSHOT")
        }
    }
}

// build.gradle.kts Kotlin/Multiplatform
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlinxDocumentStore.core)
            }
        }
        val jvmMain by getting {
            dependencies {
              implementation(kotlinxDocumentStore.mvstore)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(kotlinxDocumentStore.browser)
            }
        }
    }
}

// build.gradle.kts Kotlin/JVM
dependencies {
    implementation(kotlinxDocumentStore.mvstore)
}
```