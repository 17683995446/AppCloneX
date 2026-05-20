package com.appclonex.infrastructure.logging

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Analytics 事件追踪器
 *
 * 封装 Firebase Analytics，提供类型安全的事件追踪接口
 * 所有用户行为事件通过此组件统一上报
 */
@Singleton
class FirebaseAnalyticsEventTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    /**
     * 追踪应用克隆操作
     */
    fun trackCloneApp(packageName: String, profileId: Int, success: Boolean) {
        firebaseAnalytics.logEvent("app_clone") {
            param("package_name", packageName)
            param("profile_id", profileId.toLong())
            param("success", success)
        }
    }

    /**
     * 追踪应用删除操作
     */
    fun trackDeleteApp(packageName: String, profileId: Int) {
        firebaseAnalytics.logEvent("app_delete") {
            param("package_name", packageName)
            param("profile_id", profileId.toLong())
        }
    }

    /**
     * 追踪应用启动操作
     */
    fun trackLaunchApp(packageName: String) {
        firebaseAnalytics.logEvent("app_launch") {
            param("package_name", packageName)
        }
    }

    /**
     * 追踪应用冻结操作
     */
    fun trackFreezeApp(packageName: String, frozen: Boolean) {
        firebaseAnalytics.logEvent("app_freeze") {
            param("package_name", packageName)
            param("frozen", frozen)
        }
    }

    /**
     * 追踪配置文件创建
     */
    fun trackCreateProfile(profileName: String) {
        firebaseAnalytics.logEvent("profile_create") {
            param("profile_name", profileName)
        }
    }

    /**
     * 追踪配置文件删除
     */
    fun trackDeleteProfile(profileId: Int) {
        firebaseAnalytics.logEvent("profile_delete") {
            param("profile_id", profileId.toLong())
        }
    }

    /**
     * 追踪 Shizuku 权限状态
     */
    fun trackShizukuPermission(granted: Boolean) {
        firebaseAnalytics.logEvent("shizuku_permission") {
            param("granted", granted)
        }
    }

    /**
     * 追踪屏幕浏览
     */
    fun trackScreenView(screenName: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "AppCloneX")
        }
    }

    /**
     * 追踪自定义事件
     */
    fun trackCustomEvent(eventName: String, params: Map<String, String> = emptyMap()) {
        firebaseAnalytics.logEvent(eventName) {
            params.forEach { (key, value) ->
                param(key, value)
            }
        }
    }

    /**
     * 设置用户属性
     */
    fun setUserProperty(name: String, value: String) {
        firebaseAnalytics.setUserProperty(name, value)
    }
}
