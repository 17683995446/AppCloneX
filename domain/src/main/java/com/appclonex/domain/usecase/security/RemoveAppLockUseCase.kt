package com.appclonex.domain.usecase.security

import com.appclonex.domain.repository.AppLockRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 移除应用锁用例
 */
@Singleton
class RemoveAppLockUseCase @Inject constructor(
    private val appLockRepository: AppLockRepository
) {
    suspend operator fun invoke(packageName: String, profileUserId: Int): Result<Boolean> {
        require(packageName.isNotBlank()) { "应用包名不能为空" }
        require(profileUserId > 0) { "用户ID必须大于0" }
        
        return appLockRepository.removeAppLock(packageName, profileUserId)
    }
}
