@file:Suppress("OPT_IN_USAGE")

import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    convention
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.power-assert")
}

kotlin {

    jvm()
    js {
        browser()
    }
    macosArm64()
    macosX64()
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    explicitApi = ExplicitApiMode.Disabled

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

powerAssert {
    functions =
        setOf(
            "kotlin.test.assertEquals",
        )
}
