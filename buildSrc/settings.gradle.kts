@file:Suppress("UnstableApiUsage")

rootProject.name = "buildSrc"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    rulesMode = RulesMode.PREFER_SETTINGS
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}