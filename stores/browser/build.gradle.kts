plugins {
    convention
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    js {
        browser()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                api(npm("idb-keyval", "3.0.0"))
//                api(npm("idb", "8.0.0"))
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
