package com.appclonex.infrastructure.monitoring

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.appclonex.infrastructure.logging.FirebaseCrashReportingService
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户反馈收集器
 *
 * 收集用户反馈、设备信息和日志，用于问题诊断
 * 支持通过邮件或GitHub Issues提交反馈
 */
@Singleton
class UserFeedbackCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashReportingService: FirebaseCrashReportingService,
    private val breadcrumbTracker: AnalyticsBreadcrumbTracker
) {

    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    /**
     * 反馈数据类
     */
    data class Feedback(
        val type: FeedbackType,
        val title: String,
        val description: String,
        val email: String? = null,
        val attachLogs: Boolean = true
    )

    /**
     * 反馈类型
     */
    enum class FeedbackType {
        BUG,           // Bug报告
        FEATURE,       // 功能建议
        PERFORMANCE,   // 性能问题
        USABILITY,     // 可用性问题
        OTHER          // 其他
    }

    /**
     * 提交反馈
     *
     * @param feedback 反馈信息
     */
    suspend fun submitFeedback(feedback: Feedback): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 记录反馈事件
            firebaseAnalytics.logEvent("user_feedback") {
                param("type", feedback.type.name.lowercase())
                param("has_email", feedback.email != null)
                param("attach_logs", feedback.attachLogs)
            }

            // 收集设备信息
            val deviceInfo = collectDeviceInfo()

            // 收集日志
            val logs = if (feedback.attachLogs) collectLogs() else ""

            // 收集面包屑
            val breadcrumbs = breadcrumbTracker.getRecentBreadcrumbs(30)
                .joinToString("\n") { "[${it.timestamp}] [${it.category}] ${it.message}" }

            // 生成反馈报告
            val report = buildFeedbackReport(feedback, deviceInfo, logs, breadcrumbs)

            // 保存到本地文件
            val reportFile = saveReportToFile(report)

            // 发送邮件
            sendFeedbackEmail(feedback, reportFile)

            Result.success(Unit)
        } catch (e: Exception) {
            crashReportingService.recordException(e, mapOf("action" to "submit_feedback"))
            Result.failure(e)
        }
    }

    /**
     * 收集设备信息
     */
    private fun collectDeviceInfo(): Map<String, String> {
        return mapOf(
            "Device Model" to Build.MODEL,
            "Manufacturer" to Build.MANUFACTURER,
            "Android Version" to Build.VERSION.RELEASE,
            "SDK Level" to Build.VERSION.SDK_INT.toString(),
            "App Version" to getAppVersion(),
            "Screen Resolution" to getScreenResolution(),
            "Available Memory" to getAvailableMemory(),
            "Total Memory" to getTotalMemory()
        )
    }

    /**
     * 获取应用版本
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * 获取屏幕分辨率
     */
    private fun getScreenResolution(): String {
        val displayMetrics = context.resources.displayMetrics
        return "${displayMetrics.widthPixels}x${displayMetrics.heightPixels} (${displayMetrics.densityDpi}dpi)"
    }

    /**
     * 获取可用内存
     */
    private fun getAvailableMemory(): String {
        val runtime = Runtime.getRuntime()
        val available = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()
        return "${available / 1024 / 1024}MB"
    }

    /**
     * 获取总内存
     */
    private fun getTotalMemory(): String {
        val runtime = Runtime.getRuntime()
        return "${runtime.maxMemory() / 1024 / 1024}MB"
    }

    /**
     * 收集日志
     */
    private fun collectLogs(): String {
        return try {
            val process = Runtime.getRuntime().exec("logcat -d -t 500")
            process.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Failed to collect logs: ${e.message}"
        }
    }

    /**
     * 构建反馈报告
     */
    private fun buildFeedbackReport(
        feedback: Feedback,
        deviceInfo: Map<String, String>,
        logs: String,
        breadcrumbs: String
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())

        return buildString {
            appendLine("=".repeat(60))
            appendLine("AppCloneX 用户反馈报告")
            appendLine("User Feedback Report")
            appendLine("=".repeat(60))
            appendLine()
            appendLine("提交时间 / Submission Time: $timestamp")
            appendLine("反馈类型 / Feedback Type: ${feedback.type.name}")
            appendLine()
            appendLine("-".repeat(60))
            appendLine("标题 / Title:")
            appendLine("-".repeat(60))
            appendLine(feedback.title)
            appendLine()
            appendLine("-".repeat(60))
            appendLine("描述 / Description:")
            appendLine("-".repeat(60))
            appendLine(feedback.description)
            appendLine()
            appendLine("-".repeat(60))
            appendLine("设备信息 / Device Information:")
            appendLine("-".repeat(60))
            deviceInfo.forEach { (key, value) ->
                appendLine("$key: $value")
            }
            appendLine()
            appendLine("-".repeat(60))
            appendLine("操作轨迹 / Breadcrumbs:")
            appendLine("-".repeat(60))
            appendLine(breadcrumbs)
            appendLine()
            if (logs.isNotEmpty()) {
                appendLine("-".repeat(60))
                appendLine("日志 / Logs:")
                appendLine("-".repeat(60))
                appendLine(logs.takeLast(10000)) // 限制日志大小
            }
            appendLine()
            appendLine("=".repeat(60))
            appendLine("End of Report")
            appendLine("=".repeat(60))
        }
    }

    /**
     * 保存报告到文件
     */
    private fun saveReportToFile(report: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(context.cacheDir, "feedback_report_$timestamp.txt")
        file.writeText(report)
        return file
    }

    /**
     * 发送反馈邮件
     */
    private fun sendFeedbackEmail(feedback: Feedback, reportFile: File) {
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback@appclonex.dev"))
            putExtra(
                Intent.EXTRA_SUBJECT,
                "[AppCloneX Feedback] ${feedback.type.name}: ${feedback.title}"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                """
                感谢您的反馈！
                Thank you for your feedback!

                反馈类型 / Type: ${feedback.type.name}
                标题 / Title: ${feedback.title}

                详细描述 / Description:
                ${feedback.description}

                ${if (feedback.email != null) "联系邮箱 / Contact: ${feedback.email}" else ""}

                详细报告请查看附件。
                Please see the attachment for detailed report.
                """.trimIndent()
            )

            // 添加附件
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                reportFile
            )
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // 启动邮件应用
        val chooser = Intent.createChooser(emailIntent, "发送反馈 / Send Feedback")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * 快速提交Bug报告
     *
     * @param title 标题
     * @param description 描述
     */
    suspend fun submitBugReport(title: String, description: String): Result<Unit> {
        return submitFeedback(
            Feedback(
                type = FeedbackType.BUG,
                title = title,
                description = description,
                attachLogs = true
            )
        )
    }

    /**
     * 提交功能建议
     *
     * @param title 标题
     * @param description 描述
     */
    suspend fun submitFeatureRequest(title: String, description: String): Result<Unit> {
        return submitFeedback(
            Feedback(
                type = FeedbackType.FEATURE,
                title = title,
                description = description,
                attachLogs = false
            )
        )
    }
}
