package com.appclonex.domain.usecase.clone

import com.appclonex.domain.model.BatchCloneConfig
import com.appclonex.domain.model.BatchCloneProgress
import kotlinx.coroutines.flow.Flow

/**
 * 批量克隆用例
 * 支持进度流式返回
 */
interface BatchCloneUseCase {
    operator fun invoke(config: BatchCloneConfig): Flow<BatchCloneProgress>
}
