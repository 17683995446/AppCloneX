package com.appclonex.infrastructure.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.domain.model.FreezeCondition
import com.appclonex.domain.model.FreezeExecutionResult
import com.appclonex.domain.model.FreezeStrategy
import com.appclonex.domain.model.ConditionOperator
import com.appclonex.domain.model.ConditionType
import com.appclonex.domain.repository.FreezeStrategyRepository
import com.appclonex.domain.repository.FreezingService
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import com.appclonex.infrastructure.persistence.database.ApplicationDatabaseConfiguration
import com.appclonex.infrastructure.persistence.database.ClonedApplicationDataAccessObject
import com.appclonex.infrastructure.persistence.database.UserProfileEntityDataAccessObject
import com.appclonex.infrastructure.utility.AtomicFileWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Calendar
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 智能冻结策略仓库实现
 *
 * 使用 JSON 文件持久化冻结策略数据，避免对 Room 数据库进行 schema 变更。
 * 策略文件存储路径: context.filesDir/freeze_strategies.json
 *
 * 通过 [MutableStateFlow] 提供响应式数据流，每次写入操作后自动更新缓存和流。
 * 冻结/解冻操作通过 Room DAO 更新克隆应用的 [ClonedApplicationInformationModel.isEnabled] 字段。
 */
