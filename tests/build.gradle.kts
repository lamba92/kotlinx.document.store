@file:Suppress("OPT_IN_USAGE")

plugins {
    `publishing-convention`
    `kotlin-multiplatform-with-android-convention`
    kotlin("plugin.power-assert")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    namespace = "kotlinx.document.store.tests"
}

kotlin {
    jvm()
    androidTarget()
    js {
        browser()
    }

    mingwX64()

    linuxX64()
    linuxArm64()

    macosArm64()
    macosX64()

    iosArm64()
    iosX64()
    iosSimulatorArm64()

    watchosArm64()
    watchosX64()
    watchosSimulatorArm64()

    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    androidNativeX64()
    androidNativeX86()
    androidNativeArm64()
    androidNativeArm32()

    wasmWasi {
        nodejs()
    }

    sourceSets {

        commonMain {
            dependencies {
                api(projects.core)
                api(kotlin("test"))
                api(libs.kotlinx.coroutines.test)
                api(libs.kotlinx.io.core)
            }
        }

        jvmMain {
            dependencies {
                api(libs.junit.jupiter.api)
                api(kotlin("test-junit5"))
            }
        }

        androidMain {
            dependencies {
                api(libs.androidx.test.runner)
                api(libs.androidx.test.core)
                api(libs.android.test.junit)
            }
        }
    }
}

powerAssert {
    functions = setOf("kotlin.test.assertEquals")
}
