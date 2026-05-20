plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.appclonex.domain"
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
}

dependencies {
    // Domain layer has NO dependencies on other modules
    // Only pure Kotlin and Android framework

    // Coroutines for Flow
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Kotlinx Serialization for JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Room for entity annotations (optional if entities stay in domain)
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Dagger Hilt for @Inject, @Singleton
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
}
