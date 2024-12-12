plugins {
    `publishing-convention`
    `kotlin-multiplatform-convention`
    id("org.jlleitschuh.gradle.ktlint")
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
        jsMain {
            dependencies {
                api(npm("idb-keyval", "6.2.1"))
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
