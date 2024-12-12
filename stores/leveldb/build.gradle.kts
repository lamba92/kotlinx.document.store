plugins {
    `publishing-convention`
    `kotlin-multiplatform-with-android-convention`
}

kotlin {
    jvm()
    androidTarget()

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

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core)
                api(libs.kotlin.leveldb)
            }
        }

        commonTest {
            dependencies {
                implementation(projects.tests)
            }
        }

        jvmTest {
            dependencies {
                runtimeOnly(libs.junit.jupiter.engine)
                implementation(libs.junit.jupiter.api)
                implementation(kotlin("test-junit5"))
            }
        }

        val nativeDesktopTest by creating {
            dependsOn(commonTest.get())
        }

        mingwTest {
            dependsOn(nativeDesktopTest)
        }

        linuxTest {
            dependsOn(nativeDesktopTest)
        }

        macosTest {
            dependsOn(nativeDesktopTest)
        }

        val appleMobileTest by creating {
            dependsOn(commonTest.get())
        }
        iosTest {
            dependsOn(appleMobileTest)
        }
        watchosTest {
            dependsOn(appleMobileTest)
        }
        tvosTest {
            dependsOn(appleMobileTest)
        }

        val commonJvmTest by creating {
            dependsOn(commonTest.get())
        }

        androidInstrumentedTest {
            dependsOn(commonJvmTest)
        }
        androidUnitTest {
            dependsOn(commonJvmTest)
        }
        jvmTest {
            dependsOn(commonJvmTest)
        }
    }
}
