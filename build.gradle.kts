plugins {
    versions
    id("io.github.gradle-nexus.publish-plugin")
}

nexusPublishing {
    // repositoryDescription is used by the nexus publish plugin as identifier
    // for the repository to publish to.
    val repoDesc =
        System.getenv("SONATYPE_REPOSITORY_DESCRIPTION")
            ?: project.properties["central.sonatype.repositoryDescription"] as? String
    repoDesc?.let { repositoryDescription = it }

    repositories {
        sonatype {
            username = System.getenv("SONATYPE_USERNAME")
                ?: project.properties["central.sonatype.username"] as? String
            password = System.getenv("SONATYPE_PASSWORD")
                ?: project.properties["central.sonatype.password"] as? String
        }
    }
}
