import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

plugins {
    versions
    `version-catalog`
    `maven-publish`
}

val output = layout.buildDirectory.file("libs.versions.toml")
val replaceVersion by tasks.registering {
    val input = file("libs.versions.toml")
    inputs.file(input)
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

publishing {
    publications {
        create<MavenPublication>(rootProject.name) {
            from(components["versionCatalog"])
        }
    }
}