@Singleton
class FreezeStrategyRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ApplicationDatabaseConfiguration,
    private val clonedAppDao: ClonedApplicationDataAccessObject,
    private val userProfileDao: UserProfileEntityDataAccessObject,
    private val freezingService: FreezingService
) : FreezeStrategyRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val strategyFile: File
        get() = File(context.filesDir, STRATEGY_FILE_NAME)

    /** 内存缓存，避免频繁文件 I/O */
    private val cachedStrategies = MutableStateFlow<List<FreezeStrategy>>(emptyList())

    /** 互斥锁，保证并发操作安全 */
    private val mutex = Mutex()

    /** 自增 ID 生成器 */
    private val nextIdGenerator = AtomicLong(1L)

    companion object {
        private const val TAG = "FreezeStrategyRepo"
        private const val STRATEGY_FILE_NAME = "freeze_strategies.json"
    }

    // ==================== 初始化 ====================

    init {
        loadStrategiesFromFile()
    }

    /**
     * 从 JSON 文件加载策略到内存缓存
     */
    private fun loadStrategiesFromFile() {
        try {
            if (!strategyFile.exists()) {
                cachedStrategies.value = emptyList()
                return
            }
            val content = strategyFile.readText(Charsets.UTF_8)
            if (content.isBlank()) {
                cachedStrategies.value = emptyList()
                return
            }
            val strategies: List<FreezeStrategy> = json.decodeFromString(content)
            cachedStrategies.value = strategies
            // 更新 ID 生成器，确保新插入的 ID 不会与已有记录冲突
            val maxId = strategies.maxOfOrNull { it.id } ?: 0L
            nextIdGenerator.set(maxId + 1L)
            ApplicationLoggingServiceManager.d(TAG, "从文件加载了 ${strategies.size} 条冻结策略")
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "加载冻结策略文件失败", e)
            cachedStrategies.value = emptyList()
        }
    }

    /**
     * 将当前策略列表持久化到 JSON 文件
     */
    private suspend fun saveStrategiesToFile(strategies: List<FreezeStrategy>) {
        withContext(Dispatchers.IO) {
            try {
                val content = json.encodeToString(strategies)
                AtomicFileWriter.write(strategyFile, content)
                ApplicationLoggingServiceManager.d(TAG, "已保存 ${strategies.size} 条冻结策略到文件")
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "保存冻结策略文件失败", e)
                throw e
            }
        }
    }

    /**
     * 生成下一个唯一 ID
     */
    private fun generateNextId(): Long {
        return nextIdGenerator.getAndIncrement()
    }

    // ==================== CRUD ====================

    override fun getAllStrategies(): Flow<List<FreezeStrategy>> {
        return cachedStrategies.asStateFlow()
    }

    override suspend fun getStrategyById(id: Long): FreezeStrategy? {
        return cachedStrategies.value.find { it.id == id }
    }

    override suspend fun insertStrategy(strategy: FreezeStrategy): Long {
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                val newId = generateNextId()
                val newStrategy = strategy.copy(
                    id = newId,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                val updatedList = cachedStrategies.value.toMutableList().apply {
                    add(newStrategy)
                }
                cachedStrategies.value = updatedList
                saveStrategiesToFile(updatedList)
                ApplicationLoggingServiceManager.logOperation(
                    "InsertFreezeStrategy",
                    "id=$newId, name=${newStrategy.name}"
                )
                newId
            }
        }
    }

    override suspend fun updateStrategy(strategy: FreezeStrategy) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val currentList = cachedStrategies.value
                val index = currentList.indexOfFirst { it.id == strategy.id }
                if (index == -1) {
                    ApplicationLoggingServiceManager.e(
                        TAG,
                        "更新策略失败: 未找到 id=${strategy.id}"
                    )
                    return@withContext
                }
                val updatedStrategy = strategy.copy(
                    updatedAt = System.currentTimeMillis()
                )
                val updatedList = currentList.toMutableList().apply {
                    set(index, updatedStrategy)
                }
                cachedStrategies.value = updatedList
                saveStrategiesToFile(updatedList)
                ApplicationLoggingServiceManager.logOperation(
                    "UpdateFreezeStrategy",
                    "id=${strategy.id}, name=${strategy.name}"
                )
            }
        }
    }

    override suspend fun deleteStrategy(id: Long) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val updatedList = cachedStrategies.value.filter { it.id != id }
                cachedStrategies.value = updatedList
                saveStrategiesToFile(updatedList)
                ApplicationLoggingServiceManager.logOperation(
                    "DeleteFreezeStrategy",
                    "id=$id"
                )
            }
        }
    }

    override suspend fun deleteAllStrategies() {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                cachedStrategies.value = emptyList()
                saveStrategiesToFile(emptyList())
                nextIdGenerator.set(1L)
                ApplicationLoggingServiceManager.logOperation("DeleteAllFreezeStrategies", "")
            }
        }
    }

    // ==================== 执行 ====================

    override suspend fun executeStrategy(strategyId: Long): Result<FreezeExecutionResult> {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val strategy = getStrategyById(strategyId)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("未找到策略: id=$strategyId")
                    )

                if (!strategy.isEnabled) {
                    return@withContext Result.failure(
                        IllegalStateException("策略已禁用: ${strategy.name}")
                    )
                }

                // 评估条件
                val conditionsMet = evaluateConditions(strategy)
                if (!conditionsMet) {
                    ApplicationLoggingServiceManager.d(
                        TAG,
                        "策略 [${strategy.name}] 条件不满足，跳过执行"
                    )
                    return@withContext Result.success(
                        FreezeExecutionResult(
                            strategyId = strategy.id,
                            strategyName = strategy.name,
                            durationMs = System.currentTimeMillis() - startTime
                        )
                    )
                }

                // 根据调度配置决定冻结还是解冻
                val schedule = strategy.schedule
                val shouldFreeze = if (schedule != null) {
                    schedule.freezeOnStart && isInScheduleWindow(schedule)
                } else {
                    true // 无调度配置时默认执行冻结
                }

                val result = if (shouldFreeze) {
                    freezeAppsByStrategy(strategy)
                } else {
                    unfreezeAppsByStrategy(strategy)
                }

                result.getOrThrow().copy(durationMs = System.currentTimeMillis() - startTime)
                    .let { Result.success(it) }
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "执行策略失败: id=$strategyId", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun executeAllEnabledStrategies(): List<FreezeExecutionResult> {
        val enabledStrategies = cachedStrategies.value.filter { it.isEnabled }
        val results = mutableListOf<FreezeExecutionResult>()

        for (strategy in enabledStrategies) {
            val result = executeStrategy(strategy.id)
            result.onSuccess { results.add(it) }
        }

        ApplicationLoggingServiceManager.d(
            TAG,
            "执行了 ${enabledStrategies.size} 条已启用策略，成功 ${results.size} 条"
        )
        return results
    }

    // ==================== 评估 ====================

    override suspend fun evaluateConditions(strategy: FreezeStrategy): Boolean {
        val conditions = strategy.conditions
        if (conditions.isEmpty()) {
            // 无条件约束，默认满足
            return true
        }

        return conditions.all { condition ->
            evaluateSingleCondition(condition)
        }
    }

    /**
     * 评估单个冻结条件
     *
     * @param condition 待评估的条件
     * @return true 表示条件满足
     */
    private fun evaluateSingleCondition(condition: FreezeCondition): Boolean {
        return try {
            val actualValue = getConditionActualValue(condition.type)
            compareCondition(actualValue, condition)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(
                TAG,
                "评估条件失败: type=${condition.type}",
                e
            )
            false
        }
    }

    /**
     * 获取条件的实际值
     *
     * @param type 条件类型
     * @return 条件对应的实际系统值
     */
    private fun getConditionActualValue(type: ConditionType): Long {
        return when (type) {
            ConditionType.BATTERY_LEVEL -> getBatteryLevel()
            ConditionType.TIME_OF_DAY -> getCurrentTimeOfDayMinutes()
            ConditionType.DAY_OF_WEEK -> getCurrentDayOfWeek()
            ConditionType.WIFI_CONNECTED -> if (isWifiConnected()) 1L else 0L
            ConditionType.SCREEN_STATE -> if (isScreenOn()) 1L else 0L
            ConditionType.APP_INACTIVITY -> 0L // 需要 UsageStatsManager 支持，暂返回0
            ConditionType.MEMORY_USAGE -> getMemoryUsagePercentage()
            ConditionType.STORAGE_USAGE -> getStorageUsagePercentage()
        }
    }

    /**
     * 比较条件值与实际值
     */
    private fun compareCondition(actualValue: Long, condition: FreezeCondition): Boolean {
        val threshold = condition.threshold
        val valueLong = condition.value.toLongOrNull() ?: 0L

        return when (condition.operator) {
            ConditionOperator.EQUALS -> actualValue == threshold
            ConditionOperator.NOT_EQUALS -> actualValue != threshold
            ConditionOperator.GREATER_THAN -> actualValue > threshold
            ConditionOperator.LESS_THAN -> actualValue < threshold
            ConditionOperator.BETWEEN -> {
                // value 字段存储上限值
                actualValue in threshold..valueLong
            }
            ConditionOperator.CONTAINS -> {
                // 对数值条件，contains 等同于 equals
                actualValue == threshold
            }
        }
    }

    override suspend fun shouldFreeze(packageName: String): Boolean {
        val enabledStrategies = cachedStrategies.value.filter { it.isEnabled }
        for (strategy in enabledStrategies) {
            val targetsApps = strategy.targetApps
            val targetsMatch = targetsApps.isEmpty() || packageName in targetsApps
            if (!targetsMatch) continue

            val conditionsMet = evaluateConditions(strategy)
            if (conditionsMet) {
                return true
            }
        }
        return false
    }

    // ==================== 批量操作 ====================

    override suspend fun freezeAppsByStrategy(strategy: FreezeStrategy): Result<FreezeExecutionResult> {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val targetApps = resolveTargetApps(strategy)
                if (targetApps.isEmpty()) {
                    return@withContext Result.success(
                        FreezeExecutionResult(
                            strategyId = strategy.id,
                            strategyName = strategy.name,
                            durationMs = System.currentTimeMillis() - startTime
                        )
                    )
                }

                val frozenApps = mutableListOf<String>()
                val failedApps = mutableListOf<String>()

                for (app in targetApps) {
                    try {
                        // 通过 Shizuku 真正冻结应用
                        val freezeResult = freezingService.freezeApp(
                            app.packageName,
                            app.profileUserId
                        )
                        if (freezeResult.isSuccess) {
                            // 冻结成功后同步更新 Room 数据库状态
                            val updatedApp = app.copy(
                                isEnabled = false,
                                updatedAt = System.currentTimeMillis()
                            )
                            clonedAppDao.updateApp(updatedApp)
                            frozenApps.add(app.packageName)
                        } else {
                            val error = freezeResult.exceptionOrNull()
                            ApplicationLoggingServiceManager.e(
                                TAG,
                                "Shizuku 冻结应用失败: ${app.packageName}",
                                error
                            )
                            failedApps.add(app.packageName)
                        }
                    } catch (e: Exception) {
                        ApplicationLoggingServiceManager.e(
                            TAG,
                            "冻结应用异常: ${app.packageName}",
                            e
                        )
                        failedApps.add(app.packageName)
                    }
                }

                ApplicationLoggingServiceManager.logOperation(
                    "FreezeAppsByStrategy",
                    "strategy=${strategy.name}, frozen=${frozenApps.size}, failed=${failedApps.size}"
                )

                Result.success(
                    FreezeExecutionResult(
                        strategyId = strategy.id,
                        strategyName = strategy.name,
                        frozenCount = frozenApps.size,
                        failedApps = failedApps,
                        durationMs = System.currentTimeMillis() - startTime
                    )
                )
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "批量冻结失败", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun unfreezeAppsByStrategy(strategy: FreezeStrategy): Result<FreezeExecutionResult> {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val targetApps = resolveTargetApps(strategy)
                if (targetApps.isEmpty()) {
                    return@withContext Result.success(
                        FreezeExecutionResult(
                            strategyId = strategy.id,
                            strategyName = strategy.name,
                            durationMs = System.currentTimeMillis() - startTime
                        )
                    )
                }

                val unfrozenApps = mutableListOf<String>()
                val failedApps = mutableListOf<String>()

                for (app in targetApps) {
                    try {
                        // 通过 Shizuku 真正解冻应用
                        val unfreezeResult = freezingService.unfreezeApp(
                            app.packageName,
                            app.profileUserId
                        )
                        if (unfreezeResult.isSuccess) {
                            // 解冻成功后同步更新 Room 数据库状态
                            val updatedApp = app.copy(
                                isEnabled = true,
                                updatedAt = System.currentTimeMillis()
                            )
                            clonedAppDao.updateApp(updatedApp)
                            unfrozenApps.add(app.packageName)
                        } else {
                            val error = unfreezeResult.exceptionOrNull()
                            ApplicationLoggingServiceManager.e(
                                TAG,
                                "Shizuku 解冻应用失败: ${app.packageName}",
                                error
                            )
                            failedApps.add(app.packageName)
                        }
                    } catch (e: Exception) {
                        ApplicationLoggingServiceManager.e(
                            TAG,
                            "解冻应用异常: ${app.packageName}",
                            e
                        )
                        failedApps.add(app.packageName)
                    }
                }

                ApplicationLoggingServiceManager.logOperation(
                    "UnfreezeAppsByStrategy",
                    "strategy=${strategy.name}, unfrozen=${unfrozenApps.size}, failed=${failedApps.size}"
                )

                Result.success(
                    FreezeExecutionResult(
                        strategyId = strategy.id,
                        strategyName = strategy.name,
                        unfrozenCount = unfrozenApps.size,
                        failedApps = failedApps,
                        durationMs = System.currentTimeMillis() - startTime
                    )
                )
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "批量解冻失败", e)
                Result.failure(e)
            }
        }
    }

    // ==================== 辅助方法 ====================

    // ==================== 级联清理 ====================

    override suspend fun deleteStrategiesReferencingProfile(profileId: Int): Int {
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                val currentList = cachedStrategies.value
                // 删除 targetProfiles 包含该 profileId 的策略
                val filteredList = currentList.filter { strategy ->
                    profileId !in strategy.targetProfiles
                }
                val deletedCount = currentList.size - filteredList.size
                if (deletedCount > 0) {
                    cachedStrategies.value = filteredList
                    saveStrategiesToFile(filteredList)
                    ApplicationLoggingServiceManager.i(
                        TAG,
                        "已删除引用配置文件 $profileId 的 $deletedCount 条冻结策略"
                    )
                }
                deletedCount
            }
        }
    }

    override suspend fun removePackageFromAllStrategies(packageName: String): Int {
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                val currentList = cachedStrategies.value
                var updatedCount = 0
                val updatedList = currentList.map { strategy ->
                    if (packageName in strategy.targetApps) {
                        updatedCount++
                        strategy.copy(
                            targetApps = strategy.targetApps - packageName,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        strategy
                    }
                }
                if (updatedCount > 0) {
                    cachedStrategies.value = updatedList
                    saveStrategiesToFile(updatedList)
                    ApplicationLoggingServiceManager.i(
                        TAG,
                        "已从 $updatedCount 条冻结策略中移除包名: $packageName"
                    )
                }
                updatedCount
            }
        }
    }

    /**
     * 解析策略的目标应用列表
     *
     * 根据 [FreezeStrategy.targetApps] 和 [FreezeStrategy.targetProfiles] 从 Room 数据库中
     * 查询对应的克隆应用。如果 targetApps 为空则返回所有克隆应用，
     * 如果 targetProfiles 为空则不按配置文件过滤。
     */
    private suspend fun resolveTargetApps(strategy: FreezeStrategy): List<ClonedApplicationInformationModel> {
        val targetPackageNames = strategy.targetApps
        val targetProfileIds = strategy.targetProfiles

        return if (targetPackageNames.isEmpty() && targetProfileIds.isEmpty()) {
            // 无特定目标，返回所有克隆应用
            getAllClonedAppsFromDb()
        } else if (targetPackageNames.isNotEmpty() && targetProfileIds.isNotEmpty()) {
            // 同时指定了应用和配置文件
            val apps = mutableListOf<ClonedApplicationInformationModel>()
            for (profileId in targetProfileIds) {
                val profileApps = clonedAppDao.getAppsByProfileList(profileId)
                apps.addAll(profileApps.filter { it.packageName in targetPackageNames })
            }
            apps
        } else if (targetPackageNames.isNotEmpty()) {
            // 仅指定了应用包名
            val apps = mutableListOf<ClonedApplicationInformationModel>()
            for (packageName in targetPackageNames) {
                apps.addAll(clonedAppDao.getAppsByPackageName(packageName))
            }
            apps
        } else {
            // 仅指定了配置文件
            val apps = mutableListOf<ClonedApplicationInformationModel>()
            for (profileId in targetProfileIds) {
                apps.addAll(clonedAppDao.getAppsByProfileList(profileId))
            }
            apps
        }
    }

    /**
     * 从数据库获取所有克隆应用
     */
    private suspend fun getAllClonedAppsFromDb(): List<ClonedApplicationInformationModel> {
        val apps = mutableListOf<ClonedApplicationInformationModel>()
        val profiles = userProfileDao.getAllProfiles().first()
        for (profile in profiles) {
            apps.addAll(clonedAppDao.getAppsByProfileList(profile.userId))
        }
        return apps
    }

    /**
     * 获取当前电池电量百分比
     *
     * @return 0-100 的整数值
     */
    private fun getBatteryLevel(): Long {
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, batteryFilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) {
            ((level * 100L) / scale)
        } else {
            100L // 默认返回满电
        }
    }

    /**
     * 获取当前时间（从午夜开始的分钟数）
     *
     * @return 0-1439 的整数值
     */
    private fun getCurrentTimeOfDayMinutes(): Long {
        val calendar = Calendar.getInstance()
        return (calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)).toLong()
    }

    /**
     * 获取当前星期几
     *
     * @return 1=Monday ... 7=Sunday
     */
    private fun getCurrentDayOfWeek(): Long {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // Calendar.SUNDAY = 1, 需要转换为 ISO 标准 (1=Monday ... 7=Sunday)
        return if (dayOfWeek == Calendar.SUNDAY) 7L else dayOfWeek.toLong()
    }

    /**
     * 检查 WiFi 是否已连接
     *
     * @return true 表示 WiFi 已连接
     */
    @Suppress("DEPRECATION")
    private fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
        }
    }

    /**
     * 检查屏幕是否亮起
     *
     * @return true 表示屏幕亮起
     */
    private fun isScreenOn(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE)
            as? android.os.PowerManager ?: return true
        return powerManager.isInteractive
    }

    /**
     * 获取内存使用百分比
     *
     * @return 0-100 的整数值
     */
    private fun getMemoryUsagePercentage(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            if (maxMemory > 0) {
                (usedMemory * 100L) / maxMemory
            } else {
                0L
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "获取内存使用率失败", e)
            0L
        }
    }

    /**
     * 获取存储使用百分比
     *
     * @return 0-100 的整数值
     */
    private fun getStorageUsagePercentage(): Long {
        return try {
            val stat = android.os.StatFs(context.filesDir.absolutePath)
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            val usedBytes = totalBytes - availableBytes
            if (totalBytes > 0) {
                (usedBytes * 100L) / totalBytes
            } else {
                0L
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "获取存储使用率失败", e)
            0L
        }
    }

    /**
     * 检查当前时间是否在调度窗口内
     *
     * @param schedule 冻结调度配置
     * @return true 表示当前时间在调度窗口内
     */
    private fun isInScheduleWindow(schedule: com.appclonex.domain.model.FreezeSchedule): Boolean {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isoDay = if (currentDayOfWeek == Calendar.SUNDAY) 7 else currentDayOfWeek

        if (isoDay !in schedule.repeatDays) {
            return false
        }

        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = schedule.startTimeHour * 60 + schedule.startTimeMinute
        val endMinutes = schedule.endTimeHour * 60 + schedule.endTimeMinute

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            // 跨午夜的情况，例如 22:00 - 06:00
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
}
