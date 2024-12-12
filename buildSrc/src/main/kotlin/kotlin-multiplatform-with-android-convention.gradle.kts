@file:OptIn(ExperimentalPathApi::class, ExperimentalKotlinGradlePluginApi::class)

import com.android.build.gradle.internal.tasks.MergeJavaResourceTask
import com.android.build.gradle.tasks.factory.AndroidUnitTest
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import kotlin.io.path.ExperimentalPathApi

plugins {
    id("com.android.library")
    id("kotlin-multiplatform-convention")
}

android {
    namespace = "com.github.lamba92.leveldb"
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
