import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass = "kotlinx.document.store.samples.ktor.server.MainKt"
}

dependencies {
    implementation(projects.samples)
    implementation(projects.stores.leveldb)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    runtimeOnly(libs.logback.classic)
}

tasks {
    named<JavaExec>("run") {
        val dbPath = layout.buildDirectory.dir("db").get().asPath
        environment("DB_PATH", dbPath.absolutePathString())
        doFirst {
            dbPath.createDirectories()
        }
    }
}
