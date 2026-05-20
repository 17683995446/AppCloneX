package com.appclonex.domain.usecase.freeze

import com.appclonex.domain.model.FreezeStrategy
import com.appclonex.domain.repository.FreezeStrategyRepository
import javax.inject.Inject

/**
 * 创建冻结策略用例
 * 封装向仓库插入新冻结策略的业务逻辑
 *
 * @property freezeStrategyRepository 冻结策略仓库接口
 */
class CreateFreezeStrategyUseCase @Inject constructor(
    private val freezeStrategyRepository: FreezeStrategyRepository
) {
    /**
     * 创建并持久化一条新的冻结策略
     *
     * @param strategy 待创建的冻结策略对象
     * @return 成功时包含新记录的行 ID，失败时包含异常信息
     */
    suspend operator fun invoke(strategy: FreezeStrategy): Result<Long> = try {
        val id = freezeStrategyRepository.insertStrategy(strategy)
        Result.success(id)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
