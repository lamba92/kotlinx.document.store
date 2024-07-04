plugins {
    kotlin("multiplatform")
    convention
}

kotlin {
    jvm()
    macosArm64()
    macosX64()
    iosArm64()
    iosX64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core)
                api(libs.rocksdb.multiplatform)
                api("org.jetbrains.kotlinx:kotlinx-io-core:0.3.5")
            }
        }

        commonTest {
            dependencies {
                implementation(projects.tests)
            }
        }

    }

}
repositories {
    mavenCentral()
}



