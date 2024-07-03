plugins {
    convention
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {

    jvm()
    js {
        browser()
    }

    sourceSets {

        commonMain {
            dependencies {
                api(projects.core)
                api(kotlin("test"))
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.coroutines.test)
            }
        }

        jvmMain {
            dependencies {
                api(kotlin("test-junit5"))
            }
        }

        jsMain {
            dependencies {
                api(kotlin("test-js"))
            }
        }
    }

}



