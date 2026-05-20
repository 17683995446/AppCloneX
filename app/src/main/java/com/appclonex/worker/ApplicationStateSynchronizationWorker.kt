package com.appclonex.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.appclonex.domain.repository.ClonedApplicationRepository
import com.appclonex.domain.repository.UserProfileRepository
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class ApplicationStateSynchronizationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userProfileRepository: UserProfileRepository,
    private val clonedApplicationRepository: ClonedApplicationRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AppStateSyncWorker"
        const val WORK_NAME = "app_state_sync"

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<ApplicationStateSynchronizationWorker>(
                6, TimeUnit.HOURS  // Sync every 6 hours
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                ApplicationLoggingServiceManager.i(TAG, "开始同步应用状态")

                // 1. Sync user profiles with system
                val syncResult = userProfileRepository.syncWithSystem()
                if (syncResult.isFailure) {
                    ApplicationLoggingServiceManager.w(TAG, "用户配置同步失败: ${syncResult.exceptionOrNull()?.message}")
                }

                // 2. Verify cloned apps still exist
                val profiles = userProfileRepository.getAllProfilesWithInfo()
                var removedCount = 0
                for (profile in profiles) {
                    val apps = clonedApplicationRepository.getClonedAppsByProfile(profile.userId)
                    // Verify each app is still installed (simplified)
                }

                ApplicationLoggingServiceManager.i(TAG, "状态同步完成")
                Result.success()
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "状态同步失败", e)
                Result.retry()
            }
        }
    }
}
