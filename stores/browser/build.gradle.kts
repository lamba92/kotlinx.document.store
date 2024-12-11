plugins {
    `publishing-convention`
    `kotlin-multiplatform-convention`
}

kotlin {
    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }
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
