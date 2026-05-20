package com.appclonex.infrastructure.utility

import android.content.Context
import com.appclonex.domain.model.ActivityRecord
import com.appclonex.domain.model.TopUsedApp
import com.appclonex.domain.model.ApplicationUsageStatisticsModel as UsageStatistics
import com.appclonex.infrastructure.persistence.database.ApplicationDatabaseConfiguration as AppCloneXDatabase
import com.appclonex.infrastructure.persistence.database.ClonedApplicationDataAccessObject as ClonedAppDao
import com.appclonex.infrastructure.persistence.database.UserProfileEntityDataAccessObject as UserProfileDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用程序使用统计追踪器
 * 负责记录用户操作事件并聚合统计数据
 */
@Singleton
class ApplicationUsageStatisticsTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppCloneXDatabase
) {

    private val userProfileDao: UserProfileDao = database.userProfileDao()
    private val clonedAppDao: ClonedAppDao = database.clonedAppDao()

    /**
     * 记录启动事件
     * 更新 ClonedApp 的 launchCount 和 lastLaunchTime
     */
    suspend fun trackLaunch(packageName: String, profileUserId: Int) {
        val app = clonedAppDao.getAppByPackageAndUser(packageName, profileUserId) ?: return
        clonedAppDao.updateLaunchStats(app.id, System.currentTimeMillis())
    }

    /**
     * 记录克隆事件
     */
    suspend fun trackClone(packageName: String, profileUserId: Int) {
        // 克隆事件已通过 CloneRepository 处理，此处可用于额外的统计追踪
    }

    /**
     * 记录冻结事件
     */
    suspend fun trackFreeze(packageName: String, profileUserId: Int) {
        val app = clonedAppDao.getAppByPackageAndUser(packageName, profileUserId) ?: return
        clonedAppDao.updateApp(app.copy(
            isEnabled = false,
            updatedAt = System.currentTimeMillis()
        ))
    }

    /**
     * 记录删除事件
     */
    suspend fun trackDelete(packageName: String, profileUserId: Int) {
        // 删除事件已通过 CloneRepository 处理
    }

    /**
     * 从数据库聚合统计信息
     * 返回 Flow 实时响应数据变化
     */
    fun getStatistics(): Flow<UsageStatistics> {
        return combine(
            userProfileDao.getAllProfiles(),
            clonedAppDao.getAllClonedApps()
        ) { profiles, apps ->
            val totalProfiles = profiles.size
            val activeProfiles = profiles.count { it.isActive }
            val totalClonedApps = apps.size
            val totalLaunches = apps.sumOf { it.launchCount }
            val totalFrozenApps = apps.count { !it.isEnabled }
            val storageUsedBytes = apps.sumOf { it.size }

            // 最常使用的应用
            val topUsedApps = apps
                .filter { it.launchCount > 0 }
                .sortedByDescending { it.launchCount }
                .take(3)
                .map {
                    TopUsedApp(
                        packageName = it.packageName,
                        appName = it.getDisplayName(),
                        launchCount = it.launchCount
                    )
                }

            val mostUsedApp = topUsedApps.firstOrNull()?.appName
            val mostUsedAppLaunches = topUsedApps.firstOrNull()?.launchCount ?: 0

            // 最近活动：从应用记录中生成
            val recentActivity = generateRecentActivity(apps)

            UsageStatistics(
                totalProfiles = totalProfiles,
                activeProfiles = activeProfiles,
                totalClonedApps = totalClonedApps,
                totalLaunches = totalLaunches,
                totalFrozenApps = totalFrozenApps,
                mostUsedApp = mostUsedApp,
                mostUsedAppLaunches = mostUsedAppLaunches,
                topUsedApps = topUsedApps,
                recentActivity = recentActivity,
                storageUsedBytes = storageUsedBytes
            )
        }
    }

    /**
     * 获取最近活动记录
     * 基于应用的最后操作时间生成活动流
     */
    fun getRecentActivity(limit: Int = 20): Flow<List<ActivityRecord>> {
        return clonedAppDao.getAllClonedApps().map { apps ->
            generateRecentActivity(apps).take(limit)
        }
    }

    /**
     * 从克隆应用列表生成最近活动记录
     * 按时间倒序排列
     */
    private fun generateRecentActivity(apps: List<com.appclonex.domain.model.ClonedApplicationInformationModel>): List<ActivityRecord> {
        val activities = mutableListOf<ActivityRecord>()

        for (app in apps) {
            // 从最后启动时间生成启动活动
            if (app.lastLaunchTime > 0) {
                activities.add(
                    ActivityRecord(
                        packageName = app.packageName,
                        appName = app.getDisplayName(),
                        action = "launch",
                        timestamp = app.lastLaunchTime
                    )
                )
            }

            // 从更新时间推断其他操作
            if (app.updatedAt > app.createdAt && app.lastLaunchTime == 0L) {
                val action = if (!app.isEnabled) "freeze" else "clone"
                activities.add(
                    ActivityRecord(
                        packageName = app.packageName,
                        appName = app.getDisplayName(),
                        action = action,
                        timestamp = app.updatedAt
                    )
                )
            }

            // 创建时间作为克隆活动
            activities.add(
                ActivityRecord(
                    packageName = app.packageName,
                    appName = app.getDisplayName(),
                    action = "clone",
                    timestamp = app.createdAt
                )
            )
        }

        return activities
            .sortedByDescending { it.timestamp }
    }
}
