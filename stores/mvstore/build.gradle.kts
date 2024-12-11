plugins {
    `publishing-convention`
    `kotlin-jvm-convention`
}

dependencies {
    api(libs.h2)
    api(projects.core)
    testImplementation(projects.tests)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.datetime)
    testImplementation(kotlin("test-junit5"))
}

publishing.publications.register<MavenPublication>("main") {
    from(components["kotlin"])
}
