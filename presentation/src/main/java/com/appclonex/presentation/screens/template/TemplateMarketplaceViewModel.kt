/*
 * Copyright (c) 2024 AppCloneX
 *
 * 模板市场界面 ViewModel
 * 管理模板市场的搜索、分享、导入、评分等操作的 UI 状态
 */

package com.appclonex.presentation.screens.template

import androidx.lifecycle.viewModelScope
import com.appclonex.domain.model.CloneTemplate
import com.appclonex.domain.model.SortField
import com.appclonex.domain.model.SortOrder
import com.appclonex.domain.model.TemplateCategory
import com.appclonex.domain.model.TemplateRating
import com.appclonex.domain.model.TemplateSearchFilter
import com.appclonex.domain.model.TemplateShareInfo
import com.appclonex.domain.usecase.template.ImportTemplateUseCase
import com.appclonex.domain.usecase.template.RateTemplateUseCase
import com.appclonex.domain.usecase.template.SearchTemplatesUseCase
import com.appclonex.domain.usecase.template.ShareTemplateUseCase
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 模板市场界面 UI 状态
 * 包含搜索过滤、模板列表、加载状态、对话框状态等
 *
 * @property templates 当前显示的模板列表
 * @property searchQuery 搜索关键词
 * @property selectedCategories 已选中的分类筛选列表
 * @property sortField 当前排序字段
 * @property sortOrder 当前排序顺序
 * @property isLoading 是否正在加载
 * @property isRefreshing 是否正在下拉刷新
 * @property error 错误消息
 * @property message 提示消息
 * @property successMessage 成功消息
 * @property selectedTemplate 当前选中的模板（用于详情弹窗）
 * @property showDetailSheet 是否显示模板详情底部弹窗
 * @property showShareDialog 是否显示分享对话框
 * @property showImportDialog 是否显示导入对话框
 * @property showRatingDialog 是否显示评分对话框
 * @property shareInfo 分享信息（分享成功后填充）
 * @property shareProgress 分享进度状态
 * @property importProgress 导入进度状态
 * @property templateRatings 当前查看的模板评分列表
 * @property importShareCode 导入对话框中输入的分享码
 */
data class TemplateMarketplaceUiState(
    val templates: List<CloneTemplate> = emptyList(),
    val searchQuery: String = "",
    val selectedCategories: List<TemplateCategory> = emptyList(),
    val sortField: SortField = SortField.UPDATED,
    val sortOrder: SortOrder = SortOrder.DESCENDING,
    override val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    override val error: String? = null,
    override val message: String? = null,
    val successMessage: String? = null,
    val selectedTemplate: CloneTemplate? = null,
    val showDetailSheet: Boolean = false,
    val showShareDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val showRatingDialog: Boolean = false,
    val shareInfo: TemplateShareInfo? = null,
    val shareProgress: ShareProgressUiState = ShareProgressUiState.Idle,
    val importProgress: ImportProgressUiState = ImportProgressUiState.Idle,
    val templateRatings: List<TemplateRating> = emptyList(),
    val importShareCode: String = ""
) : BaseUiState

/**
 * 分享进度 UI 状态
 */
sealed interface ShareProgressUiState {
    /** 空闲状态 */
    data object Idle : ShareProgressUiState
    /** 正在准备 */
    data object Preparing : ShareProgressUiState
    /** 正在生成分享码 */
    data object GeneratingCode : ShareProgressUiState
    /** 正在上传 */
    data object Uploading : ShareProgressUiState
    /** 分享完成 */
    data class Completed(val shareInfo: TemplateShareInfo) : ShareProgressUiState
    /** 分享失败 */
    data class Error(val message: String) : ShareProgressUiState
}

/**
 * 导入进度 UI 状态
 */
sealed interface ImportProgressUiState {
    /** 空闲状态 */
    data object Idle : ImportProgressUiState
    /** 正在验证 */
    data object Validating : ImportProgressUiState
    /** 正在下载 */
    data object Downloading : ImportProgressUiState
    /** 正在解析 */
    data object Parsing : ImportProgressUiState
    /** 正在合并 */
    data object Merging : ImportProgressUiState
    /** 导入完成 */
    data class Completed(val template: CloneTemplate) : ImportProgressUiState
    /** 导入失败 */
    data class Error(val message: String) : ImportProgressUiState
}

/**
 * 模板市场界面 ViewModel
 * 管理模板市场的搜索、分享、导入、评分等操作
 *
 * 功能:
 * - 搜索防抖（300ms）
 * - 分类筛选和排序切换
 * - 分享/导入/评分操作的状态管理
 * - 下拉刷新
 * - 模板详情、分享、导入、评分对话框的状态控制
 *
 * @property searchTemplatesUseCase 搜索模板用例
 * @property shareTemplateUseCase 分享模板用例
 * @property importTemplateUseCase 导入模板用例
 * @property rateTemplateUseCase 评分模板用例
 */
