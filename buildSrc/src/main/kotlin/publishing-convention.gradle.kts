import gradle.kotlin.dsl.accessors._5110d0ad46c3465a3034c0fe268105a5.kotlin
import kotlin.io.path.Path
import kotlin.io.path.readText
import org.gradle.internal.os.OperatingSystem

plugins {
    id("org.jlleitschuh.gradle.ktlint")
    `maven-publish`
    signing
}

val kotlinPlugins = listOf(
    "org.jetbrains.kotlin.jvm",
    "org.jetbrains.kotlin.multiplatform"
)

kotlinPlugins.forEach { kotlinPluginId ->
    plugins.withId(kotlinPluginId) {
        plugins.withId("org.jetbrains.dokka") {
            val javadocJar by tasks.registering(Jar::class) {
                dependsOn("dokkaGeneratePublicationHtml")
                archiveClassifier = "javadoc"
                from("dokkaGeneratePublicationHtml")
                destinationDirectory = layout.buildDirectory.dir("artifacts")
            }
            publishing {
                publications.withType<MavenPublication> {
                    artifact(javadocJar)
                }
            }
            if ("jvm" in kotlinPluginId) {
                val sourcesJar by tasks.registering(Jar::class) {
                    archiveClassifier = "sources"
                    from(kotlin.sourceSets.getByName("main").kotlin)
                    destinationDirectory = layout.buildDirectory.dir("artifacts")
                }
                publishing {
                    publications.withType<MavenPublication> {
                        artifact(sourcesJar)
                    }
                }
            }
        }
    }
}

publishing {
    repositories {
        maven(rootProject.layout.buildDirectory.dir("mavenRepo")) {
            name = "test"
        }
    }
    publications.withType<MavenPublication> {
        artifactId = "kotlin-document-store-$artifactId"
        pom {
            name = "kotlin-document-store"
            description = "Kotlin Multiplatform NoSQL document storage"
            url = "https://github.com/lamba92/kotlin.document.store"
            licenses {
                license {
                    name = "Apache-2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            developers {
                developer {
                    id = "lamba92"
                    name = "Lamberto Basti"
                    email = "basti.lamberto@gmail.com"
                }
            }
            scm {
                connection = "https://github.com/lamba92/kotlin.document.store.git"
                developerConnection = "https://github.com/lamba92/kotlin.document.store.git"
                url = "https://github.com/lamba92/kotlin.document.store.git"
            }
        }
    }
}

signing {
    val privateKey =
        System.getenv("SIGNING_PRIVATE_KEY")
            ?: project.properties["central.signing.privateKeyPath"]
                ?.let { it as? String }
                ?.let { Path(it).readText() }
            ?: return@signing
    val password =
        System.getenv("SIGNING_PASSWORD")
            ?: project.properties["central.signing.privateKeyPassword"] as? String
            ?: return@signing
    logger.lifecycle("Signing enabled")
    useInMemoryPgpKeys(privateKey, password)
    sign(publishing.publications)
}

tasks {

    // workaround https://github.com/gradle/gradle/issues/26091
    withType<PublishToMavenRepository> {
        dependsOn(withType<Sign>())
    }

    // in CI we only want to publish the artifacts for the current OS only
    // but when developing we want to publish all the possible artifacts to test them
    if (isCi) {

        val linuxNames = listOf("linux", "android", "jvm", "js", "kotlin", "metadata", "wasm")
        val windowsNames = listOf("mingw", "windows")
        val appleNames = listOf("macos", "ios", "watchos", "tvos")
        val currentOs: OperatingSystem = OperatingSystem.current()

        withType<AbstractPublishToMaven> {
            when {
                name.containsAny(linuxNames) -> onlyIf { currentOs.isLinux }
                name.containsAny(windowsNames) -> onlyIf { currentOs.isWindows }
                name.containsAny(appleNames) -> onlyIf { currentOs.isMacOsX }
            }
        }
    }
}

val isCi
    get() = System.getenv("CI") == "true"

fun String.containsAny(strings: List<String>, ignoreCase: Boolean = true): Boolean =
    strings.any { contains(it, ignoreCase) }