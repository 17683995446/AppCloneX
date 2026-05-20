package com.appclonex.domain.usecase.profile

import com.appclonex.domain.repository.UserProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 切换用户配置文件用例
 */
@Singleton
class SwitchUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(userId: Int): Result<Boolean> {
        require(userId > 0) { "用户ID必须大于0" }
        return repository.switchToProfile(userId)
    }
}
