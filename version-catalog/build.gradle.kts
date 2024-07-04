plugins {
    convention
    `version-catalog`
    `maven-publish`
}

catalog {
    versionCatalog {
        from(files("gradle/libs.versions.toml"))
    }
}

publishing {
    publications {
        create<MavenPublication>(rootProject.name) {
            artifactId = "${rootProject.name}-version-catalog"
            from(components["versionCatalog"])
        }
    }
}