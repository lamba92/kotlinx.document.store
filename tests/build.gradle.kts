@file:Suppress("OPT_IN_USAGE")

plugins {
    `publishing-convention`
    `kotlin-multiplatform-convention`
    kotlin("plugin.power-assert")
}

kotlin {
    jvm()
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
                api(libs.junit.jupiter.api)
                api(kotlin("test-junit5"))
            }
        }
    }
}

powerAssert {
    functions = setOf("kotlin.test.assertEquals")
}
