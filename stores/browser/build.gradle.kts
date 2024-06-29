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
//                implementation(libs.kotlin.browser)
            }
        }
        jsMain {
            dependencies {
                api(projects.core)
            }
        }
        jsTest {
            dependencies {
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
