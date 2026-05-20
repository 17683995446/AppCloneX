package com.appclonex.domain.usecase.configuration

import com.appclonex.domain.model.ProfileConfiguration
import com.appclonex.domain.model.OperationError
import com.appclonex.domain.repository.ConfigurationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 导入配置用例
 */
@Singleton
class ImportConfigurationUseCase @Inject constructor(
    private val configurationRepository: ConfigurationRepository
) {
    /**
     * 导入配置文件
     * 
     * @param configuration 配置数据
     * @return 新创建的配置文件ID
     */
    suspend operator fun invoke(configuration: ProfileConfiguration): Result<Int> {
        require(configuration.profile.profileName.isNotBlank()) { "配置文件名称不能为空" }
        return configurationRepository.importConfiguration(configuration)
            .mapError { error ->
                when (error) {
                    is OperationError.UnknownError -> error.throwable
                    else -> Exception(error.toString())
                }
            }
    }
    
    /**
     * 从文件导入配置
     */
    suspend fun fromFile(filePath: String): Result<ProfileConfiguration> {
        require(filePath.isNotBlank()) { "文件路径不能为空" }
        return configurationRepository.importFromFile(filePath)
            .mapError { error ->
                when (error) {
                    is OperationError.UnknownError -> error.throwable
                    else -> Exception(error.toString())
                }
            }
    }
}
