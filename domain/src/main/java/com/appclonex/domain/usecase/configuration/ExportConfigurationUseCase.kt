package com.appclonex.domain.usecase.configuration

import com.appclonex.domain.model.ProfileConfiguration
import com.appclonex.domain.model.OperationError
import com.appclonex.domain.repository.ConfigurationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 导出配置用例
 */
@Singleton
class ExportConfigurationUseCase @Inject constructor(
    private val configurationRepository: ConfigurationRepository
) {
    /**
     * 导出指定配置文件
     */
    suspend operator fun invoke(profileId: Int): Result<ProfileConfiguration> {
        require(profileId > 0) { "配置文件ID必须大于0" }
        return configurationRepository.exportConfiguration(profileId)
            .mapError { error ->
                when (error) {
                    is OperationError.UnknownError -> error.throwable
                    else -> Exception(error.toString())
                }
            }
    }

    /**
     * 获取所有可用的配置文件ID列表
     * 用于批量备份等场景
     */
    suspend fun getAvailableProfileIds(): List<Int> {
        return configurationRepository.getAllProfileIds()
    }
}
