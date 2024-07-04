import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

group = "com.github.lamba92"
version = "1.0-SNAPSHOT"

plugins {
    id("org.jlleitschuh.gradle.ktlint")
}


plugins.withId("org.jetbrains.kotlin.jvm") {
    setupKotlin()
}

plugins.withId("org.jetbrains.kotlin.multiplatform") {
    setupKotlin()
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
            }
        }
}