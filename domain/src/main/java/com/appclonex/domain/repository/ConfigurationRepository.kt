package com.appclonex.domain.repository

import com.appclonex.domain.model.ProfileConfiguration
import com.appclonex.domain.model.OperationResult

/**
 * 配置导入导出仓库接口
 */
interface ConfigurationRepository {
    /**
     * 导出配置文件
     * 
     * @param profileId 配置文件ID
     * @return 配置数据
     */
    suspend fun exportConfiguration(profileId: Int): OperationResult<ProfileConfiguration>
    
    /**
     * 导入配置文件
     * 
     * @param configuration 配置数据
     * @return 导入的配置文件ID
     */
    suspend fun importConfiguration(configuration: ProfileConfiguration): OperationResult<Int>
    
    /**
     * 导出配置到文件
     * 
     * @param configuration 配置数据
     * @param filePath 目标文件路径
     * @return 是否成功
     */
    suspend fun exportToFile(configuration: ProfileConfiguration, filePath: String): OperationResult<Boolean>
    
    /**
     * 从文件导入配置
     * 
     * @param filePath 源文件路径
     * @return 配置数据
     */
    suspend fun importFromFile(filePath: String): OperationResult<ProfileConfiguration>

    /**
     * 获取所有可用的配置文件ID列表
     * 用于批量备份等场景
     */
    suspend fun getAllProfileIds(): List<Int>
}
