import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    convention
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    js {
        browser()
    }
    explicitApi = ExplicitApiMode.Disabled
    sourceSets {
        val jsMain by getting {
            dependencies {
                api(npm("idb-keyval", "6.2.1"))
            }
        }
        jsMain {
            dependencies {
                api(projects.core)
            }
        }
        jsTest {
            dependencies {
                implementation(projects.tests)
            }
        }
    }
}
