plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

dependencies {
    api(libs.kotlin.gradle.plugin)
    api(libs.kotlin.serialization.plugin)
    api(libs.kotlin.serialization.plugin)
    api(libs.ktlint.gradle)
    api(libs.dokka.gradle.plugin)
    api(libs.android.gradle.plugin)
    api(libs.nexus.publish.plugin)
}
