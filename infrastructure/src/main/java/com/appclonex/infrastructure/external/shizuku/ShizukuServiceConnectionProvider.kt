package com.appclonex.infrastructure.external.shizuku

import android.content.pm.IPackageManager
import android.os.IUserManager
import android.os.RemoteException
import dev.rikka.shizuku.Shizuku
import dev.rikka.shizuku.ShizukuBinderWrapper
import dev.rikka.shizuku.SystemServiceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

/**
 * Shizuku服务连接提供者。
 *
 * 该单例对象负责与Shizuku服务通信，获取系统级API访问权限。
 * Shizuku是一个允许应用以ADB权限运行的工具，无需root即可访问系统API。
 *
 * ## 核心功能
 * - Shizuku服务连接管理
 * - 权限请求和处理
 * - 系统服务（UserManager、PackageManager）的获取和缓存
 * - 安全的操作执行包装（带超时机制）
 *
 * ## 内存安全
 * - 所有回调使用WeakReference包装，避免Activity/Fragment泄漏
 * - 应用退出时调用 [destroy] 清理资源
 *
 * ## 使用示例
 * ```kotlin
 * // 在Application中初始化
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         ShizukuServiceConnectionProvider.init()
 *
 *         // 设置回调（使用弱引用，无需手动清理）
 *         ShizukuServiceConnectionProvider.setOnBinderReceivedCallback {
 *             Log.d("Shizuku", "服务已连接")
 *         }
 *         ShizukuServiceConnectionProvider.setOnPermissionResultCallback { granted ->
 *             if (granted) startMain() else showPermissionDenied()
 *         }
 *     }
 * }
 * ```
 *
 * @see ShizukuException
 * @see ShizukuState
 */
object ShizukuServiceConnectionProvider {

    private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001

    // R11-007: 默认超时时间配置
    const val DEFAULT_OPERATION_TIMEOUT_MS = 30000L // 30秒
    const val LONG_OPERATION_TIMEOUT_MS = 120000L   // 2分钟（安装/卸载等长操作）
    const val SHORT_OPERATION_TIMEOUT_MS = 10000L   // 10秒（查询类操作）

    // 缓存的系统服务
    private val cachedUserManager = AtomicReference<IUserManager?>(null)
    private val cachedPackageManager = AtomicReference<IPackageManager?>(null)
    private val userManagerLock = Any()
    private val packageManagerLock = Any()
    
    // 使用WeakReference包装回调，防止内存泄漏
    private var onPermissionResultRef = AtomicReference<WeakReference<((Boolean) -> Unit)>?>(null)
    private var onBinderReceivedRef = AtomicReference<WeakReference<(() -> Unit)>?>(null)
    private var onBinderDeadRef = AtomicReference<WeakReference<(() -> Unit)>?>(null)

