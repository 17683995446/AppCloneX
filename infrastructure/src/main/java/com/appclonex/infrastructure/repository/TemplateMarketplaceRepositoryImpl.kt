/*
 * Copyright (c) 2024 AppCloneX
 *
 * 模板市场仓库实现
 * 使用 JSON 文件持久化实现模板分享、导入、评分、搜索、统计等功能
 */

package com.appclonex.infrastructure.repository

import android.content.Context
import com.appclonex.domain.model.BuiltInTemplates
import com.appclonex.domain.model.CloneTemplate
import com.appclonex.domain.model.SortField
import com.appclonex.domain.model.SortOrder
import com.appclonex.domain.model.TemplateCategory
import com.appclonex.domain.model.TemplateCategoryStats
import com.appclonex.domain.model.TemplateImportResult
import com.appclonex.domain.model.TemplateMarketplaceStats
import com.appclonex.domain.model.TemplateRating
import com.appclonex.domain.model.TemplateSearchFilter
import com.appclonex.domain.model.TemplateShareInfo
import com.appclonex.domain.model.TemplateValidationResult
import com.appclonex.domain.repository.CloneTemplateRepository
import com.appclonex.domain.repository.TemplateMarketplaceRepository
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 模板市场仓库实现
 * 使用 JSON 文件持久化模板市场数据，包括分享信息、评分记录等
 *
 * 存储文件:
 * - marketplace_shares.json: 分享信息（分享码 -> 模板数据映射）
 * - marketplace_ratings.json: 评分记录
 * - marketplace_templates.json: 市场模板（已分享的模板副本）
 *
 * 线程安全: 使用 [Mutex] 保证并发操作的数据一致性
 *
 * @property context Android 应用上下文
 * @property templateRepository 克隆模板仓库（用于读写本地模板）
 */
