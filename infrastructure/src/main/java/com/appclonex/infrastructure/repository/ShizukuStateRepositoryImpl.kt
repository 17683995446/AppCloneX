package com.appclonex.infrastructure.repository

import com.appclonex.domain.model.ShizukuConnectionState
import com.appclonex.domain.repository.ShizukuStateRepository
import com.appclonex.infrastructure.external.shizuku.ShizukuServiceConnectionProvider
import com.appclonex.infrastructure.external.shizuku.ShizukuState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shizuku状态仓库实现
 */
@Singleton
class ShizukuStateRepositoryImpl @Inject constructor(
    private val shizukuServiceProvider: ShizukuServiceConnectionProvider
) : ShizukuStateRepository {
    
    private val _shizukuState = MutableStateFlow(ShizukuConnectionState.DISCONNECTED)
    
    private val isInitialized = AtomicBoolean(false)
    
    override val shizukuState: StateFlow<ShizukuConnectionState>
        get() = _shizukuState.asStateFlow()
    
    override fun init() {
        // R9-CONC-016: 使用 compareAndSet 确保只有一个线程能执行初始化，防止并发重复初始化
        if (!isInitialized.compareAndSet(false, true)) return
        
        // 监听Shizuku服务状态变化
        shizukuServiceProvider.onBinderReceived = {
            _shizukuState.value = ShizukuConnectionState.CONNECTED
        }
        shizukuServiceProvider.onBinderDead = {
            _shizukuState.value = ShizukuConnectionState.DISCONNECTED
        }
        shizukuServiceProvider.onPermissionResult = { granted ->
            if (granted) {
                _shizukuState.value = ShizukuConnectionState.CONNECTED
            } else {
                _shizukuState.value = ShizukuConnectionState.PERMISSION_REQUIRED
            }
        }
        
        // 初始化服务连接
        shizukuServiceProvider.init()
        
        // 检查当前状态
        updateCurrentState()
    }
    
    private fun updateCurrentState() {
        _shizukuState.value = when {
            !shizukuServiceProvider.isShizukuAvailable() -> ShizukuConnectionState.DISCONNECTED
            !shizukuServiceProvider.hasPermission() -> ShizukuConnectionState.PERMISSION_REQUIRED
            else -> ShizukuConnectionState.CONNECTED
        }
    }
    
    override suspend fun requestPermission(): Result<Boolean> {
        return try {
            // 触发权限请求
            shizukuServiceProvider.requestPermission()
            Result.success(true)
        } catch (e: Exception) {
            _shizukuState.value = ShizukuConnectionState.ERROR
            Result.failure(e)
        }
    }
    
    override fun isConnected(): Boolean {
        return shizukuServiceProvider.isShizukuReady()
    }
    
    override fun destroy() {
        shizukuServiceProvider.destroy()
        isInitialized.set(false)
        _shizukuState.value = ShizukuConnectionState.DISCONNECTED
    }
}
