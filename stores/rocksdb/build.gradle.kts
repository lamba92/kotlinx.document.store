plugins {
    kotlin("multiplatform")
    convention
}

kotlin {
    jvm()
    sourceSets {

        jvmMain {
            dependencies {
                implementation(libs.h2)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.junit.jupiter.api)
                implementation(libs.junit.jupiter.engine)
                implementation(libs.kotlinx.coroutines.test)
                implementation(kotlin("test-junit5"))
            }
        }

    }

}



