@file:Suppress("UnstableApiUsage")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    `gradle-enterprise`
}

dependencyResolutionManagement {
    repositories {
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
    ":stores:rocksdb",
    ":version-catalog",
    ":samples:js-http-client",
    ":samples:ktor-server",
    ":samples:kmp-app",
)

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlwaysIf(System.getenv("CI") == "true")
    }
}