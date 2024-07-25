import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest

val GITHUB_REF: String? = System.getenv("GITHUB_REF")

group = "com.github.lamba92"
version = when {
    GITHUB_REF?.startsWith("refs/tags/") == true -> GITHUB_REF.substringAfter("refs/tags/")
    else -> "1.0.0-SNAPSHOT"
}

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
        jvmToolchain(17)
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
        jvmToolchain(17)
        explicitApi()
        publishing {
            publications.withType<MavenPublication> {
                artifact(javadocJar)
            }
        }
    }
}

fun NamedDomainObjectContainer<KotlinSourceSet>.silenceOptIns() = all {
    languageSettings {
        optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        optIn("kotlinx.cinterop.ExperimentalForeignApi")
        optIn("kotlin.io.path.ExperimentalPathApi")
    }
}

val secretKey: String? = System.getenv("SECRET_KEY")
    ?: rootProject.file("secret.txt")
        .takeIf { it.exists() }
        ?.readText()

val password: String? = System.getenv("SECRET_KEY_PASSWORD")
    ?: rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.readLines()
        ?.map { it.split("=") }
        ?.find { it.first() == "secret.password" }
        ?.get(1)


if (secretKey != null && password != null) {
    signing {
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
        maven {
            name = "Space"
            setUrl("https://packages.jetbrains.team/maven/p/kpm/public")
            credentials {
                username = System.getenv("MAVEN_SPACE_USERNAME")
                password = System.getenv("MAVEN_SPACE_PASSWORD")
            }
        }
    }

    afterEvaluate {
        publications.all {
            if (this !is MavenPublication) return@all
            artifactId = "${rootProject.name}-$artifactId"
        }
    }

}

tasks {
    check {
        dependsOn(ktlintCheck)
    }

    // workaround https://github.com/gradle/gradle/issues/26091
    withType<PublishToMavenRepository> {
        dependsOn(withType<Sign>())
    }

    withType<Test> {
        environment("DB_PATH", layout.buildDirectory.file("test.db").get().asFile.absolutePath)
        useJUnitPlatform()
    }
    withType<KotlinNativeHostTest> {
        environment("DB_PATH", layout.buildDirectory.file("test.db").get().asFile.absolutePath)
    }
}

