@file:OptIn(ExperimentalPathApi::class)

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively

plugins {
    kotlin("multiplatform")
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
        optIn("kotlin.io.path.ExperimentalPathApi")
        optIn("kotlinx.cinterop.ExperimentalForeignApi")
        optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

tasks {

    val testDbPath = layout.buildDirectory.file("test-databases").get().asPath
    withType<Test> {
        val namedTestDbPath = testDbPath
            .resolve(name)
            .createDirectories()
        doFirst { namedTestDbPath.deleteRecursively() }
        environment("DB_PATH", namedTestDbPath.absolutePathString())
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
    withType<KotlinNativeHostTest> {
        val namedTestDbPath = testDbPath
            .resolve(name)
            .createDirectories()
        doFirst { namedTestDbPath.deleteRecursively() }
        environment("DB_PATH", namedTestDbPath.absolutePathString())
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = true
            showCauses = true
            showExceptions = true
            showStackTraces = true
        }
    }
}
