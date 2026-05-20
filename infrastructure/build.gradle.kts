plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.appclonex.infrastructure"
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
    // Depends on domain
    implementation(project(":domain"))

    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Shizuku
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    compileOnly("dev.rikka.tools.hiddenapi:stub:4.3.0")

    // Retrofit for Shizuku API (if needed)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // OkHttp (BOM-managed)
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:okhttp")

    // Moshi
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")

    // Coil for image loading
    implementation("io.coil-kt:coil:2.5.0")

    // Timber for logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // Security (1.1.0-alpha06 is the latest available version; no stable 1.1.0 release yet)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Biometric (upgraded from 1.2.0-alpha05 to stable 1.1.0)
    implementation("androidx.biometric:biometric:1.1.0")

    // Pinyin
    implementation("com.belerweb:pinyin4j:2.5.1")

    // Startup
    implementation("androidx.startup:startup-runtime:1.1.1")

    // Google Play Services BOM
    implementation(platform("com.google.android.gms:play-services-bom:18.2.0"))
    implementation("com.google.android.gms:play-services-drive")
    implementation("com.google.android.gms:play-services-auth")

    // Glance
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("androidx.glance:glance-material3:1.0.0")
}