@Singleton
class TemplateMarketplaceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val templateRepository: CloneTemplateRepository
) : TemplateMarketplaceRepository {

    companion object {
        private const val TAG = "TemplateMarketplaceRepo"

        /** 分享信息持久化文件名 */
        private const val SHARES_FILE_NAME = "marketplace_shares.json"

        /** 评分记录持久化文件名 */
        private const val RATINGS_FILE_NAME = "marketplace_ratings.json"

        /** 市场模板持久化文件名 */
        private const val MARKETPLACE_TEMPLATES_FILE_NAME = "marketplace_templates.json"

        /** 分享码长度（Base62编码的UUID前N位） */
        private const val SHARE_CODE_LENGTH = 8

        /** Base62 字符集 */
        private const val BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

        /** 趋势计算的时间窗口（7天，毫秒） */
        private const val TRENDING_WINDOW_MS = 7L * 24 * 60 * 60 * 1000

        /** 默认热门/趋势模板数量上限 */
        private const val DEFAULT_LIMIT = 20
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /** 分享码序列生成器，确保唯一性 */
    private val shareCodeSequence = AtomicLong(System.currentTimeMillis())

    /** 互斥锁，保证并发操作的数据一致性 */
    private val mutex = Mutex()

    // ==================== 持久化文件路径 ====================

    private val sharesFile: File
        get() = File(context.filesDir, SHARES_FILE_NAME)

    private val ratingsFile: File
        get() = File(context.filesDir, RATINGS_FILE_NAME)

    private val marketplaceTemplatesFile: File
        get() = File(context.filesDir, MARKETPLACE_TEMPLATES_FILE_NAME)

    // ==================== 内存缓存 ====================

    /**
     * 分享信息缓存: shareCode -> ShareEntry
     * ShareEntry 包含模板数据和分享元信息
     */
    private data class ShareEntry(
        val template: CloneTemplate,
        val shareInfo: TemplateShareInfo
    )

    private val sharesCache = MutableStateFlow<Map<String, ShareEntry>>(emptyMap())

    /**
     * 评分记录缓存: templateId -> List<TemplateRating>
     */
    private val ratingsCache = MutableStateFlow<Map<Long, List<TemplateRating>>>(emptyMap())

    /**
     * 市场模板缓存（已分享的模板列表）
     */
    private val marketplaceTemplates = MutableStateFlow<List<CloneTemplate>>(emptyList())

    init {
        loadAllData()
    }

    // ==================== 公开接口实现 ====================

    override suspend fun shareTemplate(templateId: Long): Result<TemplateShareInfo> {
        return mutex.withLock {
            try {
                // 查找模板
                val template = templateRepository.getTemplateById(templateId)
                    ?: return Result.failure(IllegalArgumentException("模板不存在: ID=$templateId"))

                // 生成分享码
                val shareCode = generateBase62Code()

                // 创建分享信息
                val shareInfo = TemplateShareInfo(
                    shareCode = shareCode,
                    shareUrl = "appclonex://template/$shareCode",
                    downloadCount = 0,
                    shareCount = 1,
                    createdAt = System.currentTimeMillis()
                )

                // 保存分享信息到缓存
                val currentShares = sharesCache.value.toMutableMap()
                currentShares[shareCode] = ShareEntry(template, shareInfo)
                sharesCache.value = currentShares

                // 将模板添加到市场模板列表
                val currentMarketTemplates = marketplaceTemplates.value.toMutableList()
                // 检查是否已存在于市场中
                if (currentMarketTemplates.none { it.id == templateId }) {
                    currentMarketTemplates.add(template)
                    marketplaceTemplates.value = currentMarketTemplates
                }

                // 持久化
                persistShares()
                persistMarketplaceTemplates()

                ApplicationLoggingServiceManager.i(TAG, "模板已分享: ${template.name} (分享码: $shareCode)")
                Result.success(shareInfo)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "分享模板失败: ID=$templateId", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun importTemplate(shareCode: String): Result<TemplateImportResult> {
        return mutex.withLock {
            try {
                val trimmedCode = shareCode.trim()
                if (trimmedCode.isBlank()) {
                    return Result.failure(IllegalArgumentException("分享码不能为空"))
                }

                // 查找分享信息
                val shareEntry = sharesCache.value[trimmedCode]
                    ?: return Result.success(
                        TemplateImportResult(
                            success = false,
                            errorMessage = "分享码无效或已过期: $trimmedCode"
                        )
                    )

                val sourceTemplate = shareEntry.template

                // 验证模板
                val validationResult = validateTemplateInternal(sourceTemplate)
                if (!validationResult.isValid) {
                    return Result.success(
                        TemplateImportResult(
                            success = false,
                            errorMessage = "模板数据验证失败: ${validationResult.errors.joinToString()}"
                        )
                    )
                }

                // 检查本地是否已存在相同模板（通过名称和包名组合判断）
                val existingTemplates = templateRepository.getCustomTemplates()
                val isDuplicate = existingTemplates.any { existing ->
                    existing.name == sourceTemplate.name &&
                        existing.appPackages == sourceTemplate.appPackages
                }

                if (isDuplicate) {
                    return Result.success(
                        TemplateImportResult(
                            success = false,
                            errorMessage = "本地已存在相同模板: ${sourceTemplate.name}"
                        )
                    )
                }

                // 创建新模板（去除内置标记，设置新的ID和时间戳）
                val newTemplate = sourceTemplate.copy(
                    id = 0, // 由仓库自动分配新ID
                    isBuiltIn = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    usageCount = 0
                )

                // 插入到本地模板仓库
                val newId = templateRepository.insertTemplate(newTemplate)
                val importedTemplate = newTemplate.copy(id = newId)

                // 更新下载计数
                val currentShares = sharesCache.value.toMutableMap()
                val updatedEntry = currentShares[trimmedCode]
                if (updatedEntry != null) {
                    currentShares[trimmedCode] = updatedEntry.copy(
                        shareInfo = updatedEntry.shareInfo.copy(
                            downloadCount = updatedEntry.shareInfo.downloadCount + 1
                        )
                    )
                    sharesCache.value = currentShares
                    persistShares()
                }

                ApplicationLoggingServiceManager.i(
                    TAG,
                    "模板导入成功: ${importedTemplate.name} (新ID: $newId)"
                )
                Result.success(
                    TemplateImportResult(
                        success = true,
                        template = importedTemplate
                    )
                )
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "导入模板失败: 分享码=$shareCode", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun rateTemplate(
        templateId: Long,
        rating: Float,
        comment: String
    ): Result<TemplateRating> {
        return mutex.withLock {
            try {
                val newRating = TemplateRating(
                    userId = "local_user",
                    userName = "本地用户",
                    rating = rating.coerceIn(1f, 5f),
                    comment = comment,
                    timestamp = System.currentTimeMillis()
                )

                // 保存评分到缓存
                val currentRatings = ratingsCache.value.toMutableMap()
                val templateRatings = currentRatings[templateId]?.toMutableList() ?: mutableListOf()

                // 如果用户已有评分，替换旧评分；否则添加新评分
                val existingIndex = templateRatings.indexOfFirst { it.userId == newRating.userId }
                if (existingIndex >= 0) {
                    templateRatings[existingIndex] = newRating
                } else {
                    templateRatings.add(newRating)
                }

                // 按时间降序排列
                currentRatings[templateId] = templateRatings.sortedByDescending { it.timestamp }
                ratingsCache.value = currentRatings

                // 持久化评分
                persistRatings()

                ApplicationLoggingServiceManager.i(TAG, "模板已评分: ID=$templateId, 评分=$rating")
                Result.success(newRating)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "评分失败: ID=$templateId", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getTemplateRatings(templateId: Long): Result<List<TemplateRating>> {
        return Result.success(
            ratingsCache.value[templateId] ?: emptyList()
        )
    }

    override suspend fun searchTemplates(filter: TemplateSearchFilter): Result<List<CloneTemplate>> {
        return mutex.withLock {
            try {
                // 在锁内获取缓存快照，避免在锁外访问共享状态
                val templatesSnapshot = marketplaceTemplates.value
                val ratingsSnapshot = ratingsCache.value
                val sharesSnapshot = sharesCache.value

                var results = templatesSnapshot.toMutableList()

                // 关键词模糊搜索（匹配名称和描述）
                if (filter.query.isNotBlank()) {
                    val query = filter.query.lowercase().trim()
                    results = results.filter { template ->
                        template.name.lowercase().contains(query) ||
                            template.description.lowercase().contains(query)
                    }.toMutableList()
                }

                // 分类筛选
                if (filter.categories.isNotEmpty()) {
                    results = results.filter { it.category in filter.categories }.toMutableList()
                }

                // 最低评分过滤（使用快照数据）
                if (filter.minRating > 0f) {
                    results = results.filter { template ->
                        getAverageRatingFromSnapshot(template.id, ratingsSnapshot) >= filter.minRating
                    }.toMutableList()
                }

                // 排序（使用快照数据）
                results = when (filter.sortBy) {
                    SortField.NAME -> {
                        if (filter.sortOrder == SortOrder.ASCENDING) {
                            results.sortedBy { it.name.lowercase() }
                        } else {
                            results.sortedByDescending { it.name.lowercase() }
                        }
                    }
                    SortField.RATING -> {
                        if (filter.sortOrder == SortOrder.ASCENDING) {
                            results.sortedBy { getAverageRatingFromSnapshot(it.id, ratingsSnapshot) }
                        } else {
                            results.sortedByDescending { getAverageRatingFromSnapshot(it.id, ratingsSnapshot) }
                        }
                    }
                    SortField.DOWNLOADS -> {
                        if (filter.sortOrder == SortOrder.ASCENDING) {
                            results.sortedBy { getDownloadCountFromSnapshot(it.id, sharesSnapshot) }
                        } else {
                            results.sortedByDescending { getDownloadCountFromSnapshot(it.id, sharesSnapshot) }
                        }
                    }
                    SortField.CREATED -> {
                        if (filter.sortOrder == SortOrder.ASCENDING) {
                            results.sortedBy { it.createdAt }
                        } else {
                            results.sortedByDescending { it.createdAt }
                        }
                    }
                    SortField.UPDATED -> {
                        if (filter.sortOrder == SortOrder.ASCENDING) {
                            results.sortedBy { it.updatedAt }
                        } else {
                            results.sortedByDescending { it.updatedAt }
                        }
                    }
                }.toMutableList()

                Result.success(results)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "搜索模板失败", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getTrendingTemplates(limit: Int): Result<List<CloneTemplate>> {
        return mutex.withLock {
            try {
                val effectiveLimit = limit.coerceAtLeast(1)
                val now = System.currentTimeMillis()
                val windowStart = now - TRENDING_WINDOW_MS

                // 按最近7天下载量排序
                val sorted = marketplaceTemplates.value
                    .filter { it.updatedAt >= windowStart || it.createdAt >= windowStart }
                    .sortedByDescending { getDownloadCount(it.id) }

                Result.success(sorted.take(effectiveLimit))
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "获取热门趋势模板失败", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getPopularTemplates(limit: Int): Result<List<CloneTemplate>> {
        return mutex.withLock {
            try {
                val effectiveLimit = limit.coerceAtLeast(1)

                // 按总下载量排序
                val sorted = marketplaceTemplates.value
                    .sortedByDescending { getDownloadCount(it.id) }

                Result.success(sorted.take(effectiveLimit))
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "获取热门模板失败", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getRecentlyUpdatedTemplates(limit: Int): Result<List<CloneTemplate>> {
        return mutex.withLock {
            try {
                val effectiveLimit = limit.coerceAtLeast(1)

                // 按更新时间降序排列
                val sorted = marketplaceTemplates.value
                    .sortedByDescending { it.updatedAt }

                Result.success(sorted.take(effectiveLimit))
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "获取最近更新模板失败", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getCategoryStats(): Result<List<TemplateCategoryStats>> {
        return mutex.withLock {
            try {
                val stats = TemplateCategory.entries.map { category ->
                    val templates = marketplaceTemplates.value.filter { it.category == category }
                    val totalDownloads = templates.sumOf { getDownloadCount(it.id).toLong() }.toInt()
                    val avgRating = if (templates.isNotEmpty()) {
                        templates.map { getAverageRating(it.id) }.average().toFloat()
                    } else {
                        0f
                    }

                    TemplateCategoryStats(
                        category = category,
                        templateCount = templates.size,
                        totalDownloads = totalDownloads,
                        averageRating = avgRating
                    )
                }
                Result.success(stats)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "获取分类统计失败", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getMarketplaceStats(): Result<TemplateMarketplaceStats> {
        return mutex.withLock {
            try {
                val templates = marketplaceTemplates.value
                val totalDownloads = sharesCache.value.values.sumOf {
                    it.shareInfo.downloadCount.toLong()
                }.toInt()
                val totalShares = sharesCache.value.values.sumOf {
                    it.shareInfo.shareCount.toLong()
                }.toInt()

                val allRatings = ratingsCache.value.values.flatten()
                val averageRating = if (allRatings.isNotEmpty()) {
                    allRatings.map { it.rating }.average().toFloat()
                } else {
                    0f
                }

                // 找出模板数量最多的分类
                val topCategory = TemplateCategory.entries
                    .maxByOrNull { category ->
                        templates.count { it.category == category }
                    }
                    ?.takeIf { templates.any { t -> t.category == it } }

                Result.success(
                    TemplateMarketplaceStats(
                        totalTemplates = templates.size,
                        totalDownloads = totalDownloads,
                        totalShares = totalShares,
                        averageRating = averageRating,
                        topCategory = topCategory
                    )
                )
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "获取市场统计失败", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun validateTemplate(template: CloneTemplate): Result<TemplateValidationResult> {
        return Result.success(validateTemplateInternal(template))
    }

    override suspend fun generateShareCode(templateId: Long): Result<String> {
        return try {
            val template = templateRepository.getTemplateById(templateId)
                ?: return Result.failure(IllegalArgumentException("模板不存在: ID=$templateId"))

            // 确保分享码唯一
            val code = generateUniqueShareCode()
            ApplicationLoggingServiceManager.d(TAG, "生成分享码: $code (模板: ${template.name})")
            Result.success(code)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "生成分享码失败: ID=$templateId", e)
            Result.failure(e)
        }
    }

    override fun observeTemplateUpdates(): Flow<List<CloneTemplate>> {
        return marketplaceTemplates.asStateFlow()
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 内部模板验证逻辑
     * 检查必填字段、包名格式、选项合法性等
     *
     * @param template 待验证的模板
     * @return 验证结果
     */
    private fun validateTemplateInternal(template: CloneTemplate): TemplateValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // 检查必填字段
        if (template.name.isBlank()) {
            errors.add("模板名称不能为空")
        }
        if (template.name.length > 50) {
            warnings.add("模板名称过长（超过50个字符），可能影响显示效果")
        }

        // 检查描述
        if (template.description.length > 200) {
            warnings.add("模板描述过长（超过200个字符），建议精简")
        }

        // 检查包名格式
        val invalidPackages = template.appPackages.filter { packageName ->
            !isValidPackageName(packageName)
        }
        if (invalidPackages.isNotEmpty()) {
            errors.add("包含无效的包名格式: ${invalidPackages.joinToString(", ")}")
        }

        // 检查克隆选项合法性
        if (template.options.maxParallel < 1 || template.options.maxParallel > 10) {
            warnings.add("最大并行数建议在 1-10 之间，当前值: ${template.options.maxParallel}")
        }

        return TemplateValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * 验证 Android 包名格式
     * 包名应至少包含一个点号，且各段以字母开头
     *
     * @param packageName 包名字符串
     * @return 是否为合法包名
     */
    private fun isValidPackageName(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        val parts = packageName.split(".")
        return parts.size >= 2 && parts.all { part ->
            part.isNotBlank() && part[0].isLetter() && part.all { ch -> ch.isLetterOrDigit() || ch == '_' }
        }
    }

    /**
     * 生成 Base62 编码的唯一分享码
     * 使用 UUID 的前 N 位字符进行 Base62 编码
     *
     * @return 分享码字符串
     */
    private fun generateBase62Code(): String {
        val uuid = UUID.randomUUID().toString().replace("-", "")
        return encodeBase62(uuid).take(SHARE_CODE_LENGTH)
    }

    /**
     * 生成确保唯一的分享码
     * 循环生成直到不与已有分享码冲突
     *
     * @return 唯一的分享码
     */
    private fun generateUniqueShareCode(): String {
        var code: String
        var attempts = 0
        do {
            code = generateBase62Code()
            attempts++
            if (attempts > 100) {
                // 极端情况下使用时间戳+序列号保证唯一
                code = encodeBase62(
                    "${System.currentTimeMillis()}_${shareCodeSequence.getAndIncrement()}"
                ).take(SHARE_CODE_LENGTH)
                break
            }
        } while (sharesCache.value.containsKey(code))
        return code
    }

    /**
     * Base62 编码
     * 将字符串转换为 Base62 编码格式
     *
     * @param input 输入字符串
     * @return Base62 编码结果
     */
    private fun encodeBase62(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val sb = StringBuilder()
        for (byte in bytes) {
            val unsignedByte = byte.toInt() and 0xFF
            sb.append(BASE62_CHARS[unsignedByte % BASE62_CHARS.length])
        }
        return sb.toString()
    }

    /**
     * 获取模板的平均评分
     *
     * @param templateId 模板ID
     * @return 平均评分，无评分时返回 0f
     */
    private fun getAverageRating(templateId: Long): Float {
        return getAverageRatingFromSnapshot(templateId, ratingsCache.value)
    }

    /**
     * 从快照获取模板的平均评分（线程安全版本）
     *
     * @param templateId 模板ID
     * @param ratingsSnapshot 评分缓存快照
     * @return 平均评分，无评分时返回 0f
     */
    private fun getAverageRatingFromSnapshot(
        templateId: Long,
        ratingsSnapshot: Map<Long, List<TemplateRating>>
    ): Float {
        val ratings = ratingsSnapshot[templateId] ?: return 0f
        return if (ratings.isNotEmpty()) {
            ratings.map { it.rating }.average().toFloat()
        } else {
            0f
        }
    }

    /**
     * 获取模板的下载量
     *
     * @param templateId 模板ID
     * @return 下载次数
     */
    private fun getDownloadCount(templateId: Long): Int {
        return getDownloadCountFromSnapshot(templateId, sharesCache.value)
    }

    /**
     * 从快照获取模板的下载量（线程安全版本）
     *
     * @param templateId 模板ID
     * @param sharesSnapshot 分享缓存快照
     * @return 下载次数
     */
    private fun getDownloadCountFromSnapshot(
        templateId: Long,
        sharesSnapshot: Map<String, ShareEntry>
    ): Int {
        return sharesSnapshot.values
            .filter { it.template.id == templateId }
            .sumOf { it.shareInfo.downloadCount }
    }

    // ==================== 数据加载与持久化 ====================

    /**
     * 加载所有持久化数据
     * 初始化时调用，从 JSON 文件恢复分享信息和评分记录
     */
    private fun loadAllData() {
        try {
            loadShares()
            loadRatings()
            loadMarketplaceTemplates()
            ApplicationLoggingServiceManager.i(
                TAG,
                "市场数据加载完成: ${sharesCache.value.size} 个分享, " +
                    "${ratingsCache.value.size} 个模板有评分, " +
                    "${marketplaceTemplates.value.size} 个市场模板"
            )
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "加载市场数据失败，使用空数据", e)
        }
    }

    /**
     * 从文件加载分享信息
     */
    @Suppress("UNCHECKED_CAST")
    private fun loadShares() {
        val file = sharesFile
        if (!file.exists()) return

        try {
            val jsonString = file.readText(Charsets.UTF_8)
            if (jsonString.isBlank()) return

            val data = json.decodeFromString<Map<String, Map<String, Any>>>(jsonString)
            val shares = data.mapNotNull { (code, entry) ->
                try {
                    val templateJson = json.encodeToString(entry["template"] ?: return@mapNotNull null)
                    val shareInfoJson = json.encodeToString(entry["shareInfo"] ?: return@mapNotNull null)
                    val template = json.decodeFromString<CloneTemplate>(templateJson)
                    val shareInfo = json.decodeFromString<TemplateShareInfo>(shareInfoJson)
                    code to ShareEntry(template, shareInfo)
                } catch (e: Exception) {
                    ApplicationLoggingServiceManager.w(TAG, "解析分享条目失败: $code", e)
                    null
                }
            }.toMap()
            sharesCache.value = shares
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "加载分享信息失败", e)
        }
    }

    /**
     * 从文件加载评分记录
     */
    private fun loadRatings() {
        val file = ratingsFile
        if (!file.exists()) return

        try {
            val jsonString = file.readText(Charsets.UTF_8)
            if (jsonString.isBlank()) return

            val data = json.decodeFromString<Map<String, List<TemplateRating>>>(jsonString)
            // R13-003 修复：将 return 改为 filterKeys + mapKeys
            // 之前使用 `key.toLongOrNull() ?: return` 会在遇到无效键时直接退出整个函数
            // 导致 ratingsCache 永远不会被更新，所有评分数据丢失
            val ratings = data
                .filterKeys { it.toLongOrNull() != null }
                .mapKeys { (key, _) -> key.toLong() }
            ratingsCache.value = ratings
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "加载评分记录失败", e)
        }
    }

    /**
     * 从文件加载市场模板列表
     */
    private fun loadMarketplaceTemplates() {
        val file = marketplaceTemplatesFile
        if (!file.exists()) return

        try {
            val jsonString = file.readText(Charsets.UTF_8)
            if (jsonString.isBlank()) return

            val templates = json.decodeFromString<List<CloneTemplate>>(jsonString)
            marketplaceTemplates.value = templates
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "加载市场模板失败", e)
        }
    }

    /**
     * 持久化分享信息到文件
     */
    private fun persistShares() {
        try {
            // 将 ShareEntry 转换为可序列化的 Map
            val data = sharesCache.value.mapValues { (_, entry) ->
                mapOf(
                    "template" to entry.template,
                    "shareInfo" to entry.shareInfo
                )
            }
            val jsonString = json.encodeToString(data)
            atomicWrite(sharesFile, jsonString)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "持久化分享信息失败", e)
        }
    }

    /**
     * 持久化评分记录到文件
     */
    private fun persistRatings() {
        try {
            // 将 Long 键转换为 String 键以兼容 JSON 序列化
            val data = ratingsCache.value.mapKeys { (key, _) -> key.toString() }
            val jsonString = json.encodeToString(data)
            atomicWrite(ratingsFile, jsonString)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "持久化评分记录失败", e)
        }
    }

    /**
     * 持久化市场模板到文件
     */
    private fun persistMarketplaceTemplates() {
        try {
            val jsonString = json.encodeToString(marketplaceTemplates.value)
            atomicWrite(marketplaceTemplatesFile, jsonString)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "持久化市场模板失败", e)
        }
    }

    /**
     * 原子写入文件
     * 先写入临时文件，再重命名为目标文件，避免写入中断导致数据损坏
     *
     * @param file 目标文件
     * @param content 文件内容
     */
    private fun atomicWrite(file: File, content: String) {
        val tempFile = File(file.parent, "${file.name}.tmp")
        try {
            tempFile.writeText(content, Charsets.UTF_8)
            val renamed = tempFile.renameTo(file)
            if (!renamed) {
                file.writeText(content, Charsets.UTF_8)
            }
        } finally {
            tempFile.delete()
        }
    }
}
