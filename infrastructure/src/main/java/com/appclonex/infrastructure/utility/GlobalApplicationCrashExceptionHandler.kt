package com.appclonex.infrastructure.utility

import android.content.Context
import android.os.Build
import android.util.Log
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局崩溃异常处理器
 * 防止应用崩溃，记录错误日志
 */
class GlobalApplicationCrashExceptionHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // 记录崩溃日志
        ApplicationLoggingServiceManager.e("GlobalApplicationCrashExceptionHandler", "应用崩溃", throwable)

        // 上报到Firebase Crashlytics
        try {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("thread_name", thread.name)
                setCustomKey("thread_id", thread.id.toString())
                setCustomKey("crash_time", System.currentTimeMillis().toString())
                recordException(throwable)
            }
        } catch (e: Exception) {
            // Firebase可能未初始化，忽略错误
            ApplicationLoggingServiceManager.e("CrashHandler", "Failed to report to Firebase", e)
        }

        // 保存崩溃信息到文件
        try {
            val crashDir = File(context.filesDir, "crashes")
            if (!crashDir.exists()) crashDir.mkdirs()

            val crashFile = File(crashDir, "crash_${System.currentTimeMillis()}.txt")
            crashFile.writeText("""
                崩溃时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
                线程: ${thread.name}

                ${ApplicationLoggingServiceManager.getDeviceInfo()}

                堆栈信息:
                ${Log.getStackTraceString(throwable)}
            """.trimIndent())
        } catch (e: Exception) {
            // 忽略
        }

        // 调用默认处理器
        defaultHandler?.uncaughtException(thread, throwable)
    }
}
