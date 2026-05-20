package com.appclonex.infrastructure.external.packagemanager

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import com.appclonex.infrastructure.external.shizuku.ShizukuException
import com.appclonex.infrastructure.external.shizuku.ShizukuServiceConnectionProvider
import com.appclonex.domain.model.InstallableApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 包安装器服务
 * 负责在指定用户空间安装、卸载、启动应用
 */
@Singleton
class ApplicationPackageInstallerService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        /** 安装标志：替换已有安装 (PackageManager.INSTALL_REPLACE_EXISTING) */
        const val INSTALL_FLAG_REPLACE_EXISTING = 0x00000002

        /** 安装成功返回码 (PackageManager.INSTALL_SUCCEEDED) */
        const val INSTALL_RETURN_CODE_SUCCEEDED = 1

        /** 卸载成功返回码 (PackageManager.DELETE_SUCCEEDED) */
        const val DELETE_RETURN_CODE_SUCCEEDED = 1
    }

    private val packageManager: PackageManager by lazy {
        context.packageManager
    }

    /**
     * 获取所有可安装的应用列表
     * @param excludeSystem 是否排除系统应用
     */
    suspend fun getInstallableApps(excludeSystem: Boolean = false): Result<List<InstallableApp>> = 
        withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            
            val resolveInfoList: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
            
            val apps = resolveInfoList
                .mapNotNull { resolveInfo ->
                    val packageName = resolveInfo.activityInfo.packageName
                    
                    // 排除自身
                    if (packageName == context.packageName) return@mapNotNull null
                    
                    try {
                        val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            packageManager.getApplicationInfo(
                                packageName,
                                PackageManager.ApplicationInfoFlags.of(0)
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            packageManager.getApplicationInfo(packageName, 0)
                        }
                        
                        // 排除系统应用
                        if (excludeSystem && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                            return@mapNotNull null
                        }
                        
                        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            packageManager.getPackageInfo(
                                packageName,
                                PackageManager.PackageInfoFlags.of(0)
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            packageManager.getPackageInfo(packageName, 0)
                        }
                        
                        InstallableApp(
                            packageName = packageName,
                            appName = appInfo.loadLabel(packageManager).toString(),
                            appIcon = appInfo.loadIcon(packageManager),
                            versionName = packageInfo.versionName,
                            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                packageInfo.longVersionCode
                            } else {
                                @Suppress("DEPRECATION")
                                packageInfo.versionCode.toLong()
                            },
                            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                            sourceDir = appInfo.sourceDir
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase() }
            
            Result.success(apps)
        } catch (e: Exception) {
            Result.failure(ShizukuException("获取应用列表失败: ${e.message}"))
        }
    }

    /**
     * 在指定用户空间安装应用
     * @param packageName 应用包名
     * @param userId 目标用户ID
     */
    suspend fun installAppToUser(packageName: String, userId: Int): Result<Boolean> = 
        withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            val pm = ShizukuServiceConnectionProvider.getPackageManager()
                ?: throw ShizukuException("无法获取PackageManager服务")
            
            // 获取APK路径
            val appInfo = getAppInfo(packageName)
                ?: throw ShizukuException("找不到应用: $packageName")
            
            val apkPath = appInfo.sourceDir
            if (!File(apkPath).exists()) {
                throw ShizukuException("APK文件不存在")
            }
            
            // 使用Shizuku在目标用户安装应用，等待安装结果
            val installResult = installPackageInternal(pm, apkPath, userId)
            if (installResult.isFailure) {
                throw installResult.exceptionOrNull()
                    ?: ShizukuException("安装失败")
            }
            true
        }
    }

    /**
     * 从指定用户空间卸载应用
     * @param packageName 应用包名
     * @param userId 目标用户ID
     */
    suspend fun uninstallAppFromUser(packageName: String, userId: Int): Result<Boolean> = 
        withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            val pm = ShizukuServiceConnectionProvider.getPackageManager()
                ?: throw ShizukuException("无法获取PackageManager服务")
            
            // 使用Shizuku在目标用户卸载应用，等待卸载结果
            val uninstallResult = uninstallPackageInternal(pm, packageName, userId)
            if (uninstallResult.isFailure) {
                throw uninstallResult.exceptionOrNull()
                    ?: ShizukuException("卸载失败")
            }
            true
        }
    }

    /**
     * 在指定用户空间启动应用
     * @param packageName 应用包名
     * @param userId 目标用户ID
     */
    suspend fun launchAppInUser(packageName: String, userId: Int): Result<Boolean> = 
        withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            launchAppInternal(packageName, userId)
            true
        }
    }

    /**
     * 检查应用是否在指定用户空间已安装
     * @param packageName 应用包名
     * @param userId 用户ID
     */
    suspend fun isAppInstalledInUser(packageName: String, userId: Int): Result<Boolean> = 
        withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            val pm = ShizukuServiceConnectionProvider.getPackageManager()
                ?: throw ShizukuException("无法获取PackageManager服务")
            
            isPackageInstalledInternal(pm, packageName, userId)
        }
    }

    /**
     * 获取应用信息
     */
    private fun getAppInfo(packageName: String): ApplicationInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取应用图标
     */
    fun getAppIcon(packageName: String): Drawable? {
        return try {
            val appInfo = getAppInfo(packageName) ?: return null
            appInfo.loadIcon(packageManager)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取应用名称
     */
    fun getAppName(packageName: String): String? {
        return try {
            val appInfo = getAppInfo(packageName) ?: return null
            appInfo.loadLabel(packageManager).toString()
        } catch (e: Exception) {
            null
        }
    }

    // ==================== 内部方法（使用反射调用隐藏API）====================

    /**
     * 安装应用（反射调用，异步等待结果）
     */
    private suspend fun installPackageInternal(
        pm: android.content.pm.IPackageManager,
        apkPath: String,
        userId: Int
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        try {
            // 方法：installPackage(String originPath, IPackageInstallObserver observer, 
            //                     int flags, String installerPackageName, int userId)
            val method = pm.javaClass.getDeclaredMethod(
                "installPackage",
                String::class.java,
                android.content.pm.IPackageInstallObserver::class.java,
                Int::class.java,
                String::class.java,
                Int::class.java
            )
            
            // 创建安装观察者
            val observer = object : android.content.pm.IPackageInstallObserver.Stub() {
                override fun packageInstalled(name: String?, returnCode: Int) {
                    if (returnCode == INSTALL_RETURN_CODE_SUCCEEDED) {
                        continuation.resume(Result.success(Unit))
                    } else {
                        continuation.resume(Result.failure(
                            ShizukuException("安装失败，返回码: $returnCode")
                        ))
                    }
                }
            }
            
            continuation.invokeOnCancellation {
                // 协程被取消时的清理逻辑（观察者回调仍可能触发，但 continuation 已完成）
            }
            
            // 安装标志
            val flags = INSTALL_FLAG_REPLACE_EXISTING
            
            method.invoke(pm, apkPath, observer, flags, context.packageName, userId)
        } catch (e: Exception) {
            continuation.resume(Result.failure(ShizukuException("安装应用失败: ${e.message}")))
        }
    }

    /**
     * 卸载应用（反射调用，异步等待结果）
     */
    private suspend fun uninstallPackageInternal(
        pm: android.content.pm.IPackageManager,
        packageName: String,
        userId: Int
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        try {
            // 方法：deletePackage(String packageName, IPackageDeleteObserver observer, int userId)
            val method = pm.javaClass.getDeclaredMethod(
                "deletePackage",
                String::class.java,
                android.content.pm.IPackageDeleteObserver::class.java,
                Int::class.java
            )
            
            // 创建卸载观察者
            val observer = object : android.content.pm.IPackageDeleteObserver.Stub() {
                override fun packageDeleted(name: String?, returnCode: Int) {
                    if (returnCode == DELETE_RETURN_CODE_SUCCEEDED) {
                        continuation.resume(Result.success(Unit))
                    } else {
                        continuation.resume(Result.failure(
                            ShizukuException("卸载失败，返回码: $returnCode")
                        ))
                    }
                }
            }
            
            continuation.invokeOnCancellation {
                // 协程被取消时的清理逻辑
            }
            
            method.invoke(pm, packageName, observer, userId)
        } catch (e: Exception) {
            continuation.resume(Result.failure(ShizukuException("卸载应用失败: ${e.message}")))
        }
    }

    /**
     * 检查应用是否已安装（反射调用）
     */
    private fun isPackageInstalledInternal(
        pm: android.content.pm.IPackageManager,
        packageName: String,
        userId: Int
    ): Boolean {
        return try {
            val method = pm.javaClass.getDeclaredMethod(
                "getPackageInfo",
                String::class.java,
                Int::class.java,
                Int::class.java
            )
            
            val info = method.invoke(pm, packageName, 0, userId) as? PackageInfo
            info != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 启动应用（反射调用）
     */
    private fun launchAppInternal(packageName: String, userId: Int) {
        try {
            // 获取ActivityManager服务
            val amClass = Class.forName("android.app.ActivityManager")
            val getServiceMethod = amClass.getDeclaredMethod("getService")
            val amService = getServiceMethod.invoke(null)
            
            // 构建启动Intent
            val intent = packageManager.getLaunchIntentForPackage(packageName)
                ?: throw ShizukuException("无法获取启动Intent")
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // 使用ActivityTaskManager启动Activity
            val atmClass = Class.forName("android.app.IActivityTaskManager")
            val startActivityMethod = atmClass.getDeclaredMethod(
                "startActivityAsUser",
                String::class.java,
                String::class.java,
                Intent::class.java,
                String::class.java,
                android.os.Bundle::class.java,
                Int::class.java
            )
            
            startActivityMethod.invoke(
                amService,
                context.packageName,
                null,
                intent,
                null,
                null,
                userId
            )
        } catch (e: Exception) {
            throw ShizukuException("启动应用失败: ${e.message}")
        }
    }
}
