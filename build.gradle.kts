plugins {
    kotlin("jvm") version "2.0.0"
}

group = "com.github.lamba92"
version = "1.0-SNAPSHOT"

dependencies {
    api("com.h2database:h2:2.2.224")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

}

kotlin {
    sourceSets {
        all {
            languageSettings {
                optIn("com.github.lamba92.kotlin.db.InternalDbApi")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}