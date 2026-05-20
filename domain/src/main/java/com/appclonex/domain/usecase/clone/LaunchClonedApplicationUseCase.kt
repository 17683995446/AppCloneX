package com.appclonex.domain.usecase.clone

import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.domain.repository.ClonedApplicationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 启动克隆应用用例
 */
@Singleton
class LaunchClonedApplicationUseCase @Inject constructor(
    private val repository: ClonedApplicationRepository
) {
    suspend operator fun invoke(clonedApp: ClonedApplicationInformationModel): Result<Boolean> {
        require(clonedApp.id > 0) { "应用ID必须大于0" }
        return repository.launchClonedApp(clonedApp)
    }
}
