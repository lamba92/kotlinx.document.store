plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm()
    js {
        browser()
        nodejs()
    }
    macosX64()
    macosArm64()
    iosArm64()
    iosX64()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core)
                api(libs.kotlinx.serialization.core)
            }
        }
    }
}
