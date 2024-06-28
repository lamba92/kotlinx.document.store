plugins {
    `kotlin-dsl`
}

dependencies {
    api(libs.kotlin.gradle.plugin)
    api(libs.kotlin.serialization.plugin)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}