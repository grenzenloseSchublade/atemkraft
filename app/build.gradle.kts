plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "app.atemkraft"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.atemkraft"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "1.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Nur echte Geräte-ABIs mitliefern (sherpa-onnx-AAR enthält auch x86/x86_64 für Emulatoren).
        // Spart ~2/3 der Native-Libs-Größe; arm64 deckt fast alle modernen Geräte, arm32 die alten.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        // Fester Debug-Keystore im Projekt -> stabile Signatur über alle (Container-)Builds.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            // Emulatoren/ChromeOS: Debug-Builds zusätzlich mit x86_64 (Release bleibt schlank).
            ndk { abiFilters += "x86_64" }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Vorerst mit Debug-Keystore signiert, damit die minifizierte APK testbar ist.
            // (Echte Release-Signatur folgt; F-Droid signiert ohnehin selbst.)
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Neuronale Offline-TTS-Engine (Piper/VITS „Thorsten") – AAR bringt Native-Libs + Kotlin-API.
    // Bewusst als gebündeltes AAR (offline, reproduzierbar) statt Maven/JitPack.
    implementation(files("libs/sherpa-onnx-1.13.6.aar"))
    // Zum Entpacken des heruntergeladenen Stimmmodells (.tar.bz2): reines Java, keine Native-Libs.
    implementation(libs.commons.compress)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)

    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
}
