@file:Suppress("UnstableApiUsage")

plugins {
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
    ":version-catalog",
)

val isRocksdbDisabled: Boolean
    get()= System.getenv("DISABLE_ROCKSDB") == "true"

if (!isRocksdbDisabled) {
    include(":stores:rocksdb")
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlwaysIf(System.getenv("CI") == "true")
    }
}