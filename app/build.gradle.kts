import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.ktlint)
}

// Release-Keystore + Passwörter liegen bewusst außerhalb des Repos (gitignored).
// Suchreihenfolge: Pfad aus ATEMKRAFT_KEYSTORE_PROPERTIES (CI, anderer Rechner),
// sonst keystore.properties im Projekt-Root (lokaler Maintainer-Checkout).
// Ist die Variable gesetzt, die Datei aber nicht da, bricht der Build ab: Wer explizit
// einen Keystore verlangt, soll nicht stillschweigend etwas anderes bekommen.
val keystoreEnvPath = providers.environmentVariable("ATEMKRAFT_KEYSTORE_PROPERTIES").orNull
    ?.takeIf { it.isNotBlank() }
val keystorePropsFile = if (keystoreEnvPath != null) {
    File(keystoreEnvPath).also {
        if (!it.isFile) {
            throw GradleException(
                "ATEMKRAFT_KEYSTORE_PROPERTIES zeigt auf '$keystoreEnvPath', die Datei existiert nicht.",
            )
        }
    }
} else {
    rootProject.file("keystore.properties").takeIf { it.isFile }
}
val keystoreProps = keystorePropsFile
    ?.let { f -> Properties().apply { f.inputStream().use { load(it) } } }
// Nur für lokale Tests minifizierter Builds ohne Release-Key: -PallowDebugSignedRelease=true.
// Bewusst Opt-in, damit eine debug-signierte APK nie versehentlich als Release rausgeht.
val allowDebugSignedRelease = providers.gradleProperty("allowDebugSignedRelease").orNull == "true"

android {
    namespace = "app.atemkraft"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.atemkraft"
        minSdk = 26
        targetSdk = 36
        versionCode = 17
        versionName = "1.5.1"
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
        if (keystoreProps != null && keystorePropsFile != null) {
            fun prop(key: String): String = keystoreProps.getProperty(key)?.takeIf { it.isNotBlank() }
                ?: throw GradleException("'$key' fehlt in ${keystorePropsFile.path}.")
            create("release") {
                // Relativer storeFile: bei keystore.properties im Root wie bisher relativ zu app/,
                // bei ATEMKRAFT_KEYSTORE_PROPERTIES relativ zur Properties-Datei (liegt dort meist daneben).
                val storePath = prop("storeFile")
                storeFile = if (keystoreEnvPath != null && !File(storePath).isAbsolute) {
                    File(keystorePropsFile.absoluteFile.parentFile, storePath)
                } else {
                    file(storePath)
                }
                storePassword = prop("storePassword")
                keyAlias = prop("keyAlias")
                keyPassword = prop("keyPassword")
            }
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
            // Echter Release-Key, wenn gefunden. Sonst UNSIGNIERT (app-release-unsigned.apk):
            // Genau das erwartet F-Droid, das selbst signiert. Ein stiller Debug-Key-Fallback
            // würde Release-Artefakte mit öffentlich bekanntem Schlüssel erzeugen.
            signingConfig = when {
                signingConfigs.findByName("release") != null -> signingConfigs.getByName("release")
                allowDebugSignedRelease -> signingConfigs.getByName("debug")
                else -> null
            }
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

    // Android Lint als Gate (docs/STYLEGUIDE.md §13.1): jede Warnung ist ein Fehler. Aus sind
    // nur Versions-Hinweise (Updates laufen über Dependabot) und ChromeOS-ABIs (bewusst nur ARM).
    lint {
        abortOnError = true
        warningsAsErrors = true
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable", "ChromeOsAbiSupport")
    }

    // Screenshot-Tests (Robolectric + Roborazzi) brauchen echte Ressourcen statt Stubs.
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Hohe Screenshots (bis 8000 dp bei fontScale 2,0) brauchen mehr als den Standard-Heap.
        unitTests.all { it.maxHeapSize = "4g" }
    }
}

// ktlint mit Compose-Regeln (docs/STYLEGUIDE.md CODE-04, CODE-06); Regeln in .editorconfig,
// bekannte Compose-Funde in der Baseline (dürfen nur weniger werden).
ktlint {
    version.set(libs.versions.ktlint)
    baseline.set(rootProject.file("config/ktlint-baseline.xml"))
}

// Regeltests lesen Baselines und Wortlisten aus config/: als Test-Eingabe registrieren, sonst
// hält Gradle die Tests nach einer reinen Baseline-Änderung für aktuell und überspringt sie.
tasks.withType<Test>().configureEach {
    inputs.dir(rootProject.file("config"))
        .withPropertyName("regelConfig")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

// Deutliche Warnung beim Paketieren statt beim Konfigurieren: So erscheint sie nur bei
// Release-Builds (nicht bei jedem assembleDebug) und auch bei Configuration-Cache-Treffern.
if (keystoreProps == null) {
    val releaseSigningWarning = if (allowDebugSignedRelease) {
        "WARNUNG: Release wird mit dem DEBUG-Key signiert (-PallowDebugSignedRelease=true). " +
            "Nur für lokale Tests, nicht verteilen."
    } else {
        "WARNUNG: Kein Release-Keystore gefunden (ATEMKRAFT_KEYSTORE_PROPERTIES / keystore.properties). " +
            "Release wird UNSIGNIERT gebaut (app-release-unsigned.apk) – passend für F-Droid, " +
            "nicht installierbar. Für einen debug-signierten Test-Build: -PallowDebugSignedRelease=true."
    }
    tasks.matching { it.name == "packageRelease" || it.name == "bundleRelease" }.configureEach {
        doFirst { logger.warn(releaseSigningWarning) }
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
    ktlintRuleset(libs.compose.rules.ktlint)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
