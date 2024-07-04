import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

group = "com.github.lamba92"
version = "1.0-SNAPSHOT"

plugins {
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.dokka")
    `maven-publish`
    signing
}

val dokkaHtml = tasks.named<DokkaTask>("dokkaHtml")

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier = "javadoc"
    from(dokkaHtml)
}

plugins.withId("org.jetbrains.kotlin.jvm") {
    extensions.getByName<KotlinJvmProjectExtension>("kotlin").apply {
        sourceSets.silenceOptIns()
        explicitApi()

        val sourcesJar by tasks.registering(Jar::class) {
            archiveClassifier = "sources"
            from(sourceSets["main"].kotlin)
        }

        publishing {
            publications {
                register<MavenPublication>(project.name) {
                    from(components["kotlin"])
                    artifact(sourcesJar)
                    artifact(javadocJar)
                }
            }
        }
    }
}

plugins.withId("org.jetbrains.kotlin.multiplatform") {
    extensions.getByName<KotlinMultiplatformExtension>("kotlin").apply {
        sourceSets.silenceOptIns()
        explicitApi()
        publishing {
            publications.withType<MavenPublication> {
                artifact(javadocJar)
            }
        }
    }
}

tasks {
    withType<Test> {
        environment("DB_PATH", layout.buildDirectory.file("test.db").get().asFile.absolutePath)
        useJUnitPlatform()
    }
}

fun NamedDomainObjectContainer<KotlinSourceSet>.silenceOptIns() = all {
    languageSettings {
        optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        optIn("kotlinx.cinterop.ExperimentalForeignApi")
        optIn("kotlin.io.path.ExperimentalPathApi")
    }
}


signing {
    val secretKey: String? = System.getenv("SECRET_KEY")
    val password: String? = System.getenv("SECRET_KEY_PASSWORD")

    if (secretKey != null && password != null) {
        useInMemoryPgpKeys(secretKey, password)
        publishing.publications.all {
            sign(this)
        }
    }
}

publishing {
    repositories {
        maven(rootProject.layout.buildDirectory.dir("mavenRepo")) {
            name = "test"
        }
    }

    afterEvaluate {
        publications.all {
            if (this !is MavenPublication) return@all
            artifactId = "${rootProject.name}-$artifactId"
        }
    }

}