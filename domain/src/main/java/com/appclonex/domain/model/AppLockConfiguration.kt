package com.appclonex.domain.model

/**
 * 应用锁配置
 */
data class AppLockConfiguration(
    val packageName: String,
    val profileUserId: Int,
    val lockType: LockType,
    val isLocked: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastVerifiedAt: Long? = null
) {
    enum class LockType {
        PIN,            // PIN码
        BIOMETRIC,      // 生物识别（指纹/面容）
        PATTERN,        // 图案
        NONE            // 无锁
    }
}

/**
 * 锁验证结果
 */
sealed class LockVerificationResult {
    data class Success(val lockType: AppLockConfiguration.LockType) : LockVerificationResult()
    data class Failed(val attemptsRemaining: Int) : LockVerificationResult()
    data object Locked : LockVerificationResult()  // 锁定状态，需要等待
    data object Cancelled : LockVerificationResult()
    data class Error(val message: String) : LockVerificationResult()
}
