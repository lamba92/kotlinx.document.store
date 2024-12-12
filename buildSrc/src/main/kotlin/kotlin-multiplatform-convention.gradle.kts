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
    id("versions")
}

val currentOs: OperatingSystem = OperatingSystem.current()

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

    // in CI we only want to publish the artifacts for the current OS only
    // but when developing we want to publish all the possible artifacts to test them
    if (isCi) {

        val linuxNames = listOf("linux", "android", "jvm", "js", "kotlin", "metadata", "wasm")
        val windowsNames = listOf("mingw", "windows")
        val appleNames = listOf("macos", "ios", "watchos", "tvos")

        withType<AbstractPublishToMaven> {
            when {
                name.containsAny(linuxNames) -> onlyIf { currentOs.isLinux }
                name.containsAny(windowsNames) -> onlyIf { currentOs.isWindows }
                name.containsAny(appleNames) -> onlyIf { currentOs.isMacOsX }
            }
        }
    }
}

val isCi
    get() = System.getenv("CI") == "true"

fun String.containsAny(strings: List<String>, ignoreCase: Boolean = true): Boolean =
    strings.any { contains(it, ignoreCase) }
