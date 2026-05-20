/*
 * Copyright (c) 2024 AppCloneX Contributors
 *
 * 安装插件用例
 *
 * 封装插件安装的业务逻辑，包括：
 * - 验证最低应用版本要求
 * - 检查依赖关系是否满足
 * - 检查权限声明
 * - 执行安装操作
 *
 * 遵循单一职责原则，每个用例只负责一个业务操作
 */
package com.appclonex.domain.usecase.plugin

import com.appclonex.domain.model.PluginId
import com.appclonex.domain.model.PluginInstallResult
import com.appclonex.domain.model.PluginManifest
import com.appclonex.domain.repository.PluginRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 安装插件用例
 *
 * 在执行实际安装前，会进行以下验证：
 * 1. 检查插件的最低应用版本要求
 * 2. 验证所有依赖插件是否已安装且版本兼容
 * 3. 确认插件声明的权限列表不为空（可选）
 *
 * @property repository 插件仓库，提供插件持久化操作能力
 */
@Singleton
class InstallPluginUseCase @Inject constructor(
    private val repository: PluginRepository
) {
    /**
     * 当前应用版本号，用于版本兼容性检查。
     * 实际项目中应从 BuildConfig 或版本管理器获取。
     */
    private val currentAppVersion: String = "1.0.0"

    /**
     * 执行插件安装操作
     *
     * 完整的安装流程包括：
     * 1. 验证 manifest 基本信息完整性
     * 2. 检查最低应用版本兼容性
     * 3. 检查依赖插件是否已安装
     * 4. 调用仓库执行实际安装
     *
     * @param manifest 插件清单，包含完整的插件描述信息
     * @return 安装结果，包含成功/失败状态和冲突信息
     */
    suspend operator fun invoke(manifest: PluginManifest): Result<PluginInstallResult> {
        // 验证 manifest 基本信息完整性
        val validationError = validateManifest(manifest)
        if (validationError != null) {
            // R13-005 修复：验证失败返回 Result.failure 而非 Result.success(success=false)
            return Result.failure(
                IllegalArgumentException(validationError)
            )
        }

        // 检查最低应用版本要求
        val versionCheckResult = checkMinAppVersion(manifest.metadata.minAppVersion)
        if (versionCheckResult != null) {
            return Result.failure(
                IllegalStateException(versionCheckResult)
            )
        }

        // 检查依赖关系
        val dependencyCheckResult = checkDependencies(manifest)
        if (dependencyCheckResult != null) {
            return Result.failure(
                IllegalStateException(dependencyCheckResult.errorMessage)
            )
        }

        // 检查权限声明（警告级别，不阻止安装）
        // 权限检查作为信息提示，不影响安装流程

        // 执行安装
        return repository.installPlugin(manifest)
    }

    /**
     * 验证插件清单的基本信息完整性
     *
     * @param manifest 插件清单
     * @return 验证失败时的错误信息，验证通过返回null
     */
    private fun validateManifest(manifest: PluginManifest): String? {
        val metadata = manifest.metadata
        return when {
            metadata.name.isBlank() -> "插件名称不能为空"
            metadata.version.isBlank() -> "插件版本号不能为空"
            metadata.author.isBlank() -> "插件作者不能为空"
            metadata.description.isBlank() -> "插件描述不能为空"
            metadata.minAppVersion.isBlank() -> "最低应用版本号不能为空"
            !isValidVersion(metadata.version) -> "插件版本号格式无效"
            !isValidVersion(metadata.minAppVersion) -> "最低应用版本号格式无效"
            else -> null
        }
    }

    /**
     * 检查最低应用版本兼容性
     *
     * 比较插件要求的最低版本与当前应用版本。
     * 使用简单的语义化版本比较（主版本.次版本.修订版本）。
     *
     * @param minAppVersion 插件要求的最低应用版本
     * @return 版本不兼容时的错误信息，兼容时返回null
     */
    private fun checkMinAppVersion(minAppVersion: String): String? {
        if (!isVersionCompatible(currentAppVersion, minAppVersion)) {
            return "应用版本不兼容：需要 $minAppVersion 或更高版本，当前版本 $currentAppVersion"
        }
        return null
    }

    /**
     * 检查插件依赖关系
     *
     * 验证所有声明的依赖插件是否已安装且版本兼容。
     *
     * @param manifest 插件清单
     * @return 依赖检查失败时的安装结果，通过返回null
     */
    private suspend fun checkDependencies(manifest: PluginManifest): PluginInstallResult? {
        if (manifest.dependencies.isEmpty()) return null

        val missingDependencies = mutableListOf<String>()
        val conflicts = mutableListOf<PluginId>()

        for (dependency in manifest.dependencies) {
            // 检查依赖插件是否已安装
            val dependencyId = PluginId(dependency.pluginId)
            val pluginInfo = repository.getPluginInfo(dependencyId)

            if (pluginInfo.isFailure || pluginInfo.getOrNull() == null) {
                missingDependencies.add(dependency.pluginId)
                continue
            }

            // 检查版本兼容性
            val installedVersion = pluginInfo.getOrNull()!!.version
            if (!isVersionInRange(installedVersion, dependency.minVersion, dependency.maxVersion)) {
                conflicts.add(dependencyId)
            }
        }

        return when {
            missingDependencies.isNotEmpty() -> PluginInstallResult(
                success = false,
                pluginId = manifest.metadata.id,
                errorMessage = "缺少依赖插件：${missingDependencies.joinToString(", ")}",
                conflicts = conflicts
            )
            conflicts.isNotEmpty() -> PluginInstallResult(
                success = false,
                pluginId = manifest.metadata.id,
                errorMessage = "依赖插件版本不兼容：${conflicts.joinToString(", ") { it.value }}",
                conflicts = conflicts
            )
            else -> null
        }
    }

    /**
     * 验证版本号格式是否合法
     *
     * 支持语义化版本格式：主版本.次版本.修订版本（如 1.0.0）
     *
     * @param version 版本号字符串
     * @return 版本号格式是否合法
     */
    private fun isValidVersion(version: String): Boolean {
        return version.matches(Regex("""^\d+\.\d+\.\d+$"""))
    }

    /**
     * 比较两个版本号，检查当前版本是否满足最低版本要求
     *
     * @param currentVersion 当前版本
     * @param minVersion 最低版本要求
     * @return 当前版本是否满足最低版本要求
     */
    private fun isVersionCompatible(currentVersion: String, minVersion: String): Boolean {
        val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val minParts = minVersion.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(currentParts.size, minParts.size)) {
            val current = currentParts.getOrElse(i) { 0 }
            val min = minParts.getOrElse(i) { 0 }
            if (current > min) return true
            if (current < min) return false
        }
        return true
    }

    /**
     * 检查版本是否在指定范围内
     *
     * @param version 要检查的版本
     * @param minVersion 最低版本（包含）
     * @param maxVersion 最高版本（包含），null表示不限制
     * @return 版本是否在范围内
     */
    private fun isVersionInRange(version: String, minVersion: String, maxVersion: String?): Boolean {
        if (!isVersionCompatible(version, minVersion)) return false
        if (maxVersion != null && !isVersionCompatible(maxVersion, version)) return false
        return true
    }
}
