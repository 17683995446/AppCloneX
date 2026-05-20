package com.appclonex.infrastructure.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 克隆进度通知管理器
 * 用于显示克隆操作的进度通知
 *
 * R13-002 修复：统一通知渠道ID
 * - 之前使用 "clone_progress"，与 Application 中创建的 "clone_operations" 不一致
 * - Android 8.0+ 使用未注册渠道的通知会被系统静默丢弃
 * - 统一为 "clone_operations" 以匹配 AppCloneXAndroidApplication 中的渠道定义
 */
class ApplicationCloneProgressNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = NotificationManagerCompat.from(context)
    
    companion object {
        // R13-002 修复：统一渠道ID为 "clone_operations"，与 Application 和 ForegroundService 一致
        const val CHANNEL_ID = "clone_operations"
        const val CHANNEL_NAME = "克隆进度"
        const val NOTIFICATION_ID_CLONE = 1001
        const val NOTIFICATION_ID_COMPLETE = 1002
    }

    /**
     * 显示克隆进度通知
     * @param current 当前进度
     * @param total 总数
     * @param appName 当前正在克隆的应用名称
     */
    fun showProgress(current: Int, total: Int, appName: String) {
        val intent = Intent(context, com.appclonex.presentation.activity.ApplicationMainLauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progress = if (total > 0) (current * 100 / total) else 0

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("正在克隆应用")
            .setContentText("$appName ($current/$total)")
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID_CLONE, notification)
        } catch (e: SecurityException) {
            // R13-002 修复：记录日志而非静默吞没
            ApplicationLoggingServiceManager.w("CloneNotif", "缺少通知权限，无法显示克隆进度", e)
        }
    }

    /**
     * 显示克隆完成通知
     * @param successCount 成功数量
     * @param failCount 失败数量
     */
    fun showComplete(successCount: Int, failCount: Int) {
        // 取消进度通知
        notificationManager.cancel(NOTIFICATION_ID_CLONE)

        val intent = Intent(context, com.appclonex.presentation.activity.ApplicationMainLauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            failCount == 0 -> "克隆完成"
            successCount == 0 -> "克隆失败"
            else -> "克隆完成（部分失败）"
        }

        val message = when {
            failCount == 0 -> "成功克隆 $successCount 个应用"
            successCount == 0 -> "所有应用克隆失败"
            else -> "成功 $successCount 个，失败 $failCount 个"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID_COMPLETE, notification)
        } catch (e: SecurityException) {
            // R13-002 修复：记录日志而非静默吞没
            ApplicationLoggingServiceManager.w("CloneNotif", "缺少通知权限，无法显示克隆完成", e)
        }
    }

    /**
     * 取消所有通知
     */
    fun cancelAll() {
        notificationManager.cancel(NOTIFICATION_ID_CLONE)
        notificationManager.cancel(NOTIFICATION_ID_COMPLETE)
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示应用克隆进度"
                setShowBadge(false)
            }
            
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
