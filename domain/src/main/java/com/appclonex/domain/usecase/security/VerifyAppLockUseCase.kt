package com.appclonex.domain.usecase.security

import com.appclonex.domain.model.LockVerificationResult
import com.appclonex.domain.repository.AppLockRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 验证应用锁用例
 */
@Singleton
class VerifyAppLockUseCase @Inject constructor(
    private val appLockRepository: AppLockRepository
) {
    /**
     * 验证PIN码
     */
    suspend fun verifyPin(
        packageName: String,
        profileUserId: Int,
        pin: String
    ): LockVerificationResult {
        require(packageName.isNotBlank()) { "应用包名不能为空" }
        require(pin.isNotBlank()) { "PIN码不能为空" }
        
        return appLockRepository.verifyPin(packageName, profileUserId, pin)
    }
    
    /**
     * 验证生物识别
     */
    suspend fun verifyBiometric(
        packageName: String,
        profileUserId: Int
    ): LockVerificationResult {
        require(packageName.isNotBlank()) { "应用包名不能为空" }
        
        return appLockRepository.verifyBiometric(packageName, profileUserId)
    }
    
    /**
     * 检查是否需要验证
     */
    suspend fun needsVerification(packageName: String, profileUserId: Int): Boolean {
        return appLockRepository.needsVerification(packageName, profileUserId)
    }
}
