package com.appclonex.infrastructure.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.appclonex.application.AppCloneXAndroidApplication
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 克隆操作前台服务
 *
 * 在执行克隆、冻结等长时间操作时启动前台服务，
 * 防止系统因资源回收而终止操作。
 */
class CloneOperationForegroundService : Service() {

    companion object {
        private const val TAG = "CloneOperationForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "clone_operations"

        const val ACTION_START = "com.appclonex.action.START_CLONE_SERVICE"
        const val ACTION_STOP = "com.appclonex.action.STOP_CLONE_SERVICE"
        const val EXTRA_OPERATION_TYPE = "operation_type"
        const val EXTRA_PACKAGE_NAME = "package_name"

        // 操作类型
        const val OP_CLONE = "clone"
        const val OP_FREEZE = "freeze"
        const val OP_UNFREEZE = "unfreeze"
        const val OP_DELETE = "delete"
        const val OP_BATCH_FREEZE = "batch_freeze"
        const val OP_BATCH_UNFREEZE = "batch_unfreeze"

        /**
         * 启动克隆操作前台服务
         */
        fun startService(
            context: Context,
            operationType: String,
            packageName: String = ""
        ) {
            val intent = Intent(context, CloneOperationForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_OPERATION_TYPE, operationType)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * 停止克隆操作前台服务
         */
        fun stopService(context: Context) {
            val intent = Intent(context, CloneOperationForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val _operationState = MutableStateFlow(CloneOperationUiState())
    val operationState: StateFlow<CloneOperationUiState> = _operationState.asStateFlow()

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ApplicationLoggingServiceManager.i(TAG, "前台服务已创建")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "克隆操作",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示克隆、冻结等操作的进度"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val operationType = intent.getStringExtra(EXTRA_OPERATION_TYPE) ?: OP_CLONE
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
                startForeground(operationType, packageName)
            }
            ACTION_STOP -> {
                stopForegroundAndSelf()
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun startForeground(operationType: String, packageName: String) {
        val notification = createNotification(operationType, packageName)
        startForeground(NOTIFICATION_ID, notification)

        _operationState.value = CloneOperationUiState(
            isRunning = true,
            operationType = operationType,
            packageName = packageName
        )

        ApplicationLoggingServiceManager.i(TAG, "前台服务已启动: $operationType $packageName")
    }

    private fun createNotification(operationType: String, packageName: String): Notification {
        val operationLabel = when (operationType) {
            OP_CLONE -> "正在克隆"
            OP_FREEZE -> "正在冻结"
            OP_UNFREEZE -> "正在解冻"
            OP_DELETE -> "正在删除"
            OP_BATCH_FREEZE -> "正在批量冻结"
            OP_BATCH_UNFREEZE -> "正在批量解冻"
            else -> "正在处理"
        }

        val title = if (packageName.isNotEmpty()) {
            "$operationLabel $packageName"
        } else {
            operationLabel
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("AppCloneX 正在执行操作，请勿关闭")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    /**
     * 更新操作进度
     */
    fun updateProgress(current: Int, total: Int) {
        _operationState.update { it.copy(
            currentProgress = current,
            totalProgress = total,
            progressText = "$current / $total"
        ) }
        // 更新通知
        val currentState = _operationState.value
        val notification = createNotification(
            currentState.operationType,
            currentState.packageName
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 更新操作状态消息
     */
    fun updateStatusMessage(message: String) {
        _operationState.value = _operationState.value.copy(
            statusMessage = message
        )
    }

    /**
     * 标记操作完成
     */
    fun markCompleted(success: Boolean, message: String = "") {
        _operationState.update { it.copy(
            isRunning = false,
            isCompleted = true,
            isSuccess = success,
            statusMessage = if (message.isNotEmpty()) message else (if (success) "操作完成" else "操作失败")
        ) }
        ApplicationLoggingServiceManager.i(TAG, "操作完成: success=$success, message=$message")
        stopForegroundAndSelf()
    }

    private fun stopForegroundAndSelf() {
        _operationState.value = _operationState.value.copy(isRunning = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        ApplicationLoggingServiceManager.i(TAG, "前台服务已停止")
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        ApplicationLoggingServiceManager.i(TAG, "前台服务已销毁")
    }

    /**
     * LocalBinder 提供对 Service 实例的访问
     */
    inner class LocalBinder : Binder() {
        fun getService(): CloneOperationForegroundService = this@CloneOperationForegroundService
    }
}

/**
 * 克隆操作 UI 状态
 */
data class CloneOperationUiState(
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val isSuccess: Boolean = false,
    val operationType: String = "",
    val packageName: String = "",
    val currentProgress: Int = 0,
    val totalProgress: Int = 0,
    val progressText: String = "",
    val statusMessage: String = ""
)
