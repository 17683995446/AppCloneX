package com.appclonex.domain.usecase.clone

import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.domain.repository.ClonedApplicationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 删除克隆应用用例

 * 封装删除克隆应用的业务逻辑
 */
@Singleton
class DeleteClonedApplicationUseCase @Inject constructor(
    private val repository: ClonedApplicationRepository
) {
    /**
     * 执行删除操作

     * @param clonedApp 要删除的克隆应用
     * @return 删除结果
     */
    suspend operator fun invoke(clonedApp: ClonedApplicationInformationModel): Result<Boolean> {
        require(clonedApp.id > 0) { "应用ID必须大于0" }
        return repository.deleteClonedApp(clonedApp)
    }
}
