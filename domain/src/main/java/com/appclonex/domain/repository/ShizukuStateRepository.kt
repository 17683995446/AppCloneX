package com.appclonex.domain.repository

import com.appclonex.domain.model.ShizukuConnectionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Shizuku状态仓库接口
 * 
 * 提供Shizuku服务连接状态的查询和监听
 */
interface ShizukuStateRepository {
    /**
     * 获取当前Shizuku连接状态
     */
    val shizukuState: StateFlow<ShizukuConnectionState>
    
    /**
     * 请求Shizuku权限
     */
    suspend fun requestPermission(): Result<Boolean>
    
    /**
     * 检查Shizuku是否已连接
     */
    fun isConnected(): Boolean
    
    /**
     * 初始化Shizuku服务连接
     */
    fun init()
    
    /**
     * 销毁Shizuku服务连接
     */
    fun destroy()
}
