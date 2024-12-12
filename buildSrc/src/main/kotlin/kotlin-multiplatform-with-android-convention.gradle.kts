@file:OptIn(ExperimentalPathApi::class, ExperimentalKotlinGradlePluginApi::class)

import com.android.build.gradle.tasks.factory.AndroidUnitTest
import gradle.kotlin.dsl.accessors._f5ffce11a4b5604b3d89b5ef03ba37e3.android
import kotlin.io.path.ExperimentalPathApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

plugins {
    id("com.android.library")
    id("kotlin-multiplatform-convention")
}

android {
    compileSdk = 35
    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            excludes += setOf("**/*.md")
        }
    }
}

kotlin {
    targets.withType<KotlinAndroidTarget> {
        publishLibraryVariants("release")

        // KT-46452 Allow to run common tests as Android Instrumentation tests
        // https://youtrack.jetbrains.com/issue/KT-46452
        instrumentedTestVariant {
            sourceSetTree = KotlinSourceSetTree.test
        }
    }
}

tasks {

    // This project will test only instrumentation tests
    withType<AndroidUnitTest> {
        onlyIf { false }
    }

}
