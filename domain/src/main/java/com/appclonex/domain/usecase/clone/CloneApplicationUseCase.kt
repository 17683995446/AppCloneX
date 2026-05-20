package com.appclonex.domain.usecase.clone

import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.domain.repository.ClonedApplicationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 克隆应用用例

 * 封装克隆应用的业务逻辑，包括：
 * - 验证应用是否可克隆
 * - 执行克隆操作
 * - 更新相关状态

 * 遵循单一职责原则，每个用例只负责一个业务操作
 */
@Singleton
class CloneApplicationUseCase @Inject constructor(
    private val repository: ClonedApplicationRepository
) {
    /**
     * 执行克隆应用操作

     * @param packageName 要克隆的应用包名
     * @param profileUserId 目标配置文件用户ID
     * @return 克隆结果，成功返回克隆的应用信息，失败返回异常
     */
    suspend operator fun invoke(
        packageName: String,
        profileUserId: Int
    ): Result<ClonedApplicationInformationModel> {
        // 验证参数
        require(packageName.isNotBlank()) { "应用包名不能为空" }
        require(profileUserId > 0) { "用户ID必须大于0" }

        return repository.cloneApp(packageName, profileUserId)
    }
}
