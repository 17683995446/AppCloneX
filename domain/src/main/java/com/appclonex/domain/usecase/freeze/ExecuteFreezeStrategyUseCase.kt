package com.appclonex.domain.usecase.freeze

import com.appclonex.domain.model.FreezeExecutionResult
import com.appclonex.domain.repository.FreezeStrategyRepository
import javax.inject.Inject

/**
 * 执行冻结策略用例
 * 封装单条策略执行和全部已启用策略批量执行的业务逻辑
 *
 * @property freezeStrategyRepository 冻结策略仓库接口
 */
class ExecuteFreezeStrategyUseCase @Inject constructor(
    private val freezeStrategyRepository: FreezeStrategyRepository
) {
    /**
     * 执行指定 ID 的冻结策略
     * 根据策略配置评估条件并执行冻结/解冻操作
     *
     * @param strategyId 目标策略的唯一标识
     * @return 执行结果，包含冻结/解冻数量和失败列表
     */
    suspend operator fun invoke(strategyId: Long): Result<FreezeExecutionResult> =
        freezeStrategyRepository.executeStrategy(strategyId)

    /**
     * 执行所有已启用的冻结策略
     * 依次评估并执行所有处于启用状态的策略
     *
     * @return 每个策略的执行结果列表
     */
    suspend fun executeAll(): List<FreezeExecutionResult> =
        freezeStrategyRepository.executeAllEnabledStrategies()
}
