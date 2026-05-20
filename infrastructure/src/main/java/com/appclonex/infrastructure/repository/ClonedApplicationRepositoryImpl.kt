package com.appclonex.infrastructure.repository

import android.content.Context
import com.appclonex.domain.model.CloneOperationResult
import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.domain.model.ClonedAppInfo
import com.appclonex.domain.model.ClonedAppState
import com.appclonex.domain.model.InstallableApp
import com.appclonex.domain.model.OperationStatus
import com.appclonex.domain.model.PendingCloneOperation
import com.appclonex.domain.repository.ClonedApplicationRepository
import com.appclonex.infrastructure.caching.ApplicationIconMemoryCacheManager
import com.appclonex.infrastructure.external.packagemanager.ApplicationPackageInstallerService
import com.appclonex.infrastructure.external.shizuku.ShizukuException
import com.appclonex.infrastructure.external.shizuku.ShizukuServiceConnectionProvider
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import com.appclonex.infrastructure.persistence.database.ApplicationDatabaseConfiguration
import com.appclonex.infrastructure.persistence.database.ClonedApplicationDataAccessObject
import com.appclonex.infrastructure.persistence.database.PendingCloneOperationDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 克隆应用仓库实现
 * 负责克隆应用的数据操作和业务逻辑
 *
 * ## R11-001 修复：分布式事务幂等性保证
 * 使用事务日志模式（Transaction Log Pattern）确保系统安装和数据库记录的原子性：
 * 1. 创建PENDING操作记录
 * 2. 执行系统安装
 * 3. 创建数据库记录
 * 4. 标记操作完成或清理
 *
 * 失败时支持幂等重试和自动补偿
 */
