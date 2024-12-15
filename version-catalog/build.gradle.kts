import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

plugins {
    `publishing-convention`
    `version-catalog`
    versions
}

val output = layout.buildDirectory.file("libs.versions.toml")
val replaceVersion by tasks.registering {
    val input = file("libs.versions.toml")
    inputs.file(input)
    outputs.file(output)
    doLast {
        output.get()
            .asPath
            .apply { parent.createDirectories() }
            .writeText(
                input
                    .readText()
                    .replace(
                        oldValue = "%%%VERSION%%%",
                        newValue = project.version.toString(),
                    ),
            )
    }
}

tasks.generateCatalogAsToml {
    dependsOn(replaceVersion)
}

catalog {
    versionCatalog {
        from(files(output))
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn(replaceVersion)
    archiveClassifier = "sources"
    from(replaceVersion)
    destinationDirectory = layout.buildDirectory.dir("artifacts")
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(replaceVersion)
    archiveClassifier = "javadoc"
    from(replaceVersion)
    destinationDirectory = layout.buildDirectory.dir("artifacts")
}

publishing {
    publications {
        create<MavenPublication>(rootProject.name) {
            from(components["versionCatalog"])
            artifact(sourcesJar)
            artifact(javadocJar)
        }
    }
}
