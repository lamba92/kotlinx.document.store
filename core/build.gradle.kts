plugins {
    `publishing-convention`
    `kotlin-multiplatform-with-android-convention`
}

kotlin {
    jvm()
    androidTarget()
    js {
        browser()
    }
    mingwX64()

    androidTarget()

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

    applyDefaultHierarchyTemplate()

    sourceSets {

        commonMain {
            dependencies {
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.io.core)
            }
        }
    }
}
