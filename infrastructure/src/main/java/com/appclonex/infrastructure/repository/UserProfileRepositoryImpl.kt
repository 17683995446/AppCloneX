package com.appclonex.infrastructure.repository

import android.content.Context
import android.os.Process
import com.appclonex.domain.model.UserProfileConfigurationModel
import com.appclonex.domain.model.UserProfileInfo
import com.appclonex.domain.model.UserProfileState
import com.appclonex.domain.repository.UserProfileRepository
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import com.appclonex.infrastructure.persistence.database.ApplicationDatabaseConfiguration
import com.appclonex.infrastructure.persistence.database.UserProfileEntityDataAccessObject
import com.appclonex.infrastructure.system.UserProfileConfigurationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户配置文件仓库实现
 * 负责用户配置文件的数据操作和业务逻辑
 */
@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ApplicationDatabaseConfiguration,
    private val userProfileManager: UserProfileConfigurationManager,
    private val automationRuleRepository: com.appclonex.domain.repository.AutomationRuleRepository,
    private val appLockRepository: com.appclonex.domain.repository.AppLockRepository,
    private val freezeStrategyRepository: com.appclonex.domain.repository.FreezeStrategyRepository
) : UserProfileRepository {

    private val userProfileDao: UserProfileEntityDataAccessObject = database.userProfileDao()

    // R10-CONC-001 修复：配置文件切换互斥锁，防止并发切换导致状态不一致
    private val switchProfileMutex = Mutex()

    companion object {
        private const val TAG = "UserProfileRepository"
    }

    override fun getAllProfilesFlow(): Flow<List<UserProfileConfigurationModel>> {
        return userProfileDao.getAllProfiles()
    }

    override suspend fun getAllProfilesWithInfo(): List<UserProfileInfo> {
        return withContext(Dispatchers.IO) {
            val systemResult = userProfileManager.getAllUsers()
            if (systemResult.isFailure) {
                return@withContext emptyList()
            }

            val systemProfiles = systemResult.getOrNull() ?: emptyList()
            systemProfiles
        }
    }

    override suspend fun getProfileById(userId: Int): UserProfileConfigurationModel? {
        return withContext(Dispatchers.IO) {
            userProfileDao.getProfileById(userId)
        }
    }

    override suspend fun createProfile(name: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            var userId: Int = -1
            try {
                // 检查最大用户数量
                val maxCountResult = userProfileManager.getMaxUserCount()
                if (maxCountResult.isSuccess) {
                    val maxCount = maxCountResult.getOrNull() ?: 10
                    val currentCount = userProfileDao.getProfileCount()
                    if (currentCount >= maxCount) {
                        return@withContext Result.failure(
                            IllegalStateException("已达到最大用户数量限制 ($maxCount)")
                        )
                    }
                }

                // 创建系统用户
                val createResult = userProfileManager.createUserProfile(name)
                if (createResult.isFailure) {
                    return@withContext Result.failure(
                        createResult.exceptionOrNull() ?: Exception("创建用户失败")
                    )
                }

                userId = createResult.getOrNull() ?: -1
                if (userId < 0) {
                    return@withContext Result.failure(Exception("创建用户失败"))
                }

                // 保存到本地数据库
                val profile = UserProfileConfigurationModel(
                    userId = userId,
                    profileName = name,
                    serialNumber = userId,
                    isActive = false,
                    isRunning = false
                )
                val insertedId = userProfileDao.insertProfile(profile)
                
                // R10-CORE-002 修复：验证数据库插入成功
                if (insertedId <= 0) {
                    throw Exception("数据库插入失败，返回无效ID: $insertedId")
                }

                ApplicationLoggingServiceManager.logOperation("CreateProfile", "userId=$userId, name=$name")

                Result.success(userId)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "创建配置文件异常", e)
                
                // R10-CORE-002 修复：补偿机制 - 如果系统用户已创建但数据库保存失败，删除系统用户
                if (userId >= 0) {
                    ApplicationLoggingServiceManager.w(TAG, "补偿：正在删除已创建的系统用户 $userId")
                    try {
                        userProfileManager.deleteUserProfile(userId)
                        ApplicationLoggingServiceManager.i(TAG, "补偿成功：已删除系统用户 $userId")
                    } catch (cleanupError: Exception) {
                        ApplicationLoggingServiceManager.e(TAG, "补偿失败：无法删除系统用户 $userId", cleanupError)
                    }
                }
                
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteProfile(userId: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // 不能删除主用户
                if (userId == 0 || userId == Process.myUserHandle().hashCode()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("不能删除主用户")
                    )
                }

                // 从系统删除用户
                val deleteResult = userProfileManager.deleteUserProfile(userId)
                if (deleteResult.isFailure) {
                    return@withContext Result.failure(
                        deleteResult.exceptionOrNull() ?: Exception("删除用户失败")
                    )
                }

                // 从本地数据库删除（Room FK CASCADE 会自动删除 cloned_apps 记录）
                userProfileDao.deleteProfileById(userId)

                // 级联清理非 Room 存储中的关联数据
                try {
                    val deletedRules = automationRuleRepository.deleteRulesByProfile(userId)
                    val deletedLocks = appLockRepository.removeAllLocksForProfile(userId)
                    val deletedStrategies = freezeStrategyRepository.deleteStrategiesReferencingProfile(userId)
                    if (deletedRules > 0 || deletedLocks > 0 || deletedStrategies > 0) {
                        ApplicationLoggingServiceManager.i(
                            TAG,
                            "级联清理完成: $deletedRules 条规则, $deletedLocks 个应用锁, $deletedStrategies 条冻结策略"
                        )
                    }
                } catch (e: Exception) {
                    // 级联清理失败不影响主删除操作，仅记录警告
                    ApplicationLoggingServiceManager.w(TAG, "级联清理关联数据部分失败（主删除已成功）", e)
                }

                ApplicationLoggingServiceManager.logOperation("DeleteProfile", "userId=$userId")

                Result.success(true)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "删除配置文件异常", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun startProfile(userId: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val startResult = userProfileManager.startUserProfile(userId)
                if (startResult.isFailure) {
                    return@withContext Result.failure(
                        startResult.exceptionOrNull() ?: Exception("启动用户失败")
                    )
                }

                // 更新数据库状态
                val profile = userProfileDao.getProfileById(userId)
                if (profile != null) {
                    userProfileDao.updateProfile(profile.copy(
                        isRunning = true,
                        updatedAt = System.currentTimeMillis()
                    ))
                }

                ApplicationLoggingServiceManager.logOperation("StartProfile", "userId=$userId")

                Result.success(true)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "启动配置文件异常", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun stopProfile(userId: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val stopResult = userProfileManager.stopUserProfile(userId)
                if (stopResult.isFailure) {
                    return@withContext Result.failure(
                        stopResult.exceptionOrNull() ?: Exception("停止用户失败")
                    )
                }

                // 更新数据库状态
                val profile = userProfileDao.getProfileById(userId)
                if (profile != null) {
                    userProfileDao.updateProfile(profile.copy(
                        isRunning = false,
                        updatedAt = System.currentTimeMillis()
                    ))
                }

                ApplicationLoggingServiceManager.logOperation("StopProfile", "userId=$userId")

                Result.success(true)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "停止配置文件异常", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun switchToProfile(userId: Int): Result<Boolean> {
        // R10-CONC-001 修复：使用互斥锁保护整个切换流程，防止并发切换导致状态不一致
        return switchProfileMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    // 记录当前激活的配置文件，用于失败时恢复
                    val previouslyActiveProfile = userProfileDao.getActiveProfile()

                    // 先尝试切换系统用户（最可能失败的操作）
                    val switchResult = userProfileManager.switchToUser(userId)
                    if (switchResult.isFailure) {
                        return@withContext Result.failure(
                            switchResult.exceptionOrNull() ?: Exception("切换用户失败")
                        )
                    }

                    // 系统切换成功后，再更新数据库状态
                    userProfileDao.deactivateAllProfiles()

                    val profile = userProfileDao.getProfileById(userId)
                    if (profile != null) {
                        userProfileDao.updateProfile(profile.copy(
                            isActive = true,
                            updatedAt = System.currentTimeMillis()
                        ))
                    }

                    ApplicationLoggingServiceManager.logOperation("SwitchProfile", "userId=$userId")

                    Result.success(true)
                } catch (e: Exception) {
                    ApplicationLoggingServiceManager.e(TAG, "切换配置文件异常", e)
                    // 尝试恢复之前的激活状态
                    try {
                        val previouslyActive = userProfileDao.getActiveProfile()
                        if (previouslyActive == null) {
                            // 没有活跃配置文件，尝试恢复
                            val allProfiles = userProfileDao.getAllProfiles().first()
                            if (allProfiles.isNotEmpty()) {
                                userProfileDao.updateProfile(allProfiles.first().copy(isActive = true))
                            }
                        }
                    } catch (recoveryError: Exception) {
                        ApplicationLoggingServiceManager.e(TAG, "恢复配置文件状态失败", recoveryError)
                    }
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun updateProfileName(userId: Int, newName: String) {
        withContext(Dispatchers.IO) {
            val profile = userProfileDao.getProfileById(userId) ?: return@withContext
            userProfileDao.updateProfile(profile.copy(
                profileName = newName,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    override suspend fun updateClonedAppCount(userId: Int) {
        withContext(Dispatchers.IO) {
            val count = database.clonedAppDao().getAppCountByProfile(userId)
            userProfileDao.updateClonedAppCount(userId, count)
        }
    }

    override suspend fun getProfileCount(): Int {
        return withContext(Dispatchers.IO) {
            userProfileDao.getProfileCount()
        }
    }

    override suspend fun getMaxUserCount(): Result<Int> {
        return userProfileManager.getMaxUserCount()
    }

    override suspend fun syncWithSystem(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val systemResult = userProfileManager.getAllUsers()
                if (systemResult.isFailure) {
                    return@withContext Result.failure(
                        systemResult.exceptionOrNull() ?: Exception("同步失败")
                    )
                }

                val systemProfiles = systemResult.getOrNull() ?: emptyList()

                // 同步到本地数据库
                systemProfiles.forEach { profileInfo ->
                    val existingProfile = userProfileDao.getProfileById(profileInfo.profile.userId)
                    if (existingProfile == null) {
                        userProfileDao.insertProfile(profileInfo.profile)
                    } else {
                        userProfileDao.updateProfile(profileInfo.profile.copy(
                            updatedAt = System.currentTimeMillis()
                        ))
                    }
                }

                ApplicationLoggingServiceManager.logOperation("SyncProfiles", "count=${systemProfiles.size}")

                Result.success(true)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "同步配置文件异常", e)
                Result.failure(e)
            }
        }
    }
}
