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
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.coroutines.core)
            }
        }

    }

}



