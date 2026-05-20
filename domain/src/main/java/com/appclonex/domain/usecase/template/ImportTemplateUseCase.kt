/*
 * Copyright (c) 2024 AppCloneX
 *
 * 导入模板用例
 * 封装通过分享码导入模板的完整业务流程，包括验证、下载、解析、合并等阶段
 */

package com.appclonex.domain.usecase.template

import com.appclonex.domain.model.CloneTemplate
import com.appclonex.domain.repository.TemplateMarketplaceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 导入进度状态
 * 表示模板导入过程中各阶段的进度
 */
sealed interface ImportProgressState {

    /** 验证阶段 - 正在验证分享码和模板数据 */
    data object Validating : ImportProgressState

    /** 下载阶段 - 正在下载模板数据 */
    data object Downloading : ImportProgressState

    /** 解析阶段 - 正在解析模板配置 */
    data object Parsing : ImportProgressState

    /** 合并阶段 - 正在合并到本地模板库 */
    data object Merging : ImportProgressState

    /** 导入完成 - 包含导入的模板 */
    data class Completed(val template: CloneTemplate) : ImportProgressState

    /** 导入失败 - 包含错误信息 */
    data class Error(val message: String) : ImportProgressState
}

/**
 * 导入模板用例
 * 封装通过分享码导入模板的完整业务流程，提供分阶段的进度反馈
 *
 * 流程: 验证分享码 -> 下载模板数据 -> 解析配置 -> 合并到本地 -> 完成
 * 每个阶段通过 [Flow] 向调用方发送进度更新
 *
 * @property repository 模板市场仓库接口
 */
class ImportTemplateUseCase @Inject constructor(
    private val repository: TemplateMarketplaceRepository
) {

    companion object {
        /** 各阶段模拟延迟时间（毫秒），用于提供更好的用户体验 */
        private const val VALIDATE_DELAY_MS = 400L
        private const val DOWNLOAD_DELAY_MS = 600L
        private const val PARSE_DELAY_MS = 300L
        private const val MERGE_DELAY_MS = 500L
    }

    /**
     * 执行模板导入操作
     * 返回包含各阶段进度状态的 Flow，调用方可以实时展示导入进度
     *
     * @param shareCode 模板分享码
     * @return 导入进度状态的 Flow 流
     */
    operator fun invoke(shareCode: String): Flow<ImportProgressState> = flow {
        try {
            // 阶段1: 验证分享码
            emit(ImportProgressState.Validating)
            delay(VALIDATE_DELAY_MS)

            // 执行实际导入操作（包含验证、下载、解析、合并）
            val importResult = repository.importTemplate(shareCode)
            if (importResult.isFailure) {
                emit(ImportProgressState.Error(
                    importResult.exceptionOrNull()?.message ?: "导入模板失败"
                ))
                return@flow
            }

            val result = importResult.getOrThrow()
            if (!result.success || result.template == null) {
                emit(ImportProgressState.Error(
                    result.errorMessage ?: "导入失败：未知错误"
                ))
                return@flow
            }

            // 阶段2: 下载模板数据
            emit(ImportProgressState.Downloading)
            delay(DOWNLOAD_DELAY_MS)

            // 阶段3: 解析模板配置
            emit(ImportProgressState.Parsing)
            delay(PARSE_DELAY_MS)

            // 阶段4: 合并到本地模板库
            emit(ImportProgressState.Merging)
            delay(MERGE_DELAY_MS)

            // 阶段5: 完成
            emit(ImportProgressState.Completed(result.template))
        } catch (e: Exception) {
            emit(ImportProgressState.Error(e.message ?: "导入过程中发生未知错误"))
        }
    }
}
