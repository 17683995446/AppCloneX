plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.appclonex"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.appclonex"
        minSdk = 24
        targetSdk = 34
        versionCode = 25
        versionName = "2.5.0"

        // Room schema 导出
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    signingConfigs {
        create("release") {
            // 从环境变量或本地属性读取签名配置
            // Read signing config from environment or local properties
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: ""
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            val keyAlias = System.getenv("KEY_ALIAS") ?: ""
            val keyPassword = System.getenv("KEY_PASSWORD") ?: ""

            if (keystorePath.isNotEmpty() && keystorePassword.isNotEmpty()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // App Bundle 配置
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
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
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Lint 配置
    lint {
        disable += listOf("ObsoleteLintCustomCheck", "MissingTranslation")
        abortOnError = true
        warningsAsErrors = true
        checkReleaseBuilds = true
        checkDependencies = true
    }
}

dependencies {
    // ==================== Module Dependencies ====================
    implementation(project(":domain"))
    implementation(project(":infrastructure"))
    implementation(project(":presentation"))

    // ==================== AndroidX Core ====================
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")

    // ==================== Jetpack Compose ====================
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ==================== Hilt 依赖注入 ====================
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // ==================== Room 数据库 ====================
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ==================== Shizuku ====================
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    // Hidden API stub（编译 IPackageInstallObserver 等隐藏类）
    compileOnly("dev.rikka.tools.hiddenapi:stub:4.3.0")

    // ==================== 协程 ====================
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ==================== DataStore ====================
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // ==================== 图片加载 (替代手写 AppIconCache) ====================
    implementation("io.coil-kt:coil-compose:2.5.0")

    // ==================== 日志 (替代手写 LogManager) ====================
    implementation("com.jakewharton.timber:timber:5.0.1")

    // ==================== 拼音转换 ====================
    implementation("com.belerweb:pinyin4j:2.5.1")

    // ==================== Glance 小组件 ====================
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("androidx.glance:glance-material3:1.0.0")

    // ==================== WorkManager 后台任务 ====================
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // ==================== OkHttp 网络请求 ====================
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:okhttp")

    // ==================== Moshi JSON解析 (用于 Retrofit) ====================
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")

    // ==================== 调试工具 ====================
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ==================== Kotlinx DateTime ====================
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // ==================== LeakCanary 内存泄漏检测 ====================
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")

    // ==================== Startup 初始化优化 ====================
    implementation("androidx.startup:startup-runtime:1.1.1")

    // ==================== AppCompat 兼容性 ====================
    implementation("androidx.appcompat:appcompat:1.6.1")

    // ==================== Accompanist Compose辅助库 ====================
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    implementation("com.google.accompanist:accompanist-placeholder:0.32.0")

    // ==================== Palette 图片取色 ====================
    implementation("androidx.palette:palette-ktx:1.0.0")

    // ==================== Result 函数式错误处理 ====================
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.18")
    implementation("com.michael-bull.kotlin-result:kotlin-result-coroutines:1.1.18")

    // ==================== Kotlinx Immutable Collections ====================
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7")

    // ==================== Lottie 动画 ====================
    implementation("com.airbnb.android:lottie-compose:6.3.0")

    // ==================== Coil SVG/GIF 支持 ====================
    implementation("io.coil-kt:coil-svg:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")

    // ==================== Security 安全存储 ====================
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ==================== Biometric 生物识别 ====================
    implementation("androidx.biometric:biometric:1.1.0")

    // ==================== SplashScreen 启动屏 ====================
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ==================== Retrofit 网络请求 ====================
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor")

    // ==================== Paging 3 分页加载 ====================
    implementation("androidx.paging:paging-runtime-ktx:3.2.1")
    implementation("androidx.paging:paging-compose:3.2.1")

    // ==================== Store 5 数据缓存 ====================
    implementation("org.mobilenativefoundation.store:store5:5.0.0")

    // ==================== 依赖注入优化 ====================
    implementation("com.google.dagger:hilt-core:2.48")

    // ==================== Firebase ====================
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // ==================== 测试 ====================
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("com.google.truth:truth:1.4.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
}
