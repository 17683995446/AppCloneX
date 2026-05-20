package com.appclonex.infrastructure.monitoring

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Process
import androidx.core.content.getSystemService
import com.appclonex.infrastructure.logging.FirebaseCrashReportingService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用健康监控器
 *
 * 监控应用运行状态、内存使用、存储空间等健康指标
 * 当检测到异常时上报崩溃报告
 */
@Singleton
class AppHealthMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashReportingService: FirebaseCrashReportingService
) {

    private val activityManager = context.getSystemService<ActivityManager>()

    /**
     * 健康状态数据类
     */
    data class HealthStatus(
        val memoryStatus: MemoryStatus,
        val storageStatus: StorageStatus,
        val overallHealth: HealthLevel,
        val warnings: List<String>
    )

    /**
     * 内存状态
     */
    data class MemoryStatus(
        val totalMemory: Long,
        val availableMemory: Long,
        val usedMemory: Long,
        val appMemoryUsage: Long,
        val memoryUsagePercent: Float
    )

    /**
     * 存储状态
     */
    data class StorageStatus(
        val totalStorage: Long,
        val availableStorage: Long,
        val appStorageUsage: Long
    )

    /**
     * 健康等级
     */
    enum class HealthLevel {
        HEALTHY,      // 健康
        WARNING,      // 警告
        CRITICAL      // 严重
    }

    /**
     * 检查应用健康状态
     */
    fun checkHealth(): HealthStatus {
        val memoryStatus = checkMemoryStatus()
        val storageStatus = checkStorageStatus()
        val warnings = mutableListOf<String>()

        // 检查内存警告
        if (memoryStatus.memoryUsagePercent > 0.9) {
            warnings.add("内存使用率过高: ${(memoryStatus.memoryUsagePercent * 100).toInt()}%")
        }

        // 检查存储警告
        val storageUsagePercent = 1f - (storageStatus.availableStorage.toFloat() / storageStatus.totalStorage)
        if (storageUsagePercent > 0.9) {
            warnings.add("存储空间不足: 仅剩 ${storageStatus.availableStorage / 1024 / 1024}MB")
        }

        // 确定整体健康等级
        val overallHealth = when {
            warnings.isEmpty() -> HealthLevel.HEALTHY
            warnings.size >= 2 -> HealthLevel.CRITICAL
            else -> HealthLevel.WARNING
        }

        // 如果是严重状态，上报监控
        if (overallHealth == HealthLevel.CRITICAL) {
            reportCriticalHealth(memoryStatus, storageStatus, warnings)
        }

        return HealthStatus(
            memoryStatus = memoryStatus,
            storageStatus = storageStatus,
            overallHealth = overallHealth,
            warnings = warnings
        )
    }

    /**
     * 检查内存状态
     */
    private fun checkMemoryStatus(): MemoryStatus {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.maxMemory()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val availableMemory = totalMemory - usedMemory

        // 获取应用内存信息
        val appMemoryUsage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Debug.MemoryInfo().also { 
                Debug.getMemoryInfo(it) 
            }.totalPss * 1024L
        } else {
            usedMemory
        }

        return MemoryStatus(
            totalMemory = totalMemory,
            availableMemory = availableMemory,
            usedMemory = usedMemory,
            appMemoryUsage = appMemoryUsage.toLong(),
            memoryUsagePercent = usedMemory.toFloat() / totalMemory
        )
    }

    /**
     * 检查存储状态
     */
    private fun checkStorageStatus(): StorageStatus {
        val dataDir = context.filesDir
        val totalStorage = dataDir.totalSpace
        val availableStorage = dataDir.usableSpace

        // 计算应用存储使用
        val appStorageUsage = calculateAppStorageUsage()

        return StorageStatus(
            totalStorage = totalStorage,
            availableStorage = availableStorage,
            appStorageUsage = appStorageUsage
        )
    }

    /**
     * 计算应用存储使用量
     */
    private fun calculateAppStorageUsage(): Long {
        var totalSize = 0L
        context.filesDir?.let { totalSize += it.walkTopDown().map { f -> f.length() }.sum() }
        context.cacheDir?.let { totalSize += it.walkTopDown().map { f -> f.length() }.sum() }
        context.getDatabasePath("appclonex_db")?.let { 
            if (it.exists()) totalSize += it.length() 
        }
        return totalSize
    }

    /**
     * 上报严重健康状态
     */
    private fun reportCriticalHealth(
        memoryStatus: MemoryStatus,
        storageStatus: StorageStatus,
        warnings: List<String>
    ) {
        crashReportingService.recordException(
            RuntimeException("Critical App Health Status"),
            mapOf(
                "memory_usage_percent" to "${(memoryStatus.memoryUsagePercent * 100).toInt()}%",
                "available_memory_mb" to "${memoryStatus.availableMemory / 1024 / 1024}",
                "available_storage_mb" to "${storageStatus.availableStorage / 1024 / 1024}",
                "warnings" to warnings.joinToString("; "),
                "health_level" to "CRITICAL"
            )
        )
    }

    /**
     * 监控健康状态流
     */
    fun monitorHealth(intervalMs: Long = 60000): Flow<HealthStatus> = flow {
        while (true) {
            emit(checkHealth())
            kotlinx.coroutines.delay(intervalMs)
        }
    }.flowOn(Dispatchers.Default)

    /**
     * 检查是否处于低内存状态
     */
    fun isLowMemory(): Boolean {
        return activityManager?.let { am ->
            val memoryInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memoryInfo)
            memoryInfo.lowMemory
        } ?: false
    }

    /**
     * 获取内存信息
     */
    fun getMemoryInfo(): ActivityManager.MemoryInfo? {
        return activityManager?.let { am ->
            ActivityManager.MemoryInfo().also { 
                am.getMemoryInfo(it) 
            }
        }
    }

    /**
     * 清理缓存
     */
    fun clearCache(): Boolean {
        return try {
            context.cacheDir?.deleteRecursively()
            true
        } catch (e: Exception) {
            crashReportingService.recordException(e, mapOf("action" to "clear_cache"))
            false
        }
    }

    /**
     * 获取应用PID
     */
    fun getAppPid(): Int = Process.myPid()

    /**
     * 获取进程内存信息
     */
    fun getProcessMemoryInfo(): Debug.MemoryInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }
        } else {
            null
        }
    }
}
