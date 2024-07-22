plugins {
    kotlin("multiplatform")
    convention
}

kotlin {

    jvm()

    if (System.getenv("CI") != "true") {
        macosArm64()
        macosX64()
        iosArm64()
        iosX64()
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core)
                api(libs.rocksdb.multiplatform)
                api(libs.kotlinx.io.core)
            }
        }

        commonTest {
            dependencies {
                implementation(projects.tests)
            }
        }
    }
}
