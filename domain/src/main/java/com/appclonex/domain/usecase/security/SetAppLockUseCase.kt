package com.appclonex.domain.usecase.security

import com.appclonex.domain.model.AppLockConfiguration
import com.appclonex.domain.repository.AppLockRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设置应用锁用例
 */
@Singleton
class SetAppLockUseCase @Inject constructor(
    private val appLockRepository: AppLockRepository
) {
    suspend operator fun invoke(
        packageName: String,
        profileUserId: Int,
        lockType: AppLockConfiguration.LockType,
        pin: String? = null
    ): Result<Boolean> {
        require(packageName.isNotBlank()) { "应用包名不能为空" }
        require(profileUserId > 0) { "用户ID必须大于0" }
        
        if (lockType == AppLockConfiguration.LockType.PIN) {
            require(!pin.isNullOrBlank()) { "PIN码不能为空" }
            require(pin.length >= 4) { "PIN码至少4位" }
        }
        
        val configuration = AppLockConfiguration(
            packageName = packageName,
            profileUserId = profileUserId,
            lockType = lockType
        )
        
        return appLockRepository.setAppLock(configuration)
    }
}
