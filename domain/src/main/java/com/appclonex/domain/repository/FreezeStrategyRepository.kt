package com.appclonex.domain.repository

import com.appclonex.domain.model.FreezeExecutionResult
import com.appclonex.domain.model.FreezeStrategy
import kotlinx.coroutines.flow.Flow

/**
 * 智能冻结策略仓库接口
 * 提供冻结策略的 CRUD 操作、执行、条件评估和批量冻结/解冻能力
 */
interface FreezeStrategyRepository {

    // ========== CRUD ==========

    /**
     * 获取所有冻结策略的响应式数据流
     *
     * @return 冷流，发射当前所有策略列表，策略变更时自动更新
     */
    fun getAllStrategies(): Flow<List<FreezeStrategy>>

    /**
     * 根据 ID 获取单个冻结策略
     *
     * @param id 策略唯一标识
     * @return 对应的策略对象，不存在时返回 null
     */
    suspend fun getStrategyById(id: Long): FreezeStrategy?

    /**
     * 插入新的冻结策略
     *
     * @param strategy 待插入的策略对象
     * @return 新插入记录的行 ID
     */
    suspend fun insertStrategy(strategy: FreezeStrategy): Long

    /**
     * 更新已有的冻结策略
     *
     * @param strategy 包含更新内容的策略对象，需确保 [FreezeStrategy.id] 有效
     */
    suspend fun updateStrategy(strategy: FreezeStrategy)

    /**
     * 根据 ID 删除冻结策略
     *
     * @param id 待删除策略的唯一标识
     */
    suspend fun deleteStrategy(id: Long)

    /**
     * 删除所有冻结策略
     */
    suspend fun deleteAllStrategies()

    // ========== 执行 ==========

    /**
     * 执行指定 ID 的冻结策略
     * 根据策略配置的触发类型和条件，对目标应用执行冻结或解冻操作
     *
     * @param strategyId 策略唯一标识
     * @return 执行结果，包含冻结/解冻数量和失败列表；策略不存在或条件不满足时返回失败
     */
    suspend fun executeStrategy(strategyId: Long): Result<FreezeExecutionResult>

    /**
     * 执行所有已启用的冻结策略
     * 依次评估并执行所有 [FreezeStrategy.isEnabled] 为 true 的策略
     *
     * @return 每个策略的执行结果列表
     */
    suspend fun executeAllEnabledStrategies(): List<FreezeExecutionResult>

    // ========== 评估 ==========

    /**
     * 评估策略的所有条件是否满足
     * 对策略中的每个 [com.appclonex.domain.model.FreezeCondition] 进行判断，
     * 所有条件均满足时返回 true
     *
     * @param strategy 待评估的冻结策略
     * @return true 表示所有条件满足，应执行冻结；false 表示不满足
     */
    suspend fun evaluateConditions(strategy: FreezeStrategy): Boolean

    /**
     * 判断指定应用是否应被冻结
     * 综合所有已启用策略的配置和当前条件进行评估
     *
     * @param packageName 应用包名
     * @return true 表示该应用应被冻结
     */
    suspend fun shouldFreeze(packageName: String): Boolean

    // ========== 批量操作 ==========

    /**
     * 根据策略配置批量冻结目标应用
     *
     * @param strategy 冻结策略，使用其 [FreezeStrategy.targetApps] 和 [FreezeStrategy.targetProfiles] 确定范围
     * @return 冻结执行结果
     */
    suspend fun freezeAppsByStrategy(strategy: FreezeStrategy): Result<FreezeExecutionResult>

    /**
     * 根据策略配置批量解冻目标应用
     *
     * @param strategy 冻结策略，使用其 [FreezeStrategy.targetApps] 和 [FreezeStrategy.targetProfiles] 确定范围
     * @return 解冻执行结果
     */
    suspend fun unfreezeAppsByStrategy(strategy: FreezeStrategy): Result<FreezeExecutionResult>

    // ========== 级联清理 ==========

    /**
     * 删除所有引用指定配置文件ID的冻结策略
     * @param profileId 要清理的配置文件ID
     * @return 被删除的策略数量
     */
    suspend fun deleteStrategiesReferencingProfile(profileId: Int): Int

    /**
     * 从所有策略中移除指定的应用包名
     * @param packageName 要移除的应用包名
     * @return 被更新的策略数量
     */
    suspend fun removePackageFromAllStrategies(packageName: String): Int
}
