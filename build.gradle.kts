plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
}

group = "com.github.lamba92"
version = "1.0-SNAPSHOT"

dependencies {
    api("com.h2database:h2:2.2.224")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

kotlin {
    sourceSets {
        all {
            languageSettings {
                optIn("com.github.lamba92.kotlin.db.InternalDbApi")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }
    }
}
repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}