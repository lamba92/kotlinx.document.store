plugins {
    `kotlin-dsl`
}

dependencies {
    api(libs.kotlin.gradle.plugin)
    api(libs.kotlin.serialization.plugin)
    api(libs.ktlint.gradle)
    api(libs.dokka.gradle.plugin)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