@HiltViewModel
class TemplateMarketplaceViewModel @Inject constructor(
    private val searchTemplatesUseCase: SearchTemplatesUseCase,
    private val shareTemplateUseCase: ShareTemplateUseCase,
    private val importTemplateUseCase: ImportTemplateUseCase,
    private val rateTemplateUseCase: RateTemplateUseCase
) : BaseViewModel<TemplateMarketplaceUiState>(TemplateMarketplaceUiState()) {

    /** 搜索防抖任务 */
    private var searchDebounceJob: Job? = null

    /** 搜索防抖延迟时间（毫秒） */
    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
    }

    init {
        loadTemplates()
    }

    // ==================== 搜索与筛选 ====================

    /**
     * 加载模板列表
     * 使用当前过滤条件执行搜索
     *
     * @param isRefresh 是否为下拉刷新操作
     */
    fun loadTemplates(isRefresh: Boolean = false) {
        launchWithErrorHandler {
            if (isRefresh) {
                updateState { copy(isRefreshing = true, error = null) }
            } else {
                updateState { copy(isLoading = true, error = null) }
            }

            val filter = TemplateSearchFilter(
                query = currentState.searchQuery,
                categories = currentState.selectedCategories,
                sortBy = currentState.sortField,
                sortOrder = currentState.sortOrder
            )

            searchTemplatesUseCase(filter)
                .onSuccess { templates ->
                    updateState {
                        copy(
                            templates = templates,
                            isLoading = false,
                            isRefreshing = false
                        )
                    }
                }
                .onFailure { throwable ->
                    updateState {
                        copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = throwable.message ?: "加载模板失败"
                        )
                    }
                }
        }
    }

    /**
     * 更新搜索关键词（带防抖）
     * 输入停止 300ms 后自动触发搜索
     *
     * @param query 新的搜索关键词
     */
    fun updateSearchQuery(query: String) {
        updateState { copy(searchQuery = query) }

        // 取消之前的防抖任务
        searchDebounceJob?.cancel()

        // 空查询直接搜索
        if (query.isBlank()) {
            loadTemplates()
            return
        }

        // 防抖：等待用户停止输入后再搜索
        searchDebounceJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            loadTemplates()
        }
    }

    /**
     * ViewModel 销毁时清理资源
     */
    override fun onCleared() {
        super.onCleared()
        searchDebounceJob?.cancel()
        searchDebounceJob = null
    }

    /**
     * 切换分类筛选
     * 如果分类已选中则取消选中，否则添加选中
     *
     * @param category 要切换的分类
     */
    fun toggleCategory(category: TemplateCategory) {
        val currentCategories = currentState.selectedCategories.toMutableList()
        if (category in currentCategories) {
            currentCategories.remove(category)
        } else {
            currentCategories.add(category)
        }
        updateState { copy(selectedCategories = currentCategories) }
        loadTemplates()
    }

    /**
     * 更新排序方式
     *
     * @param sortField 排序字段
     * @param sortOrder 排序顺序
     */
    fun updateSort(sortField: SortField, sortOrder: SortOrder = SortOrder.DESCENDING) {
        updateState { copy(sortField = sortField, sortOrder = sortOrder) }
        loadTemplates()
    }

    // ==================== 模板详情 ====================

    /**
     * 选中模板并显示详情底部弹窗
     *
     * @param template 要查看详情的模板
     */
    fun selectTemplate(template: CloneTemplate) {
        launchWithErrorHandler {
            updateState {
                copy(
                    selectedTemplate = template,
                    showDetailSheet = true
                )
            }
            // 加载模板评分
            loadTemplateRatings(template.id)
        }
    }

    /**
     * 关闭模板详情底部弹窗
     */
    fun dismissDetailSheet() {
        updateState {
            copy(
                showDetailSheet = false,
                selectedTemplate = null,
                templateRatings = emptyList()
            )
        }
    }

    /**
     * 加载模板评分列表
     *
     * @param templateId 模板ID
     */
    private suspend fun loadTemplateRatings(templateId: Long) {
        // 评分数据通过搜索结果中的模板信息获取
        // 此处预留接口调用
    }

    // ==================== 分享操作 ====================

    /**
     * 显示分享对话框
     *
     * @param template 要分享的模板
     */
    fun showShareDialog(template: CloneTemplate) {
        updateState {
            copy(
                showShareDialog = true,
                selectedTemplate = template,
                shareInfo = null,
                shareProgress = ShareProgressUiState.Idle
            )
        }
    }

    /**
     * 关闭分享对话框
     */
    fun dismissShareDialog() {
        updateState {
            copy(
                showShareDialog = false,
                shareInfo = null,
                shareProgress = ShareProgressUiState.Idle
            )
        }
    }

    /**
     * 执行模板分享操作
     * 通过 Flow 接收分享进度更新
     */
    fun shareTemplate() {
        val template = currentState.selectedTemplate ?: return
        updateState { copy(shareProgress = ShareProgressUiState.Preparing) }

        launchWithErrorHandler {
            shareTemplateUseCase(template.id).collect { progressState ->
                when (progressState) {
                    is com.appclonex.domain.usecase.template.ShareProgressState.Preparing -> {
                        updateState { copy(shareProgress = ShareProgressUiState.Preparing) }
                    }
                    is com.appclonex.domain.usecase.template.ShareProgressState.GeneratingCode -> {
                        updateState { copy(shareProgress = ShareProgressUiState.GeneratingCode) }
                    }
                    is com.appclonex.domain.usecase.template.ShareProgressState.Uploading -> {
                        updateState { copy(shareProgress = ShareProgressUiState.Uploading) }
                    }
                    is com.appclonex.domain.usecase.template.ShareProgressState.Completed -> {
                        updateState {
                            copy(
                                shareProgress = ShareProgressUiState.Completed(progressState.shareInfo),
                                shareInfo = progressState.shareInfo
                            )
                        }
                    }
                    is com.appclonex.domain.usecase.template.ShareProgressState.Error -> {
                        updateState {
                            copy(
                                shareProgress = ShareProgressUiState.Error(progressState.message)
                            )
                        }
                    }
                }
            }
        }
    }

    // ==================== 导入操作 ====================

    /**
     * 显示导入对话框
     */
    fun showImportDialog() {
        updateState {
            copy(
                showImportDialog = true,
                importShareCode = "",
                importProgress = ImportProgressUiState.Idle
            )
        }
    }

    /**
     * 关闭导入对话框
     */
    fun dismissImportDialog() {
        updateState {
            copy(
                showImportDialog = false,
                importShareCode = "",
                importProgress = ImportProgressUiState.Idle
            )
        }
    }

    /**
     * 更新导入分享码输入
     *
     * @param code 用户输入的分享码
     */
    fun updateImportShareCode(code: String) {
        updateState { copy(importShareCode = code) }
    }

    /**
     * 执行模板导入操作
     * 通过 Flow 接收导入进度更新
     */
    fun importTemplate() {
        val shareCode = currentState.importShareCode.trim()
        if (shareCode.isBlank()) {
            updateState { copy(error = "请输入分享码") }
            return
        }

        updateState { copy(importProgress = ImportProgressUiState.Validating) }

        launchWithErrorHandler {
            importTemplateUseCase(shareCode).collect { progressState ->
                when (progressState) {
                    is com.appclonex.domain.usecase.template.ImportProgressState.Validating -> {
                        updateState { copy(importProgress = ImportProgressUiState.Validating) }
                    }
                    is com.appclonex.domain.usecase.template.ImportProgressState.Downloading -> {
                        updateState { copy(importProgress = ImportProgressUiState.Downloading) }
                    }
                    is com.appclonex.domain.usecase.template.ImportProgressState.Parsing -> {
                        updateState { copy(importProgress = ImportProgressUiState.Parsing) }
                    }
                    is com.appclonex.domain.usecase.template.ImportProgressState.Merging -> {
                        updateState { copy(importProgress = ImportProgressUiState.Merging) }
                    }
                    is com.appclonex.domain.usecase.template.ImportProgressState.Completed -> {
                        updateState {
                            copy(
                                importProgress = ImportProgressUiState.Completed(progressState.template),
                                successMessage = "模板导入成功: ${progressState.template.name}"
                            )
                        }
                        // 导入成功后刷新列表
                        loadTemplates()
                    }
                    is com.appclonex.domain.usecase.template.ImportProgressState.Error -> {
                        updateState {
                            copy(
                                importProgress = ImportProgressUiState.Error(progressState.message)
                            )
                        }
                    }
                }
            }
        }
    }

    // ==================== 评分操作 ====================

    /**
     * 显示评分对话框
     *
     * @param template 要评分的模板
     */
    fun showRatingDialog(template: CloneTemplate) {
        updateState {
            copy(
                showRatingDialog = true,
                selectedTemplate = template
            )
        }
    }

    /**
     * 关闭评分对话框
     */
    fun dismissRatingDialog() {
        updateState { copy(showRatingDialog = false) }
    }

    /**
     * 提交模板评分
     *
     * @param rating 评分值（1.0 - 5.0）
     * @param comment 评论内容
     */
    fun submitRating(rating: Float, comment: String) {
        val template = currentState.selectedTemplate ?: return

        launchWithErrorHandler {
            rateTemplateUseCase(template.id, rating, comment)
                .onSuccess {
                    updateState {
                        copy(
                            showRatingDialog = false,
                            successMessage = "评分提交成功"
                        )
                    }
                }
                .onFailure { throwable ->
                    updateState {
                        copy(error = throwable.message ?: "评分提交失败")
                    }
                }
        }
    }

    // ==================== 消息管理 ====================

    /**
     * 清除成功消息
     */
    fun clearSuccessMessage() {
        updateState { copy(successMessage = null) }
    }

    /**
     * 清除分享进度状态
     */
    fun clearShareProgress() {
        updateState { copy(shareProgress = ShareProgressUiState.Idle) }
    }

    /**
     * 清除导入进度状态
     */
    fun clearImportProgress() {
        updateState { copy(importProgress = ImportProgressUiState.Idle) }
    }
}
