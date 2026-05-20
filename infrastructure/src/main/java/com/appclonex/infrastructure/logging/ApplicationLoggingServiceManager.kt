package com.appclonex.infrastructure.logging

import android.content.Context
import android.os.Build
import android.util.Log
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 应用程序日志记录器（门面类）
 * 内部委托给 Timber，提供统一的日志接口
 */
object ApplicationLoggingServiceManager {

    private const val TAG = "AppCloneX"
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024 // 5MB
    private const val MAX_LOG_FILES = 3

    /**
     * 初始化日志系统
     * 在 Application.onCreate() 中调用
     *
     * @param context 应用上下文
     * @param isDebug 是否为 Debug 模式，Debug 模式下会额外输出到 Logcat
     */
    fun init(context: Context, isDebug: Boolean = false) {
        val forest = Timber.forest().toList()
        forest.forEach { Timber.uproot(it) }

        if (isDebug) {
            // Debug模式: 输出到Logcat
            Timber.plant(DebugTree())
        }
        // 所有模式: 写入日志文件（FileTree内部应根据日志级别过滤）
        Timber.plant(FileTree(context))
    }

    /**
     * 写入调试日志
     */
    fun d(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }

    /**
     * 写入信息日志
     */
    fun i(tag: String, message: String) {
        Timber.tag(tag).i(message)
    }

    /**
     * 写入警告日志
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Timber.tag(tag).w(throwable, message)
        } else {
            Timber.tag(tag).w(message)
        }
    }

    /**
     * 写入错误日志
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Timber.tag(tag).e(throwable, message)
        } else {
            Timber.tag(tag).e(message)
        }
    }

    /**
     * 记录操作日志
     */
    fun logOperation(operation: String, details: String = "") {
        Timber.tag(TAG).i("[Operation] [$operation] $details")
    }

    /**
     * 记录 Shizuku 相关日志
     */
    fun logShizuku(action: String, result: String) {
        Timber.tag("Shizuku").i("$action -> $result")
    }

    /**
     * 获取所有日志文件
     */
    fun getLogFiles(context: Context): List<File> {
        val logDir = File(context.filesDir, "logs")
        return logDir.listFiles { file -> file.name.startsWith("app_") && file.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * 获取最新日志内容
     */
    fun getLatestLog(context: Context, maxLines: Int = 100): String {
        val logFiles = getLogFiles(context)
        if (logFiles.isEmpty()) return "暂无日志"

        val logFile = logFiles.first()
        return logFile.readLines().takeLast(maxLines).joinToString("\n")
    }

    /**
     * 清除所有日志
     */
    fun clearLogs(context: Context) {
        val logDir = File(context.filesDir, "logs")
        logDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * 获取设备信息
     */
    fun getDeviceInfo(): String {
        return """
            设备信息:
            - 品牌: ${Build.BRAND}
            - 型号: ${Build.MODEL}
            - Android版本: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            - 系统版本: ${Build.DISPLAY}
        """.trimIndent()
    }
}

// ==================== Timber Tree 实现 ====================

/**
 * DebugTree: 将日志输出到 Logcat
 */
private class DebugTree : Timber.DebugTree() {

    override fun createStackElementTag(element: StackTraceElement): String {
        return "(${element.fileName}:${element.lineNumber})#${element.methodName}"
    }
}

/**
 * FileTree: 将日志写入文件，IO 操作在后台线程执行
 */
private class FileTree(context: Context) : Timber.Tree() {

    private val logDir = File(context.filesDir, "logs").apply {
        if (!exists()) mkdirs()
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "LogWriter").apply { isDaemon = true }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        executor.submit {
            try {
                val logFile = File(logDir, "app_${FILE_DATE_FORMAT.get().format(Date())}.log")

                // 检查文件大小，超限则轮转
                if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                    rotateLogs()
                }

                val level = when (priority) {
                    Log.VERBOSE -> "V"
                    Log.DEBUG -> "D"
                    Log.INFO -> "I"
                    Log.WARN -> "W"
                    Log.ERROR -> "E"
                    Log.ASSERT -> "A"
                    else -> "?"
                }

                val timestamp = DATE_FORMAT.get().format(Date())
                val logLine = buildString {
                    append("[$timestamp] [$level] [${tag ?: "AppCloneX"}] $message")
                    if (t != null) {
                        append("\n${Log.getStackTraceString(t)}")
                    }
                    append("\n")
                }

                logFile.appendText(logLine)
            } catch (_: Exception) {
                // 日志写入失败不影响应用运行
            }
        }
    }

    private fun rotateLogs() {
        try {
            logDir.listFiles { file -> file.name.startsWith("app_") && file.name.endsWith(".log") }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(MAX_LOG_FILES - 1)
                ?.forEach { it.delete() }
        } catch (_: Exception) {
            // 忽略错误
        }
    }

    companion object {
        private const val MAX_LOG_SIZE = 5 * 1024 * 1024 // 5MB
        private const val MAX_LOG_FILES = 3

        /**
         * 使用 ThreadLocal 解决 SimpleDateFormat 线程安全问题
         */
        private val DATE_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        }

        private val FILE_DATE_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat =
                SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        }
    }
}
