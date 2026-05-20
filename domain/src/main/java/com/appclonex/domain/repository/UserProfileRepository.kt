package com.appclonex.domain.repository

import com.appclonex.domain.model.UserProfileConfigurationModel
import com.appclonex.domain.model.UserProfileInfo
import kotlinx.coroutines.flow.Flow

/**
 * 用户配置文件仓库接口。
 *
 * 定义用户配置文件相关的数据操作契约，遵循依赖倒置原则（DIP）：
 * - Domain层定义接口契约
 * - Infrastructure层提供具体实现（如 UserProfileRepositoryImpl）
 *
 * ## 职责范围
 * - 用户配置文件的CRUD操作
 * - 用户空间的启动/停止/切换
 * - 与Android系统用户管理的同步
 * - 配置文件统计信息的更新
 *
 * ## 使用示例
 * ```kotlin
 * class ProfilesViewModel(
 *     private val repository: UserProfileRepository
 * ) : ViewModel() {
 *
 *     // 获取所有配置文件
 *     val profiles = repository.getAllProfilesFlow()
 *         .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
 *
 *     // 创建新配置文件
 *     fun createProfile(name: String) {
 *         viewModelScope.launch {
 *             repository.createProfile(name)
 *                 .onSuccess { userId -> showSuccess("创建成功，ID: $userId") }
 *                 .onFailure { showError(it.message) }
 *         }
 *     }
 *
 *     // 启动配置文件
 *     fun startProfile(userId: Int) {
 *         viewModelScope.launch {
 *             repository.startProfile(userId)
 *         }
 *     }
 * }
 * ```
 *
 * @see UserProfileConfigurationModel
 * @see UserProfileInfo
 */
interface UserProfileRepository {

    /**
     * 获取所有用户配置文件的Flow。
     *
     * 返回一个Flow，当数据库中的配置文件发生变化时会自动发出更新。
     * 适合用于UI层的响应式数据展示。
     *
     * @return 用户配置文件列表的Flow，当数据变化时自动更新
     *
     * @see UserProfileConfigurationModel
     */
    fun getAllProfilesFlow(): Flow<List<UserProfileConfigurationModel>>

    /**
     * 获取所有用户配置文件的详细信息。
     *
     * 包含配置文件的基础信息和运行时状态。
     * 这是一个一次性查询，不返回Flow。
     *
     * @return 配置文件信息列表，包含状态和错误信息
     *
     * @see UserProfileInfo
     */
    suspend fun getAllProfilesWithInfo(): List<UserProfileInfo>

    /**
     * 根据用户ID获取配置文件。
     *
     * @param userId Android系统用户ID
     * @return 配置文件信息，如果不存在则返回null
     *
     * @see UserProfileConfigurationModel
     */
    suspend fun getProfileById(userId: Int): UserProfileConfigurationModel?

    /**
     * 创建新的用户配置文件。
     *
     * 在Android系统中创建一个新的用户空间，用于运行克隆应用。
     * 这是一个耗时操作，建议在后台线程执行。
     *
     * @param name 配置文件的显示名称
     * @return Result包装的新用户ID，失败时返回异常
     *
     * @throws SecurityException 当缺少Shizuku权限时
     * @throws IllegalStateException 当达到用户数量上限时
     */
    suspend fun createProfile(name: String): Result<Int>

    /**
     * 删除指定的用户配置文件。
     *
     * 删除用户空间及其中的所有克隆应用。
     * 这是一个不可逆操作，删除后数据无法恢复。
     *
     * @param userId 要删除的用户ID
     * @return Result包装的删除结果，true表示成功
     *
     * @throws SecurityException 当缺少Shizuku权限时
     * @throws IllegalArgumentException 当尝试删除主用户时
     */
    suspend fun deleteProfile(userId: Int): Result<Boolean>

    /**
     * 启动指定的用户配置文件。
     *
     * 启动用户空间，使其可以运行应用。
     * 在启动应用之前需要先启动对应的用户空间。
     *
     * @param userId 要启动的用户ID
     * @return Result包装的启动结果，true表示成功
     *
     * @throws SecurityException 当缺少Shizuku权限时
     */
    suspend fun startProfile(userId: Int): Result<Boolean>

    /**
     * 停止指定的用户配置文件。
     *
     * 停止用户空间，终止其中运行的所有应用。
     *
     * @param userId 要停止的用户ID
     * @return Result包装的停止结果，true表示成功
     *
     * @throws SecurityException 当缺少Shizuku权限时
     */
    suspend fun stopProfile(userId: Int): Result<Boolean>

    /**
     * 切换到指定的用户配置文件。
     *
     * 将当前用户切换到指定的用户空间。
     * 这会导致当前用户会话结束。
     *
     * @param userId 目标用户ID
     * @return Result包装的切换结果，true表示成功
     *
     * @throws SecurityException 当缺少Shizuku权限时
     */
    suspend fun switchToProfile(userId: Int): Result<Boolean>

    /**
     * 更新配置文件的显示名称。
     *
     * 只更新本地数据库中的名称，不影响系统用户信息。
     *
     * @param userId 用户ID
     * @param newName 新的显示名称
     */
    suspend fun updateProfileName(userId: Int, newName: String)

    /**
     * 更新配置文件中的克隆应用数量统计。
     *
     * 重新计算并更新指定配置文件中的应用数量。
     * 通常在克隆或删除应用后调用。
     *
     * @param userId 用户ID
     */
    suspend fun updateClonedAppCount(userId: Int)

    /**
     * 获取当前配置文件数量。
     *
     * @return 配置文件总数
     */
    suspend fun getProfileCount(): Int

    /**
     * 获取系统支持的最大用户数量。
     *
     * 不同Android版本和设备可能有不同的限制。
     *
     * @return Result包装的最大用户数量
     */
    suspend fun getMaxUserCount(): Result<Int>

    /**
     * 同步系统用户状态到本地数据库。
     *
     * 将Android系统中的用户信息同步到本地数据库，
     * 确保数据一致性。建议在应用启动时调用。
     *
     * @return Result包装的同步结果，true表示成功
     */
    suspend fun syncWithSystem(): Result<Boolean>
}
