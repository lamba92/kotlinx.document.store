plugins {
    convention
    `version-catalog`
    `maven-publish`
}

catalog {
    versionCatalog {
        from(files("libs.versions.toml"))
    }
}

publishing {
    publications {
        create<MavenPublication>(rootProject.name) {
            from(components["versionCatalog"])
        }
    }
}
