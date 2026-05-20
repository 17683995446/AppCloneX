package com.appclonex.domain.repository

import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.domain.model.ClonedAppInfo
import kotlinx.coroutines.flow.Flow

/**
 * 克隆应用仓库接口。
 *
 * 定义克隆应用相关的数据操作契约，遵循依赖倒置原则（DIP）：
 * - Domain层定义接口契约
 * - Infrastructure层提供具体实现（如 ClonedApplicationRepositoryImpl）
 *
 * ## 职责范围
 * - 克隆应用的CRUD操作
 * - 应用克隆/卸载的业务逻辑
 * - 可安装应用列表的获取
 * - 应用启动和状态管理
 *
 * ## 使用示例
 * ```kotlin
 * class CloneAppsViewModel(
 *     private val repository: ClonedApplicationRepository
 * ) : ViewModel() {
 *
 *     // 获取克隆应用列表
 *     val clonedApps = repository.getClonedAppsFlow(profileUserId = 10)
 *         .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
 *
 *     // 克隆应用
 *     fun cloneApp(packageName: String) {
 *         viewModelScope.launch {
 *             repository.cloneApp(packageName, profileUserId = 10)
 *                 .onSuccess { showSuccess() }
 *                 .onFailure { showError(it.message) }
 *         }
 *     }
 * }
 * ```
 *
 * @see ClonedApplicationInformationModel
 * @see ClonedAppInfo
 */
interface ClonedApplicationRepository {

    /**
     * 获取指定配置文件下的所有克隆应用。
     *
     * 返回一个Flow，当数据库中的克隆应用发生变化时会自动发出更新。
     * 适合用于UI层的响应式数据展示。
     *
     * @param profileUserId 配置文件的用户ID
     * @return 克隆应用列表的Flow，当数据变化时自动更新
     *
     * @see ClonedApplicationInformationModel
     */
    fun getClonedAppsFlow(profileUserId: Int): Flow<List<ClonedApplicationInformationModel>>

    /**
     * 获取所有配置文件中的克隆应用。
     *
     * 返回一个Flow，包含所有用户配置文件下的克隆应用。
     *
     * @return 所有克隆应用列表的Flow
     *
     * @see ClonedApplicationInformationModel
     */
    fun getAllClonedAppsFlow(): Flow<List<ClonedApplicationInformationModel>>

    /**
     * 获取指定克隆应用的详细信息。
     *
     * 包含应用的基础信息和运行时状态。
     *
     * @param id 克隆应用的数据库ID
     * @return 应用信息，如果不存在则返回null
     *
     * @see ClonedAppInfo
     */
    suspend fun getClonedAppInfo(id: Long): ClonedAppInfo?

    /**
     * 获取设备上可安装的应用列表。
     *
     * 返回设备上已安装但尚未克隆的应用列表，用于用户选择要克隆的应用。
     *
     * @param excludeSystem 是否排除系统预装应用，默认建议为true
     * @return Result包装的可安装应用列表，失败时返回异常
     *
     * @throws SecurityException 当缺少必要的权限时
     * @see InstallableApp
     */
    suspend fun getInstallableApps(excludeSystem: Boolean): Result<List<InstallableApp>>

    /**
     * 克隆应用到指定用户配置文件。
     *
     * 将指定应用安装到目标用户空间，创建应用分身。
     * 这是一个异步操作，可能需要较长时间完成。
     *
     * @param packageName 要克隆的应用包名
     * @param profileUserId 目标用户配置文件的ID
     * @return Result包装的克隆应用信息，失败时返回异常
     *
     * @throws SecurityException 当缺少Shizuku权限时
     * @throws IllegalStateException 当目标用户空间不可用时
     * @see ClonedApplicationInformationModel
     */
    suspend fun cloneApp(packageName: String, profileUserId: Int): Result<ClonedApplicationInformationModel>

    /**
     * 批量克隆应用到指定用户配置文件。
     *
     * 支持批量克隆多个应用，并提供进度回调。
     *
     * @param packageNames 要克隆的应用包名列表
     * @param profileUserId 目标用户配置文件的ID
     * @param onProgress 进度回调函数，参数为(当前进度, 总数量)
     * @return Result包装的克隆应用列表，包含成功克隆的应用
     *
     * @see ClonedApplicationInformationModel
     */
    suspend fun cloneApps(
        packageNames: List<String>,
        profileUserId: Int,
        onProgress: (Int, Int) -> Unit
    ): Result<List<ClonedApplicationInformationModel>>

    /**
     * 删除指定的克隆应用。
     *
     * 从用户空间中卸载克隆应用，并从数据库中删除记录。
     *
     * @param clonedApp 要删除的克隆应用
     * @return Result包装的删除结果，true表示成功
     *
     * @throws SecurityException 当缺少Shizuku权限时
     */
    suspend fun deleteClonedApp(clonedApp: ClonedApplicationInformationModel): Result<Boolean>

    /**
     * 启动指定的克隆应用。
     *
     * 在对应的用户空间中启动应用。
     *
     * @param clonedApp 要启动的克隆应用
     * @return Result包装的启动结果，true表示成功
     *
     * @throws SecurityException 当缺少Shizuku权限时
     * @throws IllegalStateException 当应用未安装或用户空间未启动时
     */
    suspend fun launchClonedApp(clonedApp: ClonedApplicationInformationModel): Result<Boolean>

    /**
     * 更新克隆应用的自定义名称。
     *
     * 修改应用的显示名称，不影响原始应用信息。
     *
     * @param id 克隆应用的数据库ID
     * @param customName 新的自定义名称
     */
    suspend fun updateCustomName(id: Long, customName: String)

    /**
     * 检查应用是否在指定用户空间中已安装。
     *
     * @param packageName 应用包名
     * @param userId 用户ID
     * @return Result包装的布尔值，true表示已安装
     */
    suspend fun isAppInstalledInUser(packageName: String, userId: Int): Result<Boolean>

    /**
     * 获取应用在所有配置文件中的克隆情况。
     *
     * 返回指定应用在所有用户配置文件中的克隆实例列表。
     *
     * @param packageName 应用包名
     * @return 该应用的所有克隆实例列表
     *
     * @see ClonedApplicationInformationModel
     */
    suspend fun getAppCloneStatus(packageName: String): List<ClonedApplicationInformationModel>

    /**
     * 获取指定配置文件下的所有克隆应用列表。
     *
     * 这是一次性查询，返回当前快照，不返回Flow。
     * 用于配置导入导出功能。
     *
     * @param profileUserId 配置文件的用户ID
     * @return 克隆应用列表
     *
     * @see ClonedApplicationInformationModel
     */
    suspend fun getClonedAppsByProfile(profileUserId: Int): List<ClonedApplicationInformationModel>
}
