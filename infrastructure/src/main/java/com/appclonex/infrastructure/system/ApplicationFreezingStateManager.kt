package com.appclonex.infrastructure.system

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.RemoteException
import com.appclonex.domain.repository.FreezingService
import com.appclonex.infrastructure.external.shizuku.ShizukuException
import com.appclonex.infrastructure.external.shizuku.ShizukuServiceConnectionProvider
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.rikka.shizuku.ShizukuBinderWrapper
import dev.rikka.shizuku.SystemServiceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Method
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用冻结/挂起管理器
 *
 * 核心差异化功能：通过 Shizuku 反射调用系统级 API 实现应用冻结与解冻。
 * 冻结后的应用无法启动、不会在启动器中显示，但数据完整保留。
 * 可用于节省系统资源、防止后台应用自启动、临时禁用某些克隆应用。
 *
 * 实现 FreezingService 接口，供 Domain 层通过依赖注入使用。
 */
@Singleton
class ApplicationFreezingStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) : FreezingService {

    private val packageManager: PackageManager
        get() = context.packageManager

    companion object {
        private const val TAG = "ApplicationFreezingStateManager"

        // ApplicationInfo.enabledSetting 常量
        private const val COMPONENT_ENABLED_STATE_DEFAULT = 0
        private const val COMPONENT_ENABLED_STATE_DISABLED_USER = 3

        // PackageManager 隐藏 API 标志
        private const val MATCH_ANY_USER = 0x00400000
        private const val MATCH_KNOWN_PACKAGES = 0x00002000
    }

    // ==================== 公共 API ====================

