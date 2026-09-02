plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.triviaquiz"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.triviaquiz"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isDebuggable = false
            // Enable R8 full-mode shrinking/obfuscation for a production-ready, smaller APK.
            // Keep-rule files under src/main/keepRules are picked up automatically by AGP.
            optimization {
                enable = true
            }
            // Sign the release build with the debug keystore so it can be tested locally.
            // Replace with a real signing config before publishing to the Play Store.
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Enable View Binding — generates ActivityMainBinding class
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.cardview)

    // Kotlin Coroutines for background networking
    implementation(libs.kotlinx.coroutines.android)

    // Lifecycle runtime for lifecycleScope.launch
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
