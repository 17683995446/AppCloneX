/*
 * Copyright (c) 2024 AppCloneX
 *
 * 分享模板用例
 * 封装模板分享的完整业务流程，包括准备、生成分享码、上传等阶段
 */

package com.appclonex.domain.usecase.template

import com.appclonex.domain.model.TemplateShareInfo
import com.appclonex.domain.repository.TemplateMarketplaceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 分享进度状态
 * 表示模板分享过程中各阶段的进度
 */
sealed interface ShareProgressState {

    /** 准备阶段 - 正在准备模板数据 */
    data object Preparing : ShareProgressState

    /** 生成分享码阶段 - 正在生成唯一分享码 */
    data object GeneratingCode : ShareProgressState

    /** 上传阶段 - 正在上传分享信息 */
    data object Uploading : ShareProgressState

    /** 分享完成 - 包含分享信息 */
    data class Completed(val shareInfo: TemplateShareInfo) : ShareProgressState

    /** 分享失败 - 包含错误信息 */
    data class Error(val message: String) : ShareProgressState
}

/**
 * 分享模板用例
 * 封装模板分享的完整业务流程，提供分阶段的进度反馈
 *
 * 流程: 准备模板数据 -> 生成分享码 -> 上传分享信息 -> 完成
 * 每个阶段通过 [Flow] 向调用方发送进度更新
 *
 * @property repository 模板市场仓库接口
 */
class ShareTemplateUseCase @Inject constructor(
    private val repository: TemplateMarketplaceRepository
) {

    companion object {
        /** 各阶段模拟延迟时间（毫秒），用于提供更好的用户体验 */
        private const val PREPARE_DELAY_MS = 300L
        private const val GENERATE_DELAY_MS = 500L
        private const val UPLOAD_DELAY_MS = 400L
    }

    /**
     * 执行模板分享操作
     * 返回包含各阶段进度状态的 Flow，调用方可以实时展示分享进度
     *
     * @param templateId 要分享的模板ID
     * @return 分享进度状态的 Flow 流
     */
    operator fun invoke(templateId: Long): Flow<ShareProgressState> = flow {
        try {
            // 阶段1: 准备模板数据
            emit(ShareProgressState.Preparing)
            delay(PREPARE_DELAY_MS)

            // 阶段2: 生成分享码
            emit(ShareProgressState.GeneratingCode)
            val shareCodeResult = repository.generateShareCode(templateId)
            if (shareCodeResult.isFailure) {
                emit(ShareProgressState.Error(
                    shareCodeResult.exceptionOrNull()?.message ?: "生成分享码失败"
                ))
                return@flow
            }
            delay(GENERATE_DELAY_MS)

            // 阶段3: 上传分享信息
            emit(ShareProgressState.Uploading)
            val shareResult = repository.shareTemplate(templateId)
            if (shareResult.isFailure) {
                emit(ShareProgressState.Error(
                    shareResult.exceptionOrNull()?.message ?: "分享模板失败"
                ))
                return@flow
            }
            delay(UPLOAD_DELAY_MS)

            // 阶段4: 完成
            emit(ShareProgressState.Completed(shareResult.getOrThrow()))
        } catch (e: Exception) {
            emit(ShareProgressState.Error(e.message ?: "分享过程中发生未知错误"))
        }
    }
}
