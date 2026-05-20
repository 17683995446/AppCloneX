package com.appclonex.domain.usecase.automation

import android.app.ActivityManager
import android.content.Context
import com.appclonex.domain.model.AutoFreezeConfig
import com.appclonex.domain.repository.AutomationRuleRepository
import com.appclonex.domain.repository.FreezingService
import com.appclonex.domain.repository.ClonedApplicationRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 执行自动冻结用例
 *
 * 根据自动冻结配置，检查后台应用并执行冻结操作
 * R10-CORE-004 修复：添加前台应用检查，避免冻结正在使用的应用
 */
@Singleton
class ExecuteAutoFreezeUseCase @Inject constructor(
    private val context: Context,
    private val automationRuleRepository: AutomationRuleRepository,
    private val freezingService: FreezingService,
    private val clonedApplicationRepository: ClonedApplicationRepository
) {
    /**
     * 执行自动冻结检查
     *
     * 遍历所有启用的自动化规则，检查是否满足触发条件
     * 跳过前台运行的应用
     */
    suspend operator fun invoke(): Result<Int> {
        val config = automationRuleRepository.getAutoFreezeConfig()
        if (!config.enabled) return Result.success(0)

        // R10-CORE-004 修复：获取前台应用列表
        val foregroundApps = getForegroundApps()

        var frozenCount = 0
        val rules = automationRuleRepository.getAllRules().first()

        for (rule in rules) {
            if (rule.isEnabled) {
                for (packageName in rule.targetPackageNames) {
                    // R10-CORE-004 修复：跳过前台应用
                    if (packageName in foregroundApps) {
                        android.util.Log.i("AutoFreeze", "跳过前台应用: $packageName")
                        continue
                    }

                    freezingService.freezeApp(packageName, rule.profileId)
                        .onSuccess { frozenCount++ }
                        .onFailure { e ->
                            android.util.Log.w("AutoFreeze", "冻结失败: $packageName, ${e.message}")
                        }
                }
            }
        }

        return Result.success(frozenCount)
    }

    /**
     * 获取当前前台运行的应用包名列表
     * R10-CORE-004 修复：避免冻结用户正在使用的应用
     */
    private fun getForegroundApps(): Set<String> {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningProcesses = activityManager.runningAppProcesses
            runningProcesses
                ?.filter { it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }
                ?.flatMap { it.pkgList?.toList() ?: emptyList() }
                ?.toSet()
                ?: emptySet()
        } catch (e: Exception) {
            android.util.Log.e("AutoFreeze", "获取前台应用失败", e)
            emptySet()
        }
    }

    /**
     * 冻结指定配置文件的所有应用
     */
    suspend fun freezeAllForProfile(profileId: Int): Result<Int> {
        val apps = clonedApplicationRepository.getClonedAppsByProfile(profileId)
        var frozenCount = 0

        // 获取前台应用
        val foregroundApps = getForegroundApps()

        for (app in apps) {
            if (app.packageName !in listOf("com.appclonex")) {
                // 跳过前台应用
                if (app.packageName in foregroundApps) {
                    android.util.Log.i("AutoFreeze", "跳过前台应用: ${app.packageName}")
                    continue
                }

                val result = freezingService.freezeApp(app.packageName, profileId)
                if (result.isSuccess) frozenCount++
            }
        }

        return Result.success(frozenCount)
    }

    /**
     * 解冻指定配置文件的所有应用
     */
    suspend fun unfreezeAllForProfile(profileId: Int): Result<Int> {
        val apps = clonedApplicationRepository.getClonedAppsByProfile(profileId)
        var unfrozenCount = 0

        for (app in apps) {
            val result = freezingService.unfreezeApp(app.packageName, profileId)
            if (result.isSuccess) unfrozenCount++
        }

        return Result.success(unfrozenCount)
    }
}
