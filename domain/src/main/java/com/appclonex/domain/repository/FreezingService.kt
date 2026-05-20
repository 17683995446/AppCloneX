package com.appclonex.domain.repository

/**
 * 应用冻结服务接口
 *
 * 定义应用冻结/解冻操作的领域层契约。
 * Infrastructure层的 ApplicationFreezingStateManager 负责具体实现。
 */
interface FreezingService {

    /**
     * 冻结指定用户的应用
     *
     * @param packageName 应用包名
     * @param userId 目标用户ID
     * @return 操作结果
     */
    suspend fun freezeApp(packageName: String, userId: Int): Result<Unit>

    /**
     * 解冻指定用户的应用
     *
     * @param packageName 应用包名
     * @param userId 目标用户ID
     * @return 操作结果
     */
    suspend fun unfreezeApp(packageName: String, userId: Int): Result<Unit>

    /**
     * 检查应用是否被冻结
     *
     * @param packageName 应用包名
     * @param userId 目标用户ID
     * @return true 表示已冻结
     */
    suspend fun isAppFrozen(packageName: String, userId: Int): Boolean

    /**
     * 获取指定用户下所有被冻结的应用列表
     *
     * @param userId 目标用户ID
     * @return 被冻结的应用包名列表
     */
    suspend fun getFrozenApps(userId: Int): List<String>

    /**
     * 批量冻结指定用户的所有克隆应用
     *
     * @param userId 目标用户ID
     * @return 成功冻结的应用数量
     */
    suspend fun freezeAllApps(userId: Int): Result<Int>

    /**
     * 批量解冻指定用户的所有克隆应用
     *
     * @param userId 目标用户ID
     * @return 成功解冻的应用数量
     */
    suspend fun unfreezeAllApps(userId: Int): Result<Int>
}
