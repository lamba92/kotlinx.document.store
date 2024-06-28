plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    convention
}

kotlin {

    jvm()

    sourceSets {

        commonMain {
            dependencies {
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.coroutines.core)
            }
        }

        commonTest {
            dependencies {
                api(libs.kotlinx.coroutines.test)
                api(kotlin("test"))
            }
        }

    }

}



