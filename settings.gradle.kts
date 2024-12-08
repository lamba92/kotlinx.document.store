@file:Suppress("UnstableApiUsage")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
    id("com.gradle.develocity") version "3.18.2"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    rulesMode = RulesMode.PREFER_SETTINGS
}

rootProject.name = "kotlinx-document-store"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    ":core",
    ":tests",
    ":stores:mvstore",
    ":stores:browser",
    ":stores:leveldb",
    ":version-catalog",
    ":samples:js-http-client",
    ":samples:ktor-server",
    ":samples:kmp-app",
)

includeBuild("kotlin-leveldb") {
    val endings = listOf(
        "jvm",
        "js",
        "mingwx64",
        "linuxx64",
        "linuxarm64",
        "macosx64",
        "macosarm64",
        "iosarm64",
        "iosx64",
        "iosSimulatorarm64",
        "watchosarm64",
        "watchosx64",
        "watchosSimulatorarm64",
        "tvosarm64",
        "tvosx64",
        "tvosSimulatorarm64",
    )
    dependencySubstitution {
        substitute(module("com.github.lamba92:kotlin-leveldb")).using(project(":"))
        endings.forEach {
            substitute(module("com.github.lamba92:kotlin-leveldb-$it")).using(project(":"))
        }
    }
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        publishing {
            onlyIf { System.getenv("CI") == "true" }
        }
    }
}