@Singleton
class ClonedApplicationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ApplicationDatabaseConfiguration,
    private val packageInstaller: ApplicationPackageInstallerService,
    private val automationRuleRepository: com.appclonex.domain.repository.AutomationRuleRepository,
    private val appLockRepository: com.appclonex.domain.repository.AppLockRepository,
    private val freezeStrategyRepository: com.appclonex.domain.repository.FreezeStrategyRepository
) : ClonedApplicationRepository {

    private val clonedAppDao: ClonedApplicationDataAccessObject = database.clonedAppDao()
    private val pendingOperationDao: PendingCloneOperationDao = database.pendingCloneOperationDao()

    // R11-001 修复：克隆操作互斥锁，防止同一应用并发克隆导致重复
    private val cloneMutex = Mutex()

    companion object {
        private const val TAG = "ClonedAppRepository"
        private const val CLONE_OPERATION_TIMEOUT_MS = 120000L // 2分钟超时
    }

    override fun getClonedAppsFlow(profileUserId: Int): Flow<List<ClonedApplicationInformationModel>> {
        return clonedAppDao.getAppsByProfile(profileUserId)
    }

    override fun getAllClonedAppsFlow(): Flow<List<ClonedApplicationInformationModel>> {
        return clonedAppDao.getAllClonedApps()
    }

    override suspend fun getClonedAppInfo(id: Long): ClonedAppInfo? {
        val app = clonedAppDao.getAppById(id) ?: return null
        return ClonedAppInfo(
            clonedApp = app,
            state = if (app.isEnabled) ClonedAppState.INSTALLED else ClonedAppState.FROZEN
        )
    }

    override suspend fun getInstallableApps(excludeSystem: Boolean): Result<List<InstallableApp>> {
        return packageInstaller.getInstallableApps(excludeSystem)
    }

    /**
     * R11-001 修复：幂等性克隆操作
     *
     * 使用事务日志模式确保分布式事务的原子性：
     * 1. 检查是否存在未完成的操作（幂等性检查）
     * 2. 创建PENDING操作记录
     * 3. 执行系统安装
     * 4. 创建数据库记录
     * 5. 清理操作记录
     *
     * 失败时自动触发补偿机制
     */
    override suspend fun cloneApp(packageName: String, profileUserId: Int): Result<ClonedApplicationInformationModel> {
        return withContext(Dispatchers.IO) {
            // 前置检查 Shizuku 状态
            if (!ShizukuServiceConnectionProvider.isShizukuAvailable()) {
                return@withContext Result.failure(
                    ShizukuException("Shizuku服务不可用，请先安装并启动Shizuku")
                )
            }
            if (!ShizukuServiceConnectionProvider.hasPermission()) {
                return@withContext Result.failure(
                    ShizukuException("未获得Shizuku权限授权")
                )
            }

            // R11-001: 使用互斥锁防止并发克隆同一应用
            cloneMutex.withLock {
                try {
                    // 检查是否已在当前配置文件中克隆过该应用
                    val existingApp = clonedAppDao.getAppByPackageAndUser(packageName, profileUserId)
                    if (existingApp != null) {
                        ApplicationLoggingServiceManager.i(TAG, "应用已存在，返回现有记录: $packageName")
                        return@withContext Result.success(existingApp)
                    }

                    // R11-001: 检查是否存在未完成的操作（幂等性检查）
                    val existingOperation = pendingOperationDao.getOperationByPackageAndProfile(packageName, profileUserId)
                    if (existingOperation != null && existingOperation.status != OperationStatus.COMPLETED) {
                        if (existingOperation.isRetryable()) {
                            ApplicationLoggingServiceManager.i(TAG, "发现可重试的操作，继续执行: ${existingOperation.operationId}")
                            return@withContext executeCloneWithOperation(existingOperation)
                        } else if (existingOperation.status == OperationStatus.PENDING || 
                                   existingOperation.status == OperationStatus.INSTALLING) {
                            ApplicationLoggingServiceManager.w(TAG, "发现进行中的操作，等待完成或超时: ${existingOperation.operationId}")
                            return@withContext Result.failure(
                                IllegalStateException("该应用正在克隆中，请等待完成或稍后重试")
                            )
                        }
                    }

                    // 生成新的操作ID
                    val operationId = UUID.randomUUID().toString()
                    val cloneCount = clonedAppDao.getCloneCountByPackageAndProfile(packageName, profileUserId)
                    val cloneIndex = cloneCount + 1

                    // R11-001: 创建PENDING操作记录
                    val pendingOperation = PendingCloneOperation(
                        operationId = operationId,
                        packageName = packageName,
                        profileUserId = profileUserId,
                        cloneIndex = cloneIndex,
                        status = OperationStatus.PENDING
                    )
                    pendingOperationDao.insertOperation(pendingOperation)

                    // 执行克隆操作
                    executeCloneWithOperation(pendingOperation)

                } catch (e: Exception) {
                    ApplicationLoggingServiceManager.e(TAG, "克隆应用异常: $packageName", e)
                    Result.failure(e)
                }
            }
        }
    }

    /**
     * R11-001: 执行带操作日志的克隆
     *
     * 完整的分布式事务流程：
     * 1. 更新状态为INSTALLING
     * 2. 执行系统安装（带超时）
     * 3. 创建数据库记录
     * 4. 清理操作日志或标记完成
     * 5. 失败时触发补偿
     */
    private suspend fun executeCloneWithOperation(
        operation: PendingCloneOperation
    ): Result<ClonedApplicationInformationModel> = withContext(Dispatchers.IO) {
        val operationId = operation.operationId
        val packageName = operation.packageName
        val profileUserId = operation.profileUserId
        val cloneIndex = operation.cloneIndex

        var systemInstalled = false

        try {
            // 步骤1: 更新状态为INSTALLING
            pendingOperationDao.updateStatus(operationId, OperationStatus.INSTALLING)

            // 获取应用信息
            val appName = packageInstaller.getAppName(packageName) ?: packageName
            val icon = ApplicationIconMemoryCacheManager.getIconDrawableSync(context, packageName)

            // 步骤2: 执行系统安装（带超时保护）
            val installResult = withTimeout(CLONE_OPERATION_TIMEOUT_MS) {
                packageInstaller.installAppToUser(packageName, profileUserId)
            }

            if (installResult.isFailure) {
                val error = installResult.exceptionOrNull() ?: Exception("安装应用失败")
                ApplicationLoggingServiceManager.e(TAG, "安装应用失败: $packageName", error)
                pendingOperationDao.updateStatus(operationId, OperationStatus.FAILED, errorMessage = error.message)
                return@withContext Result.failure(error)
            }

            systemInstalled = true

            // 步骤3: 创建数据库记录
            val clonedApp = ClonedApplicationInformationModel(
                packageName = packageName,
                appName = appName,
                profileUserId = profileUserId,
                cloneIndex = cloneIndex,
                appIcon = icon?.toString()
            )

            val id = clonedAppDao.insertApp(clonedApp)
            if (id <= 0) {
                throw Exception("数据库插入失败，返回无效ID: $id")
            }
            val savedApp = clonedApp.copy(id = id)

            // 步骤4: 标记操作完成并清理
            pendingOperationDao.deleteOperation(operationId)

            ApplicationLoggingServiceManager.logOperation(
                "CloneApp",
                "packageName=$packageName, userId=$profileUserId, operationId=$operationId"
            )

            Result.success(savedApp)

        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "克隆操作失败: $packageName, operationId=$operationId", e)

            // R11-001: 失败时更新操作状态
            pendingOperationDao.updateStatus(
                operationId,
                OperationStatus.FAILED,
                errorMessage = e.message
            )

            // R11-001: 触发补偿机制（如果系统已安装但数据库失败）
            if (systemInstalled) {
                ApplicationLoggingServiceManager.w(TAG, "触发补偿机制: 卸载已安装的应用 $packageName")
                triggerCompensation(packageName, profileUserId, operationId)
            }

            Result.failure(e)
        }
    }

    /**
     * R11-001: 补偿机制
     * 当系统安装成功但数据库操作失败时，回滚系统状态
     */
    private suspend fun triggerCompensation(
        packageName: String,
        profileUserId: Int,
        operationId: String
    ) {
        try {
            packageInstaller.uninstallAppFromUser(packageName, profileUserId)
            ApplicationLoggingServiceManager.i(TAG, "补偿成功: 已卸载 $packageName")
            pendingOperationDao.updateStatus(operationId, OperationStatus.COMPENSATED)
        } catch (compensationError: Exception) {
            ApplicationLoggingServiceManager.e(
                TAG,
                "补偿失败: 无法卸载 $packageName，可能需要手动清理",
                compensationError
            )
            // 补偿失败，保留FAILED状态以便后续人工处理
        }
    }

    /**
     * R11-001: 恢复未完成的操作
     * 应用启动时调用，处理崩溃或杀进程后的残留操作
     */
    suspend fun recoverIncompleteOperations(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 获取所有未完成的操作
            val incompleteOps = pendingOperationDao.getIncompleteOperations()
            val timeoutThreshold = System.currentTimeMillis() - PendingCloneOperation.OPERATION_TIMEOUT_MS
            val timeoutOps = pendingOperationDao.getTimeoutOperations(timeoutThreshold)

            var recoveredCount = 0

            // 处理超时的操作
            for (op in timeoutOps) {
                ApplicationLoggingServiceManager.w(TAG, "发现超时操作: ${op.operationId}")
                pendingOperationDao.updateStatus(op.operationId, OperationStatus.TIMEOUT)
                recoveredCount++
            }

            // 尝试恢复未完成的操作
            for (op in incompleteOps) {
                if (op.status == OperationStatus.INSTALLING) {
                    // 检查系统是否已安装
                    val isInstalled = packageInstaller.isAppInstalledInUser(op.packageName, op.profileUserId)
                        .getOrDefault(false)

                    if (isInstalled) {
                        // 系统已安装但数据库无记录，需要补偿
                        ApplicationLoggingServiceManager.w(
                            TAG,
                            "恢复：发现孤儿应用 ${op.packageName}，执行补偿卸载"
                        )
                        triggerCompensation(op.packageName, op.profileUserId, op.operationId)
                    } else {
                        // 系统未安装，可以重试
                        ApplicationLoggingServiceManager.i(TAG, "恢复：重试操作 ${op.operationId}")
                        pendingOperationDao.updateStatus(op.operationId, OperationStatus.PENDING)
                    }
                    recoveredCount++
                }
            }

            // 清理已完成的历史记录（7天前）
            val cleanupThreshold = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            pendingOperationDao.deleteCompletedOperations(cleanupThreshold)

            ApplicationLoggingServiceManager.i(TAG, "操作恢复完成，处理了 $recoveredCount 个操作")
            Result.success(recoveredCount)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "恢复操作失败", e)
            Result.failure(e)
        }
    }

    override suspend fun cloneApps(
        packageNames: List<String>,
        profileUserId: Int,
        onProgress: (Int, Int) -> Unit
    ): Result<List<ClonedApplicationInformationModel>> {
        return withContext(Dispatchers.IO) {
            val clonedApps = mutableListOf<ClonedApplicationInformationModel>()
            val failedPackages = mutableListOf<String>()

            packageNames.forEachIndexed { index, packageName ->
                onProgress(index + 1, packageNames.size)
                val result = cloneApp(packageName, profileUserId)
                result.onSuccess { app ->
                    clonedApps.add(app)
                }.onFailure { error ->
                    ApplicationLoggingServiceManager.w(TAG, "批量克隆失败: $packageName", error)
                    failedPackages.add(packageName)
                }
            }

            when {
                clonedApps.isEmpty() && packageNames.isNotEmpty() -> {
                    Result.failure(Exception("所有应用克隆失败: ${failedPackages.joinToString()}"))
                }
                failedPackages.isNotEmpty() -> {
                    // 部分成功，返回成功列表但记录警告
                    ApplicationLoggingServiceManager.w(
                        TAG,
                        "批量克隆部分失败，成功 ${clonedApps.size}/${packageNames.size}"
                    )
                    Result.success(clonedApps)
                }
                else -> {
                    Result.success(clonedApps)
                }
            }
        }
    }

    override suspend fun deleteClonedApp(clonedApp: ClonedApplicationInformationModel): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // R11-001: 检查是否有进行中的克隆操作
                val pendingOp = pendingOperationDao.getOperationByPackageAndProfile(
                    clonedApp.packageName,
                    clonedApp.profileUserId
                )
                if (pendingOp != null && 
                    (pendingOp.status == OperationStatus.PENDING || pendingOp.status == OperationStatus.INSTALLING)) {
                    return@withContext Result.failure(
                        IllegalStateException("该应用正在克隆中，请等待完成后再删除")
                    )
                }

                // 从目标用户空间卸载应用
                val uninstallResult = packageInstaller.uninstallAppFromUser(
                    clonedApp.packageName,
                    clonedApp.profileUserId
                )

                if (uninstallResult.isFailure) {
                    ApplicationLoggingServiceManager.e(TAG, "卸载应用失败", uninstallResult.exceptionOrNull())
                    return@withContext Result.failure(
                        uninstallResult.exceptionOrNull() ?: Exception("卸载应用失败: ${clonedApp.packageName}")
                    )
                }

                // 仅在卸载成功后才从数据库删除记录
                clonedAppDao.deleteApp(clonedApp)

                // 级联清理关联数据
                cleanupAssociatedData(clonedApp)

                ApplicationLoggingServiceManager.logOperation("DeleteApp", "packageName=${clonedApp.packageName}")

                Result.success(true)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "删除克隆应用异常", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 级联清理关联数据
     */
    private suspend fun cleanupAssociatedData(clonedApp: ClonedApplicationInformationModel) {
        try {
            appLockRepository.removeAppLock(clonedApp.packageName, clonedApp.profileUserId)
            automationRuleRepository.removePackageFromAllRules(clonedApp.packageName)
            freezeStrategyRepository.removePackageFromAllStrategies(clonedApp.packageName)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.w(TAG, "级联清理关联数据部分失败（主删除已成功）", e)
        }
    }

    override suspend fun launchClonedApp(clonedApp: ClonedApplicationInformationModel): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // 在目标用户空间启动应用
                val launchResult = packageInstaller.launchAppInUser(
                    clonedApp.packageName,
                    clonedApp.profileUserId
                )

                if (launchResult.isFailure) {
                    ApplicationLoggingServiceManager.e(TAG, "启动应用失败", launchResult.exceptionOrNull())
                    return@withContext Result.failure(
                        launchResult.exceptionOrNull() ?: Exception("启动应用失败")
                    )
                }

                // 更新启动统计
                clonedAppDao.updateLaunchStats(clonedApp.id, System.currentTimeMillis())

                ApplicationLoggingServiceManager.logOperation("LaunchApp", "packageName=${clonedApp.packageName}")

                Result.success(true)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "启动克隆应用异常", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun updateCustomName(id: Long, customName: String) {
        withContext(Dispatchers.IO) {
            val app = clonedAppDao.getAppById(id) ?: return@withContext
            clonedAppDao.updateApp(app.copy(
                customName = customName,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    override suspend fun isAppInstalledInUser(packageName: String, userId: Int): Result<Boolean> {
        return packageInstaller.isAppInstalledInUser(packageName, userId)
    }

    override suspend fun getAppCloneStatus(packageName: String): List<ClonedApplicationInformationModel> {
        return withContext(Dispatchers.IO) {
            clonedAppDao.getAppsByPackageName(packageName)
        }
    }

    override suspend fun getClonedAppsByProfile(profileUserId: Int): List<ClonedApplicationInformationModel> {
        return withContext(Dispatchers.IO) {
            clonedAppDao.getAppsByProfileList(profileUserId)
        }
    }
}
