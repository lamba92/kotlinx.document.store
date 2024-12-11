import kotlin.io.path.Path
import kotlin.io.path.readText

plugins {
    id("org.jetbrains.dokka")
    id("org.jlleitschuh.gradle.ktlint")
    `maven-publish`
    signing
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaGeneratePublicationHtml)
    archiveClassifier = "javadoc"
    from(tasks.dokkaGeneratePublicationHtml)
    destinationDirectory = layout.buildDirectory.dir("artifacts")
}

publishing {
    repositories {
        maven(rootProject.layout.buildDirectory.dir("mavenRepo")) {
            name = "test"
        }
    }
    publications.withType<MavenPublication> {

        // the publishing plugin is old AF and does not support lazy
        // properties, so we need to set the artifactId after the
        // publication is created
        afterEvaluate { artifactId = "kotlinx-document-store-$artifactId" }

        artifact(javadocJar)
        pom {
            name = "kotlin-document-store"
            description = "Kotlin Multiplatform NoSQL document storage"
            url = "https://github.com/lamba92/kotlin-document-store"
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
                connection = "https://github.com/lamba92/kotlinx-document-store.git"
                developerConnection = "https://github.com/lamba92/kotlinx-document-store.git"
                url = "https://github.com/lamba92/kotlinx-document-store.git"
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
    useInMemoryPgpKeys(privateKey, password)
    sign(publishing.publications)
}

tasks {

    // workaround https://github.com/gradle/gradle/issues/26091
    withType<PublishToMavenRepository> {
        dependsOn(withType<Sign>())
    }
}
