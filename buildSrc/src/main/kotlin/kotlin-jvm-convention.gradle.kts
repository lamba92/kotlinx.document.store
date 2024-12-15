@file:OptIn(ExperimentalPathApi::class)

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import org.jetbrains.kotlin.gradle.internal.builtins.StandardNames.FqNames.target

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    id("versions")
}

kotlin {
    sourceSets.silenceOptIns()
    jvmToolchain(8)
    explicitApi()
}

fun NamedDomainObjectContainer<KotlinSourceSet>.silenceOptIns() = all {
    languageSettings {
        optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        optIn("kotlin.io.path.ExperimentalPathApi")
    }
}

tasks {
    val testDbPath = layout.buildDirectory.file("test-databases").get().asPath
    withType<Test> {
        environment("DB_PATH", testDbPath.absolutePathString())
        useJUnitPlatform()
        systemProperty("jna.debug_load", "true")
        systemProperty("jna.debug_load.jna", "true")
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = true
            showCauses = true
            showExceptions = true
            showStackTraces = true
        }
    }
}
