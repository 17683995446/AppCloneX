package com.appclonex.infrastructure.utility

import com.appclonex.domain.model.ClonedApplicationInformationModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/**
 * 应用搜索性能优化引擎

 * 解决 P2 缺陷：搜索性能问题
 * 使用协程 + Flow + Channel 实现异步搜索，避免 UI 线程阻塞
 */
class ApplicationSearchPerformanceOptimizationEngine(
    private val dispatchers: CoroutineDispatcher = Dispatchers.Default
) {
    private val searchScope = CoroutineScope(SupervisorJob() + dispatchers)

    /**
     * 搜索防抖时间（毫秒）
     */
    var debounceDurationMs: Long = 300

    /**
     * 最大搜索结果数量
     */
    var maxResults: Int = 50

    /**
     * 异步搜索 Flow
     * 输入搜索关键词，输出过滤后的结果
     * 内置防抖，避免频繁搜索
     */
    fun searchApplications(
        queryFlow: Flow<String>,
        allApplications: List<ClonedApplicationInformationModel>
    ): Flow<List<ClonedApplicationInformationModel>> {
        return queryFlow
            .debounce(debounceDurationMs)
            .distinctUntilChanged()
            .map { query -> performSearch(query, allApplications) }
            .flowOn(dispatchers)
    }

    /**
     * 带拼音支持的异步搜索
     */
    fun searchApplicationsWithPinyin(
        queryFlow: Flow<String>,
        allApplications: List<ClonedApplicationInformationModel>
    ): Flow<List<ClonedApplicationInformationModel>> {
        return queryFlow
            .debounce(debounceDurationMs)
            .distinctUntilChanged()
            .map { query -> performPinyinSearch(query, allApplications) }
            .flowOn(dispatchers)
    }

    /**
     * 同步搜索（供非 Flow 场景使用）
     */
    suspend fun searchSynchronously(
        query: String,
        allApplications: List<ClonedApplicationInformationModel>
    ): List<ClonedApplicationInformationModel> {
        return withContext(dispatchers) {
            performSearch(query, allApplications)
        }
    }

    /**
     * 执行搜索（核心逻辑）
     */
    private fun performSearch(
        query: String,
        allApplications: List<ClonedApplicationInformationModel>
    ): List<ClonedApplicationInformationModel> {
        if (query.isBlank()) return allApplications.take(maxResults)

        val lowerQuery = query.lowercase().trim()

        return allApplications
            .asSequence()
            .filter { app ->
                app.appName.lowercase().contains(lowerQuery) ||
                app.packageName.lowercase().contains(lowerQuery)
            }
            .take(maxResults)
            .toList()
    }

    /**
     * 执行拼音搜索
     */
    private fun performPinyinSearch(
        query: String,
        allApplications: List<ClonedApplicationInformationModel>
    ): List<ClonedApplicationInformationModel> {
        if (query.isBlank()) return allApplications.take(maxResults)

        val lowerQuery = query.lowercase().trim()

        return allApplications
            .asSequence()
            .filter { app ->
                // 直接匹配
                app.appName.lowercase().contains(lowerQuery) ||
                app.packageName.lowercase().contains(lowerQuery) ||
                // 拼音匹配
                ChinesePinyinSearchQueryConverter.match(app.appName, query) ||
                // 首字母匹配
                ChinesePinyinSearchQueryConverter.getFirstLetters(app.appName)
                    .lowercase()
                    .contains(lowerQuery)
            }
            .take(maxResults)
            .toList()
    }

    /**
     * 取消所有搜索任务
     */
    fun cancelAll() {
        searchScope.cancel()
    }
}

/**
 * 搜索结果统计模型
 */
data class SearchResultStatisticsModel(
    val query: String,
    val resultCount: Int,
    val searchDurationMs: Long,
    val totalApplications: Int
)
