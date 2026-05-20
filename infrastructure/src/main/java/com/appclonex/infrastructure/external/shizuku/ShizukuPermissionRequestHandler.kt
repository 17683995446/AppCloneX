package com.appclonex.infrastructure.external.shizuku

import android.content.Context
import dev.rikka.shizuku.Shizuku
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Shizuku 权限处理器
 * 提供实时的 Shizuku 状态监听
 */
object ShizukuPermissionRequestHandler {

    /**
     * 获取 Shizuku 状态 Flow
     * 当 Shizuku 状态变化时会自动发射新状态
     */
    fun getStateFlow(context: Context): Flow<ShizukuState> = callbackFlow {
        // 初始状态
        trySend(getCurrentState())

        // 权限结果监听器
        val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            val state = if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ShizukuState.READY
            } else {
                ShizukuState.PERMISSION_DENIED
            }
            trySend(state)
        }

        // Binder 接收监听器
        val binderReceivedListener = Shizuku.OnBinderReceivedListener {
            trySend(getCurrentState())
        }

        // Binder 断开监听器
        val binderDeadListener = Shizuku.OnBinderDeadListener {
            trySend(ShizukuState.NOT_RUNNING)
        }

        // 注册监听器
        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)

        // 关闭时清理
        awaitClose {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
        }
    }

    /**
     * 获取当前 Shizuku 状态
     */
    fun getCurrentState(): ShizukuState {
        return try {
            when {
                !Shizuku.pingBinder() -> ShizukuState.NOT_RUNNING
                Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED ->
                    ShizukuState.PERMISSION_DENIED
                else -> ShizukuState.READY
            }
        } catch (e: Exception) {
            ShizukuState.NOT_INSTALLED
        }
    }
}
