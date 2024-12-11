plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.github.node-gradle.node") version "7.1.0"
}

kotlin {
    js {
        nodejs()
        browser()
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.samples)
                api(libs.ktor.client.content.negotiation)
                api(libs.ktor.serialization.kotlinx.json)
                api(libs.kotlinx.datetime)
            }
        }
        jsMain {
            dependencies {
                api(libs.ktor.client.js)
                api(projects.stores.browser)
            }
        }
    }
}
