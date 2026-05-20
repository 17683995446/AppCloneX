package com.appclonex.infrastructure.monitoring

import com.appclonex.infrastructure.logging.FirebaseCrashReportingService
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 分析面包屑追踪器
 *
 * 记录用户操作轨迹，用于崩溃分析和用户行为分析
 * 面包屑会附加到崩溃报告中，帮助复现问题
 */
@Singleton
class AnalyticsBreadcrumbTracker @Inject constructor(
    private val crashReportingService: FirebaseCrashReportingService
) {

    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics
    private val breadcrumbQueue = ConcurrentLinkedQueue<Breadcrumb>()
    private val maxBreadcrumbs = 50

    /**
     * 面包屑数据类
     */
    data class Breadcrumb(
        val timestamp: Long,
        val category: String,
        val message: String,
        val data: Map<String, String>
    )

    /**
     * 记录面包屑
     *
     * @param category 类别（如：navigation, action, error）
     * @param message 消息
     * @param data 附加数据
     */
    fun record(
        category: String,
        message: String,
        data: Map<String, String> = emptyMap()
    ) {
        val breadcrumb = Breadcrumb(
            timestamp = System.currentTimeMillis(),
            category = category,
            message = message,
            data = data
        )

        // 添加到队列
        breadcrumbQueue.offer(breadcrumb)

        // 限制队列大小
        while (breadcrumbQueue.size > maxBreadcrumbs) {
            breadcrumbQueue.poll()
        }

        // 记录到 Crashlytics
        val breadcrumbString = "[$category] $message ${data.entries.joinToString(",") { "${it.key}=${it.value}" }}"
        crashReportingService.log(breadcrumbString)

        // 记录到 Analytics（如果是关键事件）
        if (category in listOf("error", "crash", "critical_action")) {
            firebaseAnalytics.logEvent("breadcrumb_$category") {
                param("message", message)
                data.forEach { (key, value) ->
                    param(key, value.take(100)) // 限制长度
                }
            }
        }
    }

    /**
     * 记录导航面包屑
     *
     * @param fromScreen 来源页面
     * @param toScreen 目标页面
     * @param action 操作方式（如：click, swipe, back）
     */
    fun recordNavigation(
        fromScreen: String,
        toScreen: String,
        action: String = "click"
    ) {
        record(
            category = "navigation",
            message = "$fromScreen -> $toScreen",
            data = mapOf(
                "from" to fromScreen,
                "to" to toScreen,
                "action" to action
            )
        )
    }

    /**
     * 记录操作面包屑
     *
     * @param action 操作名称
     * @param target 操作目标
     * @param result 操作结果
     */
    fun recordAction(
        action: String,
        target: String,
        result: String = "success"
    ) {
        record(
            category = "action",
            message = "$action: $target",
            data = mapOf(
                "action" to action,
                "target" to target,
                "result" to result
            )
        )
    }

    /**
     * 记录错误面包屑
     *
     * @param error 错误信息
     * @param context 错误上下文
     */
    fun recordError(
        error: String,
        context: Map<String, String> = emptyMap()
    ) {
        record(
            category = "error",
            message = error,
            data = context
        )
    }

    /**
     * 记录Shizuku相关面包屑
     *
     * @param action 操作（bind/request/check）
     * @param result 结果
     */
    fun recordShizuku(action: String, result: String) {
        record(
            category = "shizuku",
            message = "Shizuku $action: $result",
            data = mapOf("action" to action, "result" to result)
        )
    }

    /**
     * 记录克隆操作面包屑
     *
     * @param packageName 应用包名
     * @param profileId 配置文件ID
     * @param stage 阶段（start/progress/success/failed）
     */
    fun recordCloneOperation(
        packageName: String,
        profileId: Int,
        stage: String
    ) {
        record(
            category = "clone",
            message = "Clone $packageName to profile $profileId: $stage",
            data = mapOf(
                "package_name" to packageName,
                "profile_id" to profileId.toString(),
                "stage" to stage
            )
        )
    }

    /**
     * 获取最近的面包屑列表
     *
     * @param count 数量
     * @return 面包屑列表
     */
    fun getRecentBreadcrumbs(count: Int = 20): List<Breadcrumb> {
        return breadcrumbQueue.toList().takeLast(count)
    }

    /**
     * 获取所有面包屑（用于崩溃报告）
     *
     * @return 面包屑列表
     */
    fun getAllBreadcrumbs(): List<Breadcrumb> {
        return breadcrumbQueue.toList()
    }

    /**
     * 清除所有面包屑
     */
    fun clear() {
        breadcrumbQueue.clear()
    }

    /**
     * 将面包屑附加到崩溃报告
     */
    fun attachToCrashReport() {
        val breadcrumbs = getAllBreadcrumbs()
        if (breadcrumbs.isNotEmpty()) {
            val breadcrumbJson = breadcrumbs.joinToString("\n") { b ->
                "[${b.timestamp}] [${b.category}] ${b.message}"
            }
            crashReportingService.setCustomKey("breadcrumbs", breadcrumbJson.take(1000))
        }
    }
}
