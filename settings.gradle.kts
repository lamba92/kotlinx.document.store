@file:Suppress("UnstableApiUsage")

rootProject.name = "kotlinx-document-store"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    rulesMode = RulesMode.PREFER_SETTINGS
}


include(
    ":core",
    ":stores:mvstore",
    ":stores:rocksdb",
    ":stores:browser",
)