    /**
     * 冻结指定用户的应用
     *
     * 通过 Shizuku 反射调用 ActivityManager.forceStopPackageAsUser() 强制停止应用，
     * 然后通过 IPackageManager.setApplicationEnabledSetting() 禁用应用。
     *
     * @param packageName 应用包名
     * @param userId 目标用户ID
     * @return 操作结果
     */
    override suspend fun freezeApp(packageName: String, userId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            // 第一步：强制停止应用
            forceStopPackage(packageName, userId)

            // 第二步：禁用应用（冻结核心操作）
            val ipm = ShizukuServiceConnectionProvider.getPackageManager()
                ?: throw ShizukuException("无法获取 PackageManager 服务")

            setApplicationEnabledSetting(ipm, packageName, userId)

            ApplicationLoggingServiceManager.logOperation(
                "FreezeApp",
                "packageName=$packageName, userId=$userId"
            )
        }
    }

    /**
     * 解冻指定用户的应用
     *
     * 通过 IPackageManager.setApplicationEnabledSetting() 重新启用应用。
     *
     * @param packageName 应用包名
     * @param userId 目标用户ID
     * @return 操作结果
     */
    override suspend fun unfreezeApp(packageName: String, userId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            val ipm = ShizukuServiceConnectionProvider.getPackageManager()
                ?: throw ShizukuException("无法获取 PackageManager 服务")

            enableApplication(ipm, packageName, userId)

            ApplicationLoggingServiceManager.logOperation(
                "UnfreezeApp",
                "packageName=$packageName, userId=$userId"
            )
        }
    }

    /**
     * 检查应用是否被冻结
     *
     * 通过 ApplicationInfo.enabledSetting 判断应用是否处于禁用状态。
     * COMPONENT_ENABLED_STATE_DISABLED_USER (3) 表示被用户手动禁用（冻结）。
     *
     * @param packageName 应用包名
     * @param userId 目标用户ID
     * @return true 表示已冻结
     */
    override suspend fun isAppFrozen(packageName: String, userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!ShizukuServiceConnectionProvider.isShizukuAvailable() || !ShizukuServiceConnectionProvider.hasPermission()) {
                return@withContext false
            }

            val ipm = ShizukuServiceConnectionProvider.getPackageManager()
                ?: return@withContext false

            val appInfo = getApplicationInfoForUser(ipm, packageName, userId)
                ?: return@withContext false

            // 检查 enabledSetting 是否为 DISABLED_USER
            appInfo.enabledSetting == COMPONENT_ENABLED_STATE_DISABLED_USER
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.w(TAG, "检查冻结状态失败: ${e.message}", e)
            false
        }
    }

    /**
     * 获取指定用户下所有被冻结的应用列表
     *
     * 遍历该用户下所有已安装应用，检查每个应用的 enabledSetting。
     *
     * @param userId 目标用户ID
     * @return 被冻结的应用包名列表
     */
    override suspend fun getFrozenApps(userId: Int): List<String> = withContext(Dispatchers.IO) {
        try {
            if (!ShizukuServiceConnectionProvider.isShizukuAvailable() || !ShizukuServiceConnectionProvider.hasPermission()) {
                return@withContext emptyList()
            }

            val ipm = ShizukuServiceConnectionProvider.getPackageManager()
                ?: return@withContext emptyList()

            val allPackages = getInstalledPackagesForUser(ipm, userId)
            val frozenPackages = mutableListOf<String>()

            for (packageName in allPackages) {
                try {
                    val appInfo = getApplicationInfoForUser(ipm, packageName, userId)
                    if (appInfo != null &&
                        appInfo.enabledSetting == COMPONENT_ENABLED_STATE_DISABLED_USER
                    ) {
                        frozenPackages.add(packageName)
                    }
                } catch (e: Exception) {
                    ApplicationLoggingServiceManager.w(
                        "FreezingState", 
                        "无法访问应用 $packageName 的冻结状态", 
                        e
                    )
                }
            }

            ApplicationLoggingServiceManager.d(TAG, "用户 $userId 下冻结应用数: ${frozenPackages.size}")
            frozenPackages
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.w(TAG, "获取冻结应用列表失败: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 批量冻结指定用户的所有克隆应用
     *
     * @param userId 目标用户ID
     * @return 成功冻结的应用数量
     */
    override suspend fun freezeAllApps(userId: Int): Result<Int> = withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            val ipm = ShizukuServiceConnectionProvider.getPackageManager()
                ?: throw ShizukuException("无法获取 PackageManager 服务")

            val allPackages = getInstalledPackagesForUser(ipm, userId)
            var frozenCount = 0

            for (packageName in allPackages) {
                try {
                    val appInfo = getApplicationInfoForUser(ipm, packageName, userId)

                    // 跳过系统关键应用
                    if (isSystemCriticalApp(packageName, appInfo)) continue

                    if (appInfo != null &&
                        appInfo.enabledSetting != COMPONENT_ENABLED_STATE_DISABLED_USER
                    ) {
                        forceStopPackage(packageName, userId)
                        setApplicationEnabledSetting(ipm, packageName, userId)
                        frozenCount++
                    }
                } catch (e: Exception) {
                    ApplicationLoggingServiceManager.w(
                        "FreezingState", 
                        "冻结应用 $packageName 失败", 
                        e
                    )
                }
            }

            ApplicationLoggingServiceManager.logOperation(
                "FreezeAllApps",
                "userId=$userId, frozenCount=$frozenCount"
            )

            frozenCount
        }
    }

    /**
     * 批量解冻指定用户的所有克隆应用
     *
     * @param userId 目标用户ID
     * @return 成功解冻的应用数量
     */
    override suspend fun unfreezeAllApps(userId: Int): Result<Int> = withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            val ipm = ShizukuServiceConnectionProvider.getPackageManager()
                ?: throw ShizukuException("无法获取 PackageManager 服务")

            val allPackages = getInstalledPackagesForUser(ipm, userId)
            var unfrozenCount = 0

            for (packageName in allPackages) {
                try {
                    val appInfo = getApplicationInfoForUser(ipm, packageName, userId)
                    if (appInfo != null &&
                        appInfo.enabledSetting == COMPONENT_ENABLED_STATE_DISABLED_USER
                    ) {
                        enableApplication(ipm, packageName, userId)
                        unfrozenCount++
                    }
                } catch (e: Exception) {
                    ApplicationLoggingServiceManager.w(
                        "FreezingState", 
                        "解冻应用 $packageName 失败", 
                        e
                    )
                }
            }

            ApplicationLoggingServiceManager.logOperation(
                "UnfreezeAllApps",
                "userId=$userId, unfrozenCount=$unfrozenCount"
            )

            unfrozenCount
        }
    }

    // ==================== 内部方法（反射调用隐藏 API）====================

    /**
     * 通过 Shizuku 获取 IActivityManager 并强制停止应用
     */
    private fun forceStopPackage(packageName: String, userId: Int) {
        try {
            val binder = ShizukuBinderWrapper(
                SystemServiceHelper.getSystemService("activity")
            )
            val amInterface = Class.forName("android.app.IActivityManager")
                .interfaces.firstOrNull()
                ?: throw ShizukuException("无法获取 IActivityManager 接口")

            val am = amInterface.getMethod("asInterface", android.os.IBinder::class.java)
                .invoke(null, binder)
                ?: throw ShizukuException("无法获取 IActivityManager 实例")

            // 调用 forceStopPackageAsUser(packageName, userId)
            val method: Method = am.javaClass.getDeclaredMethod(
                "forceStopPackageAsUser",
                String::class.java,
                Int::class.javaPrimitiveType
            )
            method.invoke(am, packageName, userId)

            ApplicationLoggingServiceManager.d(TAG, "已强制停止: $packageName (user: $userId)")
        } catch (e: RemoteException) {
            throw ShizukuException("远程调用 forceStopPackageAsUser 失败: ${e.message}")
        } catch (e: ShizukuException) {
            throw e
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.w(TAG, "forceStopPackage 反射调用失败: ${e.message}", e)
            // forceStop 失败不阻断冻结流程
        }
    }

    /**
     * 禁用应用（冻结）
     * 使用 IPackageManager.setApplicationEnabledSetting()
     */
    private fun setApplicationEnabledSetting(
        ipm: android.content.pm.IPackageManager,
        packageName: String,
        userId: Int
    ) {
        try {
            val method: Method = ipm.javaClass.getDeclaredMethod(
                "setApplicationEnabledSetting",
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            // newState = COMPONENT_ENABLED_STATE_DISABLED_USER (3)
            // flags = 0 (SYNCHRONOUS 不需要，使用默认异步)
            // userId = 目标用户
            // callingPackage = 本应用包名
            method.invoke(
                ipm,
                packageName,
                COMPONENT_ENABLED_STATE_DISABLED_USER,
                0,
                userId,
                context.packageName
            )

            ApplicationLoggingServiceManager.d(TAG, "已冻结: $packageName (user: $userId)")
        } catch (e: RemoteException) {
            throw ShizukuException("远程调用 setApplicationEnabledSetting 失败: ${e.message}")
        } catch (e: Exception) {
            throw ShizukuException("冻结应用失败: ${e.message}")
        }
    }

    /**
     * 启用应用（解冻）
     * 使用 IPackageManager.setApplicationEnabledSetting()
     */
    private fun enableApplication(
        ipm: android.content.pm.IPackageManager,
        packageName: String,
        userId: Int
    ) {
        try {
            val method: Method = ipm.javaClass.getDeclaredMethod(
                "setApplicationEnabledSetting",
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            // newState = COMPONENT_ENABLED_STATE_DEFAULT (0)
            method.invoke(
                ipm,
                packageName,
                COMPONENT_ENABLED_STATE_DEFAULT,
                0,
                userId,
                context.packageName
            )

            ApplicationLoggingServiceManager.d(TAG, "已解冻: $packageName (user: $userId)")
        } catch (e: RemoteException) {
            throw ShizukuException("远程调用 setApplicationEnabledSetting 失败: ${e.message}")
        } catch (e: Exception) {
            throw ShizukuException("解冻应用失败: ${e.message}")
        }
    }

    /**
     * 获取指定用户下的 ApplicationInfo
     */
    private fun getApplicationInfoForUser(
        ipm: android.content.pm.IPackageManager,
        packageName: String,
        userId: Int
    ): ApplicationInfo? {
        return try {
            val method: Method = ipm.javaClass.getDeclaredMethod(
                "getApplicationInfo",
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            // flags = 0 (MATCH_ANY_USER 由 userId 参数处理)
            method.invoke(ipm, packageName, 0, userId) as? ApplicationInfo
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.w(TAG, "获取 ApplicationInfo 失败: $packageName, ${e.message}")
            null
        }
    }

    /**
     * 获取指定用户下所有已安装应用的包名列表
     */
    private fun getInstalledPackagesForUser(
        ipm: android.content.pm.IPackageManager,
        userId: Int
    ): List<String> {
        return try {
            val method: Method = ipm.javaClass.getDeclaredMethod(
                "getInstalledApplications",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            // flags = 0, userId
            @Suppress("UNCHECKED_CAST")
            val apps = method.invoke(ipm, 0, userId) as? List<ApplicationInfo>
            apps?.map { it.packageName } ?: emptyList()
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.w(TAG, "获取已安装应用列表失败: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 判断是否为系统关键应用（不应被冻结）
     *
     * 通过以下策略判断：
     * 1. 硬编码前缀列表匹配已知关键系统应用
     * 2. PackageManager FLAG_SYSTEM 标志动态检测所有系统应用
     * 3. 检查是否为系统包本身（"android"）
     */
    private fun isSystemCriticalApp(packageName: String, appInfo: ApplicationInfo? = null): Boolean {
        val criticalPrefixes = listOf(
            "android",
            "com.android.systemui",
            "com.android.launcher",
            "com.android.settings",
            "com.android.phone",
            "com.android.providers",
            "com.android.vending",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.google.android.setupwizard"
        )

        // 1. 检查硬编码前缀列表
        if (criticalPrefixes.any { packageName.startsWith(it) }) return true

        // 2. 检查 FLAG_SYSTEM（动态检测所有系统应用）
        if (appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) return true

        // 3. 检查是否为系统包本身
        if (packageName == "android") return true

        return false
    }
}
