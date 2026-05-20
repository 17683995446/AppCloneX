package com.appclonex.domain.usecase.profile

import com.appclonex.domain.repository.UserProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 创建用户配置文件用例
 */
@Singleton
class CreateUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    /**
     * 执行创建操作

     * @param name 配置文件名称
     * @return 创建的用户ID
     */
    suspend operator fun invoke(name: String): Result<Int> {
        require(name.isNotBlank()) { "配置文件名称不能为空" }
        require(name.length <= 20) { "配置文件名称不能超过20个字符" }
        return repository.createProfile(name.trim())
    }
}