    // Shizuku权限监听器
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            onPermissionResultRef.get()?.get()?.invoke(grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED)
        }
    }

    // Shizuku绑定状态监听器
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        onBinderReceivedRef.get()?.get()?.invoke()
        checkAndRequestPermission()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        onBinderDeadRef.get()?.get()?.invoke()
        clearCachedServices()
    }

    /**
     * 权限请求结果回调。
     *
     * 当用户响应Shizuku权限请求时触发。
     * 参数为true表示用户授权，false表示拒绝。
     * 
     * @deprecated 使用 [setOnPermissionResultCallback] 替代，避免内存泄漏
     */
    @Deprecated(
        "使用 setOnPermissionResultCallback 替代，避免内存泄漏",
        ReplaceWith("setOnPermissionResultCallback(callback)")
    )
    var onPermissionResult: ((Boolean) -> Unit)?
        get() = onPermissionResultRef.get()?.get()
        set(value) {
            onPermissionResultRef.set(value?.let { WeakReference(it) })
        }

    /**
     * Shizuku服务绑定成功回调。
     *
     * 当Shizuku服务成功绑定到应用时触发。
     * 通常在此回调中更新UI状态。
     * 
     * @deprecated 使用 [setOnBinderReceivedCallback] 替代，避免内存泄漏
     */
    @Deprecated(
        "使用 setOnBinderReceivedCallback 替代，避免内存泄漏",
        ReplaceWith("setOnBinderReceivedCallback(callback)")
    )
    var onBinderReceived: (() -> Unit)?
        get() = onBinderReceivedRef.get()?.get()
        set(value) {
            onBinderReceivedRef.set(value?.let { WeakReference(it) })
        }

    /**
     * Shizuku服务断开回调。
     *
     * 当Shizuku服务意外断开或停止时触发。
     * 应在此回调中清理状态并提示用户重新启动Shizuku。
     * 
     * @deprecated 使用 [setOnBinderDeadCallback] 替代，避免内存泄漏
     */
    @Deprecated(
        "使用 setOnBinderDeadCallback 替代，避免内存泄漏",
        ReplaceWith("setOnBinderDeadCallback(callback)")
    )
    var onBinderDead: (() -> Unit)?
        get() = onBinderDeadRef.get()?.get()
        set(value) {
            onBinderDeadRef.set(value?.let { WeakReference(it) })
        }
    
    /**
     * 设置权限请求结果回调（内存安全版本）。
     * 
     * 使用WeakReference包装回调，避免Activity/Fragment泄漏。
     * 当回调对象被GC回收时，会自动失效。
     *
     * @param callback 权限结果回调，参数为true表示授权，false表示拒绝
     */
    fun setOnPermissionResultCallback(callback: ((Boolean) -> Unit)?) {
        onPermissionResultRef.set(callback?.let { WeakReference(it) })
    }
    
    /**
     * 设置服务绑定成功回调（内存安全版本）。
     *
     * @param callback 绑定成功回调
     */
    fun setOnBinderReceivedCallback(callback: (() -> Unit)?) {
        onBinderReceivedRef.set(callback?.let { WeakReference(it) })
    }
    
    /**
     * 设置服务断开回调（内存安全版本）。
     *
     * @param callback 服务断开回调
     */
    fun setOnBinderDeadCallback(callback: (() -> Unit)?) {
        onBinderDeadRef.set(callback?.let { WeakReference(it) })
    }

    /**
     * 初始化Shizuku服务连接。
     *
     * 注册必要的监听器以接收Shizuku服务状态变化通知。
     * 应在Application的onCreate()中调用。
     *
     * ## 示例
     * ```kotlin
     * class MyApp : Application() {
     *     override fun onCreate() {
     *         super.onCreate()
     *         ShizukuServiceConnectionProvider.init()
     *     }
     * }
     * ```
     */
    fun init() {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 销毁Shizuku服务连接。
     *
     * 移除所有注册的监听器并清理缓存的服务。
     * 应在应用退出或不再需要Shizuku时调用。
     */
    fun destroy() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        clearCachedServices()
    }

    /**
     * 检查Shizuku服务是否可用。
     *
     * 通过ping Shizuku的binder来检测服务是否正在运行。
     *
     * @return true表示Shizuku服务正在运行，false表示未运行或未安装
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查是否已获得Shizuku权限授权。
     *
     * 检查当前应用是否已被用户授权使用Shizuku。
     * 对于Shizuku v11之前的版本，此方法始终返回false。
     *
     * @return true表示已授权，false表示未授权或Shizuku版本过低
     */
    fun hasPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) {
                false
            } else {
                Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 请求Shizuku权限。
     *
     * 向用户发起权限请求，结果通过 [onPermissionResult] 回调返回。
     * 如果已授权，则直接调用回调并返回true。
     * 对于Shizuku v11之前的版本，直接回调false。
     */
    fun requestPermission() {
        try {
            if (Shizuku.isPreV11()) {
                // 旧版本Shizuku，不支持运行时权限
                onPermissionResult?.invoke(false)
            } else {
                if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                } else {
                    onPermissionResult?.invoke(true)
                }
            }
        } catch (e: Exception) {
            onPermissionResult?.invoke(false)
        }
    }

    /**
     * 检查并请求权限。
     *
     * 如果当前没有权限，则发起权限请求。
     * 这是一个内部方法，在binder连接成功时自动调用。
     */
    private fun checkAndRequestPermission() {
        if (!hasPermission()) {
            requestPermission()
        }
    }

    /**
     * 获取Android系统UserManager服务。
     *
     * 通过Shizuku获取系统级的IUserManager接口，
     * 可以执行用户管理相关操作（创建/删除用户等）。
     *
     * 服务会被缓存，后续调用直接返回缓存实例。
     * 当Shizuku服务断开时，缓存会自动清除。
     *
     * 使用双重检查锁定模式确保线程安全。
     *
     * @return IUserManager接口实例，如果Shizuku不可用或未授权则返回null
     *
     * @see IUserManager
     */
    fun getUserManager(): IUserManager? {
        // Fast path: check cache
        cachedUserManager.get()?.let { return it }

        // Slow path: create with synchronization
        synchronized(userManagerLock) {
            // Double-check after acquiring lock
            cachedUserManager.get()?.let { return it }

            val userManager = createUserManager()
            cachedUserManager.set(userManager)
            return userManager
        }
    }

    /**
     * 创建UserManager实例。
     *
     * 内部方法，负责实际的UserManager创建逻辑。
     */
    private fun createUserManager(): IUserManager? {
        return try {
            if (!isShizukuAvailable() || !hasPermission()) {
                return null
            }

            val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("user"))
            IUserManager.Stub.asInterface(binder)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e("ShizukuService", "Failed to create UserManager", e)
            null
        }
    }

    /**
     * 获取Android系统PackageManager服务。
     *
     * 通过Shizuku获取系统级的IPackageManager接口，
     * 可以执行包管理相关操作（安装/卸载应用等）。
     *
     * 服务会被缓存，后续调用直接返回缓存实例。
     * 当Shizuku服务断开时，缓存会自动清除。
     *
     * 使用双重检查锁定模式确保线程安全。
     *
     * @return IPackageManager接口实例，如果Shizuku不可用或未授权则返回null
     *
     * @see IPackageManager
     */
    fun getPackageManager(): IPackageManager? {
        // Fast path: check cache
        cachedPackageManager.get()?.let { return it }

        // Slow path: create with synchronization
        synchronized(packageManagerLock) {
            // Double-check after acquiring lock
            cachedPackageManager.get()?.let { return it }

            val packageManager = createPackageManager()
            cachedPackageManager.set(packageManager)
            return packageManager
        }
    }

    /**
     * 创建PackageManager实例。
     *
     * 内部方法，负责实际的PackageManager创建逻辑。
     */
    private fun createPackageManager(): IPackageManager? {
        return try {
            if (!isShizukuAvailable() || !hasPermission()) {
                return null
            }

            val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
            IPackageManager.Stub.asInterface(binder)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e("ShizukuService", "Failed to create PackageManager", e)
            null
        }
    }

    /**
     * 清除缓存的服务实例。
     *
     * 当Shizuku服务断开时自动调用，清除所有缓存的系统服务。
     */
    private fun clearCachedServices() {
        cachedUserManager.set(null)
        cachedPackageManager.set(null)
    }

    /**
     * 在Shizuku权限下安全执行操作（带超时）。
     *
     * R11-007 修复：添加超时机制，防止Shizuku调用无限挂起
     *
     * 提供一个安全的执行环境，自动处理Shizuku可用性检查和异常。
     * 操作在IO线程中执行，适合用于协程环境。
     *
     * ## 示例
     * ```kotlin
     * lifecycleScope.launch {
     *     val result = ShizukuServiceConnectionProvider.executeWithShizuku {
     *         val userManager = getUserManager()
     *         userManager?.createUser("新用户", 0)
     *     }
     *
     *     result.onSuccess { userInfo ->
     *         Log.d("User", "创建成功: ${userInfo?.name}")
     *     }
     *     result.onFailure { error ->
     *         Log.e("User", "创建失败: ${error.message}")
     *     }
     * }
     * ```
     *
     * @param T 操作返回类型
     * @param timeoutMs 超时时间（毫秒），默认30秒
     * @param block 要执行的操作代码块
     * @return Result包装的操作结果，成功时包含返回值，失败时包含 [ShizukuException]
     *
     * @throws ShizukuException 当Shizuku不可用或权限未授予时
     */
    suspend fun <T> executeWithShizuku(
        timeoutMs: Long = DEFAULT_OPERATION_TIMEOUT_MS,
        block: () -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            if (!isShizukuAvailable()) {
                return@withContext Result.failure(ShizukuException("Shizuku服务不可用，请先安装并启动Shizuku"))
            }

            if (!hasPermission()) {
                return@withContext Result.failure(ShizukuException("未获得Shizuku权限授权"))
            }

            // R11-007: 添加超时保护
            val result = withTimeout(timeoutMs) {
                block()
            }
            Result.success(result)
        } catch (e: TimeoutCancellationException) {
            ApplicationLoggingServiceManager.e("ShizukuService", "操作超时 (${timeoutMs}ms)", e)
            Result.failure(ShizukuException("操作超时，请检查Shizuku服务状态后重试"))
        } catch (e: RemoteException) {
            ApplicationLoggingServiceManager.e("ShizukuService", "远程服务调用失败", e)
            Result.failure(ShizukuException("远程服务调用失败: ${e.message}"))
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e("ShizukuService", "操作失败", e)
            Result.failure(ShizukuException("操作失败: ${e.message}"))
        }
    }

    /**
     * 在Shizuku权限下安全执行操作（短超时，用于查询类操作）。
     *
     * R11-007: 针对查询类操作使用更短的超时时间
     */
    suspend fun <T> executeWithShizukuShortTimeout(block: () -> T): Result<T> {
        return executeWithShizuku(SHORT_OPERATION_TIMEOUT_MS, block)
    }

    /**
     * 在Shizuku权限下安全执行操作（长超时，用于安装/卸载等耗时操作）。
     *
     * R11-007: 针对耗时操作使用更长的超时时间
     */
    suspend fun <T> executeWithShizukuLongTimeout(block: () -> T): Result<T> {
        return executeWithShizuku(LONG_OPERATION_TIMEOUT_MS, block)
    }
}

/**
 * Shizuku异常类。
 *
 * 封装Shizuku操作过程中可能发生的各种错误。
 *
 * @property message 错误描述信息
 */
class ShizukuException(message: String) : Exception(message)

/**
 * Shizuku服务状态枚举。
 *
 * 定义Shizuku服务可能处于的各种状态，用于UI展示和状态判断。
 *
 * ## 状态说明
 * - [NOT_INSTALLED]: Shizuku应用未安装
 * - [NOT_RUNNING]: Shizuku已安装但服务未启动
 * - [PERMISSION_DENIED]: 用户拒绝了权限请求
 * - [READY]: Shizuku服务就绪，可以正常使用
 * - [ERROR]: 发生未知错误
 *
 * @property NOT_INSTALLED 未安装Shizuku应用
 * @property NOT_RUNNING Shizuku应用已安装但服务未运行
 * @property PERMISSION_DENIED 权限被用户拒绝
 * @property READY 服务就绪，可以正常使用
 * @property ERROR 发生错误
 */
enum class ShizukuState {
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_DENIED,
    READY,
    ERROR
}
