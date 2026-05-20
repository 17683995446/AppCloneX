# AppCloneX ProGuard Rules - Release Build Optimization
# V21 - Enhanced for Google Play Release
# V25 - Added WebDAV, Plugin System, Notification, Kotlin Serialization rules

# =====================================================
# 基础规则 Basic Rules
# =====================================================

# 保留行号信息用于崩溃报告
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 保留注解信息
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepattributes Signature, Exceptions, InnerClasses

# =====================================================
# AndroidX & Jetpack
# =====================================================

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# DataStore
-keepclassmembers class androidx.datastore.preferences.core.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn dagger.hilt.**

# Navigation
-keep class androidx.navigation.** { *; }

# WorkManager
-keep class androidx.work.** { *; }
-keepclassmembers class * extends androidx.work.Worker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Biometric
-keep class androidx.biometric.** { *; }

# Security (EncryptedSharedPreferences)
-keep class androidx.security.** { *; }

# Glance Widget
-keep class androidx.glance.** { *; }

# SplashScreen
-keep class androidx.core.splashscreen.** { *; }

# =====================================================
# Kotlin
# =====================================================

# Kotlin Metadata
-dontwarn kotlin.reflect.jvm.internal.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }

# =====================================================
# Third-Party Libraries
# =====================================================

# Shizuku (Critical - reflection-based)
-keep class rikka.shizuku.** { *; }
-keep class dev.rikka.shizuku.** { *; }
-keep class dev.rikka.tools.hiddenapi.** { *; }
-keep class com.appclonex.infrastructure.external.shizuku.** { *; }

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Timber
-keep class timber.log.** { *; }
-dontwarn timber.log.**

# Pinyin4j
-keep class net.sourceforge.pinyin4j.** { *; }

# Moshi
-keep @com.squareup.moshi.JsonClass class *
-keepclassmembers class ** {
    @com.squareup.moshi.Json <fields>;
}
-dontnote com.squareup.moshi.**

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okio.**
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
-keep class okhttp3.** { *; }

# Lottie
-keep class com.airbnb.lottie.** { *; }

# Accompanist
-keep class com.google.accompanist.** { *; }

# Store 5
-keep class org.mobilenativefoundation.store.** { *; }

# Paging
-keep class androidx.paging.** { *; }

# kotlin-result
-keep class com.github.michaelbull.result.** { *; }

# Firebase
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }

# =====================================================
# AppCloneX Application Classes
# =====================================================

# Domain Models (Critical - used by Room via reflection)
-keep class com.appclonex.domain.model.** { *; }
-keep class com.appclonex.domain.repository.** { *; }
-keep class com.appclonex.domain.usecase.** { *; }

# Infrastructure
-keep class com.appclonex.infrastructure.external.shizuku.** { *; }
-keep class com.appclonex.infrastructure.persistence.database.** { *; }
-keep class com.appclonex.infrastructure.persistence.datastore.** { *; }
-keep class com.appclonex.infrastructure.repository.** { *; }

# Application
-keep class com.appclonex.application.AppCloneXApplication { *; }

# DTOs for JSON parsing
-keep class com.appclonex.infrastructure.network.dto.** { *; }

# =====================================================
# Optimization Rules
# =====================================================

# 启用 R8 全模式优化
# R8 full mode optimizations
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# 移除空构造函数
-assumenosideeffects class kotlin.jvm.internal.DefaultConstructorMarker {
    <init>();
}

# 优化枚举
-optimizations !class/unboxing/enum

# 保留测试相关（仅在测试构建中排除）
-dontwarn android.test.**
-dontwarn androidx.test.**

# =====================================================
# Play Store & Security Requirements
# =====================================================

# 保留崩溃报告所需的行号
-keepattributes SourceFile,LineNumberTable

# 保留泛型签名（用于反射）
-keepattributes Signature

# 保留异常信息
-keepattributes Exceptions

# 保留内部类信息
-keepattributes InnerClasses, EnclosingMethod

# 保留注解默认值
-keepattributes AnnotationDefault

# =====================================================
# V25: WebDAV / Nextcloud
# =====================================================

-keep class com.appclonex.domain.model.WebDavModels** { *; }
-keep class com.appclonex.domain.model.WebDav* { *; }
-keep class com.appclonex.domain.model.Nextcloud* { *; }

# =====================================================
# V25: Plugin System
# =====================================================

-keep class com.appclonex.domain.model.PluginSystem** { *; }
-keep class com.appclonex.domain.model.Plugin* { *; }
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class * { @kotlinx.serialization.Serializable <fields>; }

# =====================================================
# V25: Notification System
# =====================================================

-keep class com.appclonex.domain.model.NotificationModels** { *; }
-keep class com.appclonex.domain.model.Notification* { *; }
-keep class com.appclonex.domain.model.AppNotification* { *; }

# =====================================================
# V25: Google Play Services
# =====================================================

-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.** { *; }
-dontwarn com.google.android.gms.**
-keepclassmembers class com.google.android.gms.common.api.internal.zzag { *; }

# =====================================================
# V25: Kotlin Serialization (通用)
# =====================================================

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# 保留所有 @Serializable 注解的类及其字段
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# 保留序列化器伴生对象
-keep,includedescriptorclasses class com.appclonex.**$$serializer { *; }
-keepclassmembers class com.appclonex.** {
    *** Companion;
}
-keepclasseswithmembers class com.appclonex.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 保留自定义序列化器类（防止 R8 混淆）
-keep class com.appclonex.domain.model.AppNotificationIdSerializer { *; }
-keep class com.appclonex.domain.model.AppNotificationId { *; }
-keep class com.appclonex.domain.model.PluginId { *; }
-keep class com.appclonex.domain.model.PluginId$$serializer { *; }

# =====================================================
# V25: Chart.js (如果使用 WebView)
# =====================================================

-keep class com.appclonex.presentation.screens.statistics.charts.** { *; }
