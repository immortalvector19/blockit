plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.blockit"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.blockit"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // Uses the standard Android keystore so that FocusShield.apk is fully signed,
            // valid, and installable on user devices without signature verification failures.
            initWith(getByName("debug"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    applicationVariants.all {
        if (buildType.name == "release") {
            outputs.all {
                val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
                output?.outputFileName = "FocusShield.apk"
            }
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Core Kotlin extensions for Android system APIs
    implementation("androidx.core:core-ktx:1.12.0")

    // Official Android 12+ Splash Screen API for seamless zero-flicker startup
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Baseline Profile installer for ahead-of-time (AOT) app startup optimization
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")

    // Lifecycle aware scopes and Coroutine support
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // ViewModel integration with Jetpack Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // ComponentActivity and Compose setContent support
    implementation("androidx.activity:activity-compose:1.8.2")

    // Core Jetpack Compose UI primitives and rendering
    implementation("androidx.compose.ui:ui:1.6.5")

    // Compose Graphics utilities and canvas operations
    implementation("androidx.compose.ui:ui-graphics:1.6.5")

    // Preview support for Compose composables in IDE
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.5")

    // Material Design 3 UI component system
    implementation("androidx.compose.material3:material3:1.2.1")

    // Extended Material icon library for UI icons
    implementation("androidx.compose.material:material-icons-extended:1.6.5")

    // Type-safe navigation framework for Compose destinations
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // SQLite persistence layer and ORM runtime
    implementation("androidx.room:room-runtime:2.6.1")

    // Coroutines and Flow extensions for Room queries
    implementation("androidx.room:room-ktx:2.6.1")

    // Kotlin Symbol Processing compiler for Room DAOs
    ksp("androidx.room:room-compiler:2.6.1")

    // Type-safe asynchronous key-value persistence for settings
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Periodic background watchdog jobs that survive system reboots
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Unit test framework for domain logic and matcher tests
    testImplementation("junit:junit:4.13.2")

    // Coroutine testing dispatcher and runTest harness
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

    // Standalone JSON implementation for JVM unit tests to avoid Android runtime stubs
    testImplementation("org.json:json:20231013")

    // Android JUnit test runner extensions
    androidTestImplementation("androidx.test.ext:junit:1.1.5")

    // UI and service instrumentation test verification
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Compose UI testing framework for composable assertions
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.5")

    // Debug tooling inspector for Compose layouts
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.5")

    // Test manifest integration for Compose activity tests
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.5")
}
