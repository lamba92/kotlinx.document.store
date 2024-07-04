import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

group = "com.github.lamba92"
version = "1.0-SNAPSHOT"

plugins {
    id("org.jlleitschuh.gradle.ktlint")
}


plugins.withId("org.jetbrains.kotlin.jvm") {
    setupKotlin()
    extensions.getByName<KotlinJvmProjectExtension>("kotlin").apply {
        explicitApi()
    }
}

plugins.withId("org.jetbrains.kotlin.multiplatform") {
    setupKotlin()
    extensions.getByName<KotlinMultiplatformExtension>("kotlin").apply {
        explicitApi()
    }
}

tasks {
    withType<Test> {
        environment("DB_PATH", layout.buildDirectory.file("test.db").get().asFile.absolutePath)
        useJUnitPlatform()
    }
}

fun Project.setupKotlin() {
    extensions.getByName<ExtensionAware>("kotlin")
        .extensions
        .getByName<NamedDomainObjectContainer<KotlinSourceSet>>("sourceSets")
        .all {
            languageSettings {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlin.io.path.ExperimentalPathApi")
            }
        }
}