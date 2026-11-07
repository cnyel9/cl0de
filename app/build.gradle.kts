plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.cl0de.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.cl0de.app"
        minSdk = 26
        // 28 on purpose: proot runs binaries from the app data directory,
        // and with targetSdk >= 29 Android forbids exec from app_data_file (same as Termux).
        // The APK is distributed as a file, not through Play.
        targetSdk = 28
        versionCode = 9
        versionName = "0.0.1 tester2"
        ndk { abiFilters += "arm64-v8a" }
    }

    // targetSdk 28: see above; not an error for an APK outside Play
    lint { disable += "ExpiredTargetSdkVersion" }

    signingConfigs {
        // Fixed release key, supplied by CI from repo secrets (env vars below).
        // Without the env vars this config stays empty and release falls back to the debug key.
        create("fixed") {
            val ksPath = System.getenv("SIGNING_KEYSTORE_PATH")
            if (ksPath != null) {
                storeFile = file(ksPath)
                storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: "cl0de"
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Fixed key from CI secrets when present (same key every build, so updates
            // install over the previous version); debug key only as a local fallback.
            signingConfig = if (System.getenv("SIGNING_KEYSTORE_PATH") != null) {
                signingConfigs.getByName("fixed")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures { compose = true; buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        // proot and its libraries ship as lib*.so and must be extracted to disk,
        // otherwise they can't be run as executables.
        jniLibs.useLegacyPackaging = true
        jniLibs.keepDebugSymbols += "**/*.so"
        resources.excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin")
    }

    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.gms.code.scanner)
    implementation(libs.androidx.fragment)
    // Offline speech recognition (sherpa-onnx v2.0.1, official release AAR as a
    // local file: deterministic for CI, unlike JitPack. Only arm64-v8a is packed,
    // see the abiFilters in defaultConfig.)
    implementation(files("libs/sherpa-onnx-2.0.1.aar"))

    testImplementation(libs.junit)
}
