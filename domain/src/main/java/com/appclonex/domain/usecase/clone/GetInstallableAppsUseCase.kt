package com.appclonex.domain.usecase.clone

import com.appclonex.domain.model.InstallableApp
import com.appclonex.domain.repository.ClonedApplicationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 获取可安装应用列表用例
 */
@Singleton
class GetInstallableAppsUseCase @Inject constructor(
    private val repository: ClonedApplicationRepository
) {
    /**
     * 获取可安装的应用列表

     * @param excludeSystem 是否排除系统应用
     * @return 可安装应用列表
     */
    suspend operator fun invoke(excludeSystem: Boolean = false): Result<List<InstallableApp>> {
        return repository.getInstallableApps(excludeSystem)
    }
}
