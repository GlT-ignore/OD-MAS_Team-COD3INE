plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("com.chaquo.python") version "16.1.0"
}

android {
    namespace = "com.example.odmas"
    compileSdk = 36
    
        // NDK version for Chaquopy compatibility
    ndkVersion = "25.1.8937393"

    defaultConfig {
        applicationId = "com.example.odmas"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // NDK configuration for Chaquopy
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    packagingOptions {
        resources {
            excludes += setOf("META-INF/LICENSE*", "META-INF/AL2.0", "META-INF/LGPL2.1")
        }
    }
    
    // Chaquopy configuration with pre-compiled packages
    chaquopy {
        defaultConfig {
            version = "3.11"
            pip {
                // Use only pre-compiled packages that don't require compilation
                install("requests==2.31.0")
                install("urllib3==2.0.7")
                install("certifi==2023.7.22")
                install("charset-normalizer==3.3.2")
                install("idna==3.4")
            }
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    // Android 12+ splash screen compat (required for values-v31 Theme.SplashScreen attrs)
    implementation("androidx.core:core-splashscreen:1.0.1")
    // Lottie for animated splash
    implementation("com.airbnb.android:lottie-compose:6.3.0")
    // Google Fonts in Compose
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.7")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // Data storage
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Background work
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Biometrics
    implementation("androidx.biometric:biometric:1.1.0")
    
    // Sensors and usage stats
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    
    // ML - TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("com.google.android.gms:play-services-tflite-support:16.0.1")
    
    // Performance monitoring
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    
    // Chaquopy is included as a plugin, no need for separate dependency
    
    // Python packages for behavioral biometrics
    // These will be installed via pip in the Python environment
    // - scikit-learn (for ML models)
    // - numpy (for numerical computing)
    // - pandas (for data processing)
    // - scipy (for statistical analysis)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation("androidx.benchmark:benchmark-macro-junit4:1.2.3")
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}