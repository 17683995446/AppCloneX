/*
 * Copyright (c) 2024 AppCloneX
 *
 * 模板市场领域模型
 * 定义模板分享、评分、搜索、分类筛选相关的数据结构
 */

package com.appclonex.domain.model

import kotlinx.serialization.Serializable

// ==================== 模板评分相关模型 ====================

/**
 * 模板评分数据类
 * 表示用户对某个模板的评分和评论
 *
 * @property userId 用户唯一标识
 * @property userName 用户名称
 * @property rating 评分值，范围 1.0 - 5.0
 * @property comment 用户评论内容
 * @property timestamp 评分时间戳（毫秒）
 */
@Serializable
data class TemplateRating(
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// ==================== 模板分享相关模型 ====================

/**
 * 模板分享信息数据类
 * 包含模板分享的元数据和统计数据
 *
 * @property shareCode 分享唯一码（Base62编码）
 * @property shareUrl 分享链接
 * @property downloadCount 下载/导入次数
 * @property shareCount 分享次数
 * @property createdAt 分享创建时间戳（毫秒）
 */
@Serializable
data class TemplateShareInfo(
    val shareCode: String = "",
    val shareUrl: String = "",
    val downloadCount: Int = 0,
    val shareCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// ==================== 搜索与筛选相关模型 ====================

/**
 * 搜索排序字段枚举
 * 定义模板搜索结果的可排序字段
 */
@Serializable
enum class SortField {
    /** 按模板名称排序 */
    NAME,
    /** 按平均评分排序 */
    RATING,
    /** 按下载量排序 */
    DOWNLOADS,
    /** 按创建时间排序 */
    CREATED,
    /** 按更新时间排序 */
    UPDATED
}

/**
 * 搜索排序顺序枚举
 */
@Serializable
enum class SortOrder {
    /** 升序排列 */
    ASCENDING,
    /** 降序排列 */
    DESCENDING
}

/**
 * 模板搜索过滤条件数据类
 * 封装模板市场搜索和筛选的所有参数
 *
 * @property query 搜索关键词（模糊匹配名称和描述）
 * @property categories 分类筛选列表，为空表示不筛选
 * @property minRating 最低评分阈值，0 表示不筛选
 * @property sortBy 排序字段
 * @property sortOrder 排序顺序
 */
@Serializable
data class TemplateSearchFilter(
    val query: String = "",
    val categories: List<TemplateCategory> = emptyList(),
    val minRating: Float = 0f,
    val sortBy: SortField = SortField.UPDATED,
    val sortOrder: SortOrder = SortOrder.DESCENDING
)

// ==================== 分类统计相关模型 ====================

/**
 * 模板分类统计数据类
 * 表示某个分类下的模板统计信息
 *
 * @property category 模板分类
 * @property templateCount 该分类下的模板数量
 * @property totalDownloads 该分类的总下载量
 * @property averageRating 该分类的平均评分
 */
@Serializable
data class TemplateCategoryStats(
    val category: TemplateCategory = TemplateCategory.GENERAL,
    val templateCount: Int = 0,
    val totalDownloads: Int = 0,
    val averageRating: Float = 0f
)

/**
 * 模板市场整体统计数据类
 * 展示模板市场的全局概览数据
 *
 * @property totalTemplates 模板总数
 * @property totalDownloads 总下载量
 * @property totalShares 总分享次数
 * @property averageRating 全市场平均评分
 * @property topCategory 最热门分类
 */
@Serializable
data class TemplateMarketplaceStats(
    val totalTemplates: Int = 0,
    val totalDownloads: Int = 0,
    val totalShares: Int = 0,
    val averageRating: Float = 0f,
    val topCategory: TemplateCategory? = null
)

// ==================== 模板导入与验证相关模型 ====================

/**
 * 模板导入结果数据类
 * 表示通过分享码导入模板的结果
 *
 * @property success 是否导入成功
 * @property template 导入成功时的模板对象
 * @property errorMessage 导入失败时的错误信息
 */
@Serializable
data class TemplateImportResult(
    val success: Boolean = false,
    val template: CloneTemplate? = null,
    val errorMessage: String? = null
)

/**
 * 模板验证结果数据类
 * 表示模板数据合法性校验的结果
 *
 * @property isValid 模板是否合法
 * @property errors 错误列表（阻止导入的严重问题）
 * @property warnings 警告列表（不影响导入但需注意的问题）
 */
@Serializable
data class TemplateValidationResult(
    val isValid: Boolean = true,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)
