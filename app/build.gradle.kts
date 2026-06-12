plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    kotlin("plugin.serialization") version "2.0.21"
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        freeCompilerArgs.addAll("-P", "plugin:org.jetbrains.kotlin.parcelize:experimentalCodeGeneration=true")
        freeCompilerArgs.add("-XXLanguage:+ContextParameters")
    }
}

android {
    namespace = "com.jeffreyalanwang.dutchrailwaysandroidclient"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.jeffreyalanwang.dutchrailwaysandroidclient"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val nav_version = "2.9.8"
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.ui.graphics)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3.v150alpha18)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.maps.android:maps-compose:8.3.0")
    implementation("ca.solo-studios:kt-fuzzy:0.1.0")
    implementation("androidx.navigation:navigation-compose:$nav_version")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.navigation:navigation-testing:$nav_version")
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.ui.tooling)
}