plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.appclonex.presentation"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
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
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    // Depends on domain
    implementation(project(":domain"))

    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    ksp("com.google.dagger:hilt-compiler:2.48")

    // Coil Compose
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Accompanist
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    implementation("com.google.accompanist:accompanist-placeholder:0.32.0")

    // Lottie
    implementation("com.airbnb.android:lottie-compose:6.3.0")

    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")

    // SplashScreen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Paging
    implementation("androidx.paging:paging-runtime-ktx:3.2.1")
    implementation("androidx.paging:paging-compose:3.2.1")

    // Glance (for widgets)
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("androidx.glance:glance-material3:1.0.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // Immutable Collections
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7")

    // AppCompat
    implementation("androidx.appcompat:appcompat:1.6.1")
}
