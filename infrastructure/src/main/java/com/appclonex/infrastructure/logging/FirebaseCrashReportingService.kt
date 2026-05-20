package com.appclonex.infrastructure.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase崩溃报告服务
 *
 * 封装Firebase Crashlytics，提供统一的崩溃报告接口
 * 支持生产环境崩溃监控和诊断信息收集
 */
@Singleton
class FirebaseCrashReportingService @Inject constructor() {

    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    init {
        // 启用崩溃报告
        crashlytics.setCrashlyticsCollectionEnabled(true)
    }

    /**
     * 记录非致命异常
     *
     * @param throwable 异常对象
     * @param context 可选的上下文信息
     */
    fun recordException(throwable: Throwable, context: Map<String, String> = emptyMap()) {
        context.forEach { (key, value) ->
            crashlytics.setCustomKey(key, value)
        }
        crashlytics.recordException(throwable)
    }

    /**
     * 设置用户标识
     *
     * @param userId 用户ID
     */
    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
    }

    /**
     * 设置自定义键值对
     *
     * @param key 键
     * @param value 值
     */
    fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * 记录面包屑（操作轨迹）
     *
     * @param message 消息
     */
    fun log(message: String) {
        crashlytics.log(message)
    }

    /**
     * 标记为崩溃
     *
     * 用于在已知问题但无法立即修复时标记
     */
    fun markAsCrash() {
        crashlytics.recordException(RuntimeException("Marked as crash"))
    }
}
