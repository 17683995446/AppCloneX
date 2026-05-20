package com.appclonex.domain.repository

import com.appclonex.domain.model.CloneTemplate
import kotlinx.coroutines.flow.Flow

/**
 * 克隆模板仓库接口
 * 提供克隆模板的增删改查功能，支持内置模板和自定义模板管理
 */
interface CloneTemplateRepository {

    /**
     * 获取所有模板（内置 + 自定义）
     *
     * @return 模板列表的响应式数据流
     */
    fun getAllTemplates(): Flow<List<CloneTemplate>>

    /**
     * 根据ID获取模板
     *
     * @param id 模板唯一标识
     * @return 模板对象，不存在时返回null
     */
    suspend fun getTemplateById(id: Long): CloneTemplate?

    /**
     * 获取所有内置模板
     *
     * @return 内置模板列表
     */
    suspend fun getBuiltInTemplates(): List<CloneTemplate>

    /**
     * 获取所有用户自定义模板
     *
     * @return 自定义模板列表
     */
    suspend fun getCustomTemplates(): List<CloneTemplate>

    /**
     * 插入新模板
     *
     * @param template 要插入的模板
     * @return 新插入模板的ID
     */
    suspend fun insertTemplate(template: CloneTemplate): Long

    /**
     * 更新模板
     *
     * @param template 要更新的模板（通过id匹配）
     */
    suspend fun updateTemplate(template: CloneTemplate)

    /**
     * 删除模板
     * 注意：内置模板不可删除
     *
     * @param id 要删除的模板ID
     */
    suspend fun deleteTemplate(id: Long)

    /**
     * 增加模板使用次数
     *
     * @param id 模板ID
     */
    suspend fun incrementUsageCount(id: Long)
}
