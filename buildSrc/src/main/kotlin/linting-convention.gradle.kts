plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

tasks {
    all {
        if (name == "check") dependsOn(ktlintCheck)
    }
}
