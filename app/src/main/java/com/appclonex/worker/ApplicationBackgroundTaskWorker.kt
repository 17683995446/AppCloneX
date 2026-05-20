package com.appclonex.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.appclonex.domain.repository.ClonedApplicationRepository
import com.appclonex.domain.repository.FreezingService
import com.appclonex.infrastructure.system.UserProfileConfigurationManager
import com.appclonex.widget.WidgetAction
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * 应用后台任务 Worker
 *
 * 使用 WorkManager 处理 Widget 点击操作，确保在应用后台时也能正常执行
 */
@HiltWorker
class ApplicationBackgroundTaskWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val freezingService: FreezingService,
    private val userProfileManager: UserProfileConfigurationManager,
    private val clonedApplicationRepository: ClonedApplicationRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ApplicationBackgroundTaskWorker"
        private const val KEY_ACTION = "action"
        private const val PREFS_NAME = "appclonex_widget_prefs"
        private const val KEY_LAST_LAUNCHED_PACKAGE = "last_launched_package"

        /**
         * 调度后台任务
         */
        fun schedule(context: Context, action: String) {
            val inputData = Data.Builder()
                .putString(KEY_ACTION, action)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ApplicationBackgroundTaskWorker>()
                .setInputData(inputData)
                .addTag("widget_action")
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    override suspend fun doWork(): Result {
        val action = inputData.getString(KEY_ACTION) ?: return Result.failure()

        return try {
            when (action) {
                WidgetAction.ACTION_FREEZE_ALL -> freezeAllApps()
                WidgetAction.ACTION_NEW_PROFILE -> createNewProfile()
                WidgetAction.ACTION_QUICK_CLONE -> quickClone()
                WidgetAction.ACTION_OPEN_APP -> openApp()
                else -> Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Widget action failed: $action", e)
            Result.failure()
        }
    }

    private suspend fun freezeAllApps(): Result {
        return try {
            // 获取所有用户配置文件
            val users = userProfileManager.getAllUsers().getOrNull() ?: return Result.failure()

            var totalFrozenCount = 0

            // 遍历每个用户配置文件，冻结其下的克隆应用
            for (userInfo in users) {
                val userId = userInfo.id

                // 获取该用户下的克隆应用列表
                val clonedApps = clonedApplicationRepository.getClonedAppsFlow(userId).first()

                if (clonedApps.isEmpty()) {
                    continue
                }

                // 冻结该用户下的所有应用
                val freezeResult = freezingService.freezeAllApps(userId)

                if (freezeResult.isSuccess) {
                    totalFrozenCount += freezeResult.getOrNull() ?: 0
                    Log.d(TAG, "已冻结用户 $userId 下的 ${freezeResult.getOrNull() ?: 0} 个应用")
                }
            }

            Log.i(TAG, "批量冻结完成，共冻结 $totalFrozenCount 个应用")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "批量冻结应用失败", e)
            Result.failure()
        }
    }

    private suspend fun createNewProfile(): Result {
        return try {
            val result = userProfileManager.createUserProfile("Widget Profile")
            if (result.isSuccess) {
                val userId = result.getOrNull() ?: -1
                Log.i(TAG, "成功创建配置文件，用户ID: $userId")
                Result.success()
            } else {
                Log.e(TAG, "创建配置文件失败: ${result.exceptionOrNull()?.message}")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "创建配置文件异常", e)
            Result.failure()
        }
    }

    private suspend fun quickClone(): Result {
        return try {
            // 获取所有克隆应用，取最后一个（最近使用的）
            val allApps = clonedApplicationRepository.getAllClonedAppsFlow().first()
            val lastApp = allApps.lastOrNull()

            if (lastApp == null) {
                Log.w(TAG, "没有可克隆的应用")
                return Result.failure()
            }

            val packageName = lastApp.packageName
            val profileUserId = lastApp.profileUserId

            val result = clonedApplicationRepository.cloneApp(packageName, profileUserId)
            if (result.isSuccess) {
                Log.i(TAG, "快速克隆成功: $packageName -> 用户 $profileUserId")
                Result.success()
            } else {
                Log.e(TAG, "快速克隆失败: ${result.exceptionOrNull()?.message}")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "快速克隆异常", e)
            Result.failure()
        }
    }

    private suspend fun openApp(): Result {
        return try {
            // 从 SharedPreferences 获取上次启动的应用包名
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val packageName = prefs.getString(KEY_LAST_LAUNCHED_PACKAGE, null)

            if (packageName == null) {
                // 如果没有记录，尝试从克隆应用列表中获取最后一个
                val allApps = clonedApplicationRepository.getAllClonedAppsFlow().first()
                val lastApp = allApps.lastOrNull()

                if (lastApp == null) {
                    Log.w(TAG, "没有可打开的应用")
                    return Result.failure()
                }

                val launchIntent = applicationContext.packageManager.getLaunchIntentForPackage(lastApp.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    applicationContext.startActivity(launchIntent)
                    // 记录为上次启动的应用
                    prefs.edit().putString(KEY_LAST_LAUNCHED_PACKAGE, lastApp.packageName).apply()
                    Log.i(TAG, "成功打开应用: ${lastApp.packageName}")
                    Result.success()
                } else {
                    Log.w(TAG, "无法获取启动 Intent: ${lastApp.packageName}")
                    Result.failure()
                }
            } else {
                val launchIntent = applicationContext.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    applicationContext.startActivity(launchIntent)
                    Log.i(TAG, "成功打开应用: $packageName")
                    Result.success()
                } else {
                    Log.w(TAG, "无法获取启动 Intent: $packageName")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "打开应用异常", e)
            Result.failure()
        }
    }
}
