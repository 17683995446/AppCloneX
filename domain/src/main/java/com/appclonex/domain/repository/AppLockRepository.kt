package com.appclonex.domain.repository

import androidx.fragment.app.FragmentActivity
import com.appclonex.domain.model.AppLockConfiguration
import com.appclonex.domain.model.LockVerificationResult
import kotlinx.coroutines.flow.Flow

/**
 * 应用锁仓库接口
 */
interface AppLockRepository {
    /**
     * 获取所有锁定的应用
     */
    fun getAllLockedApps(): Flow<List<AppLockConfiguration>>
    
    /**
     * 获取指定应用的锁配置
     */
    suspend fun getLockConfiguration(packageName: String, profileUserId: Int): AppLockConfiguration?
    
    /**
     * 设置应用锁
     */
    suspend fun setAppLock(configuration: AppLockConfiguration): Result<Boolean>
    
    /**
     * 移除应用锁
     */
    suspend fun removeAppLock(packageName: String, profileUserId: Int): Result<Boolean>
    
    /**
     * 验证PIN码
     */
    suspend fun verifyPin(packageName: String, profileUserId: Int, pin: String): LockVerificationResult
    
    /**
     * 验证生物识别
     */
    suspend fun verifyBiometric(packageName: String, profileUserId: Int, activity: FragmentActivity): LockVerificationResult
    
    /**
     * 检查应用是否需要验证
     */
    suspend fun needsVerification(packageName: String, profileUserId: Int): Boolean
    
    /**
     * 设置PIN码
     */
    suspend fun setPin(packageName: String, profileUserId: Int, pin: String): Result<Boolean>

    /**
     * 删除指定配置文件下的所有应用锁
     * @param profileUserId 配置文件ID
     * @return 被删除的应用锁数量
     */
    suspend fun removeAllLocksForProfile(profileUserId: Int): Int
}
