package com.appclonex.domain.usecase.freeze

import com.appclonex.domain.model.FreezeStrategy
import com.appclonex.domain.repository.FreezeStrategyRepository
import javax.inject.Inject

/**
 * 评估冻结条件用例
 * 封装策略条件评估的业务逻辑，判断是否满足触发冻结的要求
 *
 * @property freezeStrategyRepository 冻结策略仓库接口
 */
class EvaluateFreezeConditionsUseCase @Inject constructor(
    private val freezeStrategyRepository: FreezeStrategyRepository
) {
    /**
     * 评估指定策略的所有条件是否满足
     * 对策略中的每个条件进行判断，全部满足时返回 true
     *
     * @param strategy 待评估的冻结策略
     * @return true 表示所有条件满足，应执行冻结；false 表示不满足
     */
    suspend operator fun invoke(strategy: FreezeStrategy): Boolean =
        freezeStrategyRepository.evaluateConditions(strategy)
}
