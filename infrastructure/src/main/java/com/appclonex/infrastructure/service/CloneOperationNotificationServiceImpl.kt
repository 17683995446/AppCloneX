package com.appclonex.infrastructure.service

import android.content.Context
import com.appclonex.domain.service.CloneOperationNotificationService
import com.appclonex.domain.service.CloneOperationNotificationState
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 克隆操作通知服务实现
 *
 * 通过 Context 启动/停止前台服务，
 * 并在本地维护操作状态供 ViewModel 观察。
 */
@Singleton
class CloneOperationNotificationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CloneOperationNotificationService {

    private val _operationState = MutableStateFlow(CloneOperationNotificationState())

    override val operationState: StateFlow<CloneOperationNotificationState> =
        _operationState.asStateFlow()

    override fun startForegroundOperation(operationType: String, packageName: String) {
        _operationState.value = CloneOperationNotificationState(
            isRunning = true,
            operationType = operationType,
            packageName = packageName
        )
        CloneOperationForegroundService.startService(context, operationType, packageName)
        ApplicationLoggingServiceManager.i(
            "CloneNotificationService",
            "启动前台操作: $operationType $packageName"
        )
    }

    override fun updateProgress(current: Int, total: Int) {
        _operationState.value = _operationState.value.copy(
            currentProgress = current,
            totalProgress = total,
            progressText = "$current / $total"
        )
    }

    override fun updateStatusMessage(message: String) {
        _operationState.value = _operationState.value.copy(statusMessage = message)
    }

    override fun markCompleted(success: Boolean, message: String) {
        _operationState.value = _operationState.value.copy(
            isRunning = false,
            isCompleted = true,
            isSuccess = success,
            statusMessage = if (message.isNotEmpty()) message else (if (success) "操作完成" else "操作失败")
        )
        CloneOperationForegroundService.stopService(context)
    }

    override fun stopForegroundOperation() {
        _operationState.value = _operationState.value.copy(isRunning = false)
        CloneOperationForegroundService.stopService(context)
    }
}
