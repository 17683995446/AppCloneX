/*
 * Copyright (c) 2024 AppCloneX
 *
 * 模板市场仓库接口
 * 定义模板分享、导入、评分、搜索、统计等市场功能的业务契约
 */

package com.appclonex.domain.repository

import com.appclonex.domain.model.CloneTemplate
import com.appclonex.domain.model.TemplateCategoryStats
import com.appclonex.domain.model.TemplateImportResult
import com.appclonex.domain.model.TemplateMarketplaceStats
import com.appclonex.domain.model.TemplateRating
import com.appclonex.domain.model.TemplateSearchFilter
import com.appclonex.domain.model.TemplateShareInfo
import com.appclonex.domain.model.TemplateValidationResult
import kotlinx.coroutines.flow.Flow

/**
 * 模板市场仓库接口
 * 提供模板分享、导入、评分、搜索、分类统计等市场核心功能
 *
 * 所有操作使用 [Result] 封装，确保调用方能够正确处理成功和失败场景。
 * 模板更新通过 [observeTemplateUpdates] 响应式通知。
 */
interface TemplateMarketplaceRepository {

    /**
     * 分享模板到市场
     * 为指定模板生成分享码和分享链接，使其可被其他用户导入
     *
     * @param templateId 要分享的模板ID
     * @return 分享信息（包含分享码和链接），失败时返回异常
     */
    suspend fun shareTemplate(templateId: Long): Result<TemplateShareInfo>

    /**
     * 通过分享码导入模板
     * 验证分享码有效性，解析模板数据，检查重复后执行导入
     *
     * @param shareCode 模板分享码
     * @return 导入结果（包含导入的模板或错误信息）
     */
    suspend fun importTemplate(shareCode: String): Result<TemplateImportResult>

    /**
     * 为模板评分
     * 提交用户对模板的评分和评论，同时更新模板的平均评分
     *
     * @param templateId 模板ID
     * @param rating 评分值（1.0 - 5.0）
     * @param comment 评论内容
     * @return 已保存的评分数据
     */
    suspend fun rateTemplate(templateId: Long, rating: Float, comment: String): Result<TemplateRating>

    /**
     * 获取模板的所有评分
     *
     * @param templateId 模板ID
     * @return 评分列表，按时间降序排列
     */
    suspend fun getTemplateRatings(templateId: Long): Result<List<TemplateRating>>

    /**
     * 搜索模板
     * 支持按关键词模糊搜索、分类筛选、最低评分过滤和多字段排序
     *
     * @param filter 搜索过滤条件
     * @return 匹配的模板列表
     */
    suspend fun searchTemplates(filter: TemplateSearchFilter): Result<List<CloneTemplate>>

    /**
     * 获取热门趋势模板
     * 按最近7天下载量排序，返回最热门的模板
     *
     * @param limit 返回数量上限
     * @return 热门模板列表
     */
    suspend fun getTrendingTemplates(limit: Int): Result<List<CloneTemplate>>

    /**
     * 获取热门模板
     * 按总下载量排序，返回最受欢迎的模板
     *
     * @param limit 返回数量上限
     * @return 热门模板列表
     */
    suspend fun getPopularTemplates(limit: Int): Result<List<CloneTemplate>>

    /**
     * 获取最近更新的模板
     * 按更新时间降序排列
     *
     * @param limit 返回数量上限
     * @return 最近更新的模板列表
     */
    suspend fun getRecentlyUpdatedTemplates(limit: Int): Result<List<CloneTemplate>>

    /**
     * 获取各分类的统计数据
     * 包含每个分类的模板数量、总下载量和平均评分
     *
     * @return 分类统计列表
     */
    suspend fun getCategoryStats(): Result<List<TemplateCategoryStats>>

    /**
     * 获取模板市场整体统计数据
     * 包含模板总数、总下载量、总分享数、平均评分和最热门分类
     *
     * @return 市场统计数据
     */
    suspend fun getMarketplaceStats(): Result<TemplateMarketplaceStats>

    /**
     * 验证模板数据的合法性
     * 检查必填字段、包名格式、选项合法性等
     *
     * @param template 待验证的模板
     * @return 验证结果（包含错误和警告列表）
     */
    suspend fun validateTemplate(template: CloneTemplate): Result<TemplateValidationResult>

    /**
     * 生成模板分享码
     * 使用 Base62 编码生成唯一的短分享码
     *
     * @param templateId 模板ID
     * @return 生成的分享码字符串
     */
    suspend fun generateShareCode(templateId: Long): Result<String>

    /**
     * 观察模板列表更新
     * 返回响应式数据流，当模板数据发生变化时自动通知
     *
     * @return 模板列表的 Flow
     */
    fun observeTemplateUpdates(): Flow<List<CloneTemplate>>
}
