package com.appclonex.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appclonex.domain.usecase.automation.ExecuteAutoFreezeUseCase
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 自动冻结定时任务
 *
 * 定期检查自动冻结规则并执行冻结操作
 * 使用 WorkManager 周期性调度
 */
@HiltWorker
class AutoFreezeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val executeAutoFreezeUseCase: ExecuteAutoFreezeUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            ApplicationLoggingServiceManager.i("AutoFreezeWorker", "开始执行自动冻结检查")

            return@withContext try {
                val frozenCount = executeAutoFreezeUseCase()

                if (frozenCount.isSuccess) {
                    ApplicationLoggingServiceManager.i(
                        "AutoFreezeWorker",
                        "自动冻结完成，共冻结 ${frozenCount.getOrNull() ?: 0} 个应用"
                    )
                    Result.success()
                } else {
                    ApplicationLoggingServiceManager.e(
                        "AutoFreezeWorker",
                        "自动冻结失败: ${frozenCount.exceptionOrNull()?.message}"
                    )
                    Result.retry()
                }
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e("AutoFreezeWorker", "自动冻结异常", e)
                Result.retry()
            }
        }
    }
}
