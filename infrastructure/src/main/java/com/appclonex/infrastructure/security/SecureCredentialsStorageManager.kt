package com.appclonex.infrastructure.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 安全凭据存储管理器
 *
 * R12-001 修复：使用 EncryptedSharedPreferences 安全存储敏感凭据
 *
 * 用于安全存储以下敏感数据：
 * - WebDAV 密码
 * - Nextcloud 密码和应用专用密码
 * - 其他需要加密存储的凭据
 *
 * ## 安全性说明
 * - 使用 Android Keystore 保护主密钥
 * - 密钥使用 AES256_GCM 加密
 * - 值使用 AES256_GCM 加密
 * - 包含完整的异常处理和降级机制
 *
 * ## 使用示例
 * ```kotlin
 * // 存储密码
 * secureCredentialsStorage.saveWebDavPassword(serverUrl, username, password)
 *
 * // 获取密码
 * val password = secureCredentialsStorage.getWebDavPassword(serverUrl, username)
 *
 * // 删除密码
 * secureCredentialsStorage.deleteWebDavPassword(serverUrl, username)
 * ```
 */
@Singleton
class SecureCredentialsStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SecureCredentials"
        private const val PREFS_FILE_NAME = "secure_credentials"

        // 键名前缀
        private const val PREFIX_WEBDAV_PASSWORD = "webdav_pwd_"
        private const val PREFIX_NEXTCLOUD_PASSWORD = "nc_pwd_"
        private const val PREFIX_NEXTCLOUD_APP_PASSWORD = "nc_app_pwd_"
        private const val PREFIX_GENERIC = "cred_"

        // 错误状态
        sealed class StorageState {
            object Ready : StorageState()
            object Degraded : StorageState()
            data class Error(val message: String) : StorageState()
        }
    }

    private val _storageState = MutableStateFlow<StorageState>(StorageState.Ready)
    val storageState: Flow<StorageState> = _storageState.asStateFlow()

    private var encryptedPrefs: EncryptedSharedPreferences? = null
    private var isInitialized = false
    private var initializationError: Throwable? = null

    init {
        initializeEncryptedStorage()
    }

    /**
     * 初始化加密存储
     *
     * R12-005 修复：添加完整的异常处理
     * - 捕获 MasterKey 创建失败
     * - 捕获 EncryptedSharedPreferences 创建失败
     * - 提供降级模式和错误状态
     */
    private fun initializeEncryptedStorage() {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setKeyGenParameterSpec(
                    KeyGenParameterSpec.Builder(
                        MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
                .build()

            @Suppress("UNCHECKED_CAST")
            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences

            isInitialized = true
            _storageState.value = StorageState.Ready
            ApplicationLoggingServiceManager.i(TAG, "安全凭据存储初始化成功")

        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "安全凭据存储初始化失败", e)
            initializationError = e
            isInitialized = false
            _storageState.value = StorageState.Error(e.message ?: "初始化失败")

            // 尝试恢复：清除损坏的存储并重新创建
            attemptRecovery()
        }
    }

    /**
     * 尝试恢复损坏的加密存储
     *
     * R14-003 修复：真正删除损坏的加密文件
     * - 之前仅获取 SharedPreferences 引用但未删除
     * - 现在使用 deleteSharedPreferences 彻底清除损坏文件
     */
    private fun attemptRecovery() {
        try {
            ApplicationLoggingServiceManager.w(TAG, "尝试恢复加密存储...")

            // R14-003 修复：真正删除损坏的加密存储文件
            context.deleteSharedPreferences(PREFS_FILE_NAME)
            ApplicationLoggingServiceManager.w(TAG, "已清除损坏的加密存储文件，用户需要重新配置凭据")

            // 重新初始化
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            @Suppress("UNCHECKED_CAST")
            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences

            isInitialized = true
            _storageState.value = StorageState.Degraded
            ApplicationLoggingServiceManager.i(TAG, "加密存储恢复成功（降级模式）")

        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "加密存储恢复失败", e)
            _storageState.value = StorageState.Error("无法初始化安全存储: ${e.message}")
        }
    }

    // ==================== WebDAV 密码管理 ====================

    /**
     * 保存 WebDAV 密码
     *
     * @param serverUrl 服务器 URL
     * @param username 用户名
     * @param password 密码
     * @return Result.success 如果保存成功
     */
    suspend fun saveWebDavPassword(serverUrl: String, username: String, password: String): Result<Unit> {
        return saveCredential(PREFIX_WEBDAV_PASSWORD, "$serverUrl:$username", password)
    }

    /**
     * 获取 WebDAV 密码
     *
     * @param serverUrl 服务器 URL
     * @param username 用户名
     * @return 密码，如果不存在则返回 null
     */
    suspend fun getWebDavPassword(serverUrl: String, username: String): String? {
        return getCredential(PREFIX_WEBDAV_PASSWORD, "$serverUrl:$username")
    }

    /**
     * 删除 WebDAV 密码
     */
    suspend fun deleteWebDavPassword(serverUrl: String, username: String): Result<Unit> {
        return deleteCredential(PREFIX_WEBDAV_PASSWORD, "$serverUrl:$username")
    }

    // ==================== Nextcloud 密码管理 ====================

    /**
     * 保存 Nextcloud 用户密码
     */
    suspend fun saveNextcloudPassword(serverUrl: String, username: String, password: String): Result<Unit> {
        return saveCredential(PREFIX_NEXTCLOUD_PASSWORD, "$serverUrl:$username", password)
    }

    /**
     * 获取 Nextcloud 用户密码
     */
    suspend fun getNextcloudPassword(serverUrl: String, username: String): String? {
        return getCredential(PREFIX_NEXTCLOUD_PASSWORD, "$serverUrl:$username")
    }

    /**
     * 保存 Nextcloud 应用专用密码
     */
    suspend fun saveNextcloudAppPassword(serverUrl: String, username: String, appPassword: String): Result<Unit> {
        return saveCredential(PREFIX_NEXTCLOUD_APP_PASSWORD, "$serverUrl:$username", appPassword)
    }

    /**
     * 获取 Nextcloud 应用专用密码
     */
    suspend fun getNextcloudAppPassword(serverUrl: String, username: String): String? {
        return getCredential(PREFIX_NEXTCLOUD_APP_PASSWORD, "$serverUrl:$username")
    }

    /**
     * 删除所有 Nextcloud 凭据
     */
    suspend fun deleteNextcloudCredentials(serverUrl: String, username: String): Result<Unit> {
        return try {
            val keyBase = "$serverUrl:$username"
            deleteCredential(PREFIX_NEXTCLOUD_PASSWORD, keyBase)
            deleteCredential(PREFIX_NEXTCLOUD_APP_PASSWORD, keyBase)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 通用凭据管理 ====================

    /**
     * 保存通用凭据
     */
    suspend fun saveGenericCredential(key: String, value: String): Result<Unit> {
        return saveCredential(PREFIX_GENERIC, key, value)
    }

    /**
     * 获取通用凭据
     */
    suspend fun getGenericCredential(key: String): String? {
        return getCredential(PREFIX_GENERIC, key)
    }

    /**
     * 删除通用凭据
     */
    suspend fun deleteGenericCredential(key: String): Result<Unit> {
        return deleteCredential(PREFIX_GENERIC, key)
    }

    // ==================== 内部方法 ====================

    private suspend fun saveCredential(prefix: String, key: String, value: String): Result<Unit> {
        if (!isInitialized) {
            return Result.failure(
                SecurityException("安全存储未初始化: ${initializationError?.message}")
            )
        }

        return try {
            val storageKey = buildStorageKey(prefix, key)
            encryptedPrefs?.edit()?.putString(storageKey, value)?.apply()
            ApplicationLoggingServiceManager.d(TAG, "凭据已保存: $prefix${hashKey(key)}")
            Result.success(Unit)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "保存凭据失败: $prefix", e)
            Result.failure(e)
        }
    }

    private suspend fun getCredential(prefix: String, key: String): String? {
        if (!isInitialized) {
            ApplicationLoggingServiceManager.w(TAG, "安全存储未初始化，无法获取凭据")
            return null
        }

        return try {
            val storageKey = buildStorageKey(prefix, key)
            encryptedPrefs?.getString(storageKey, null)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "获取凭据失败: $prefix", e)
            null
        }
    }

    private suspend fun deleteCredential(prefix: String, key: String): Result<Unit> {
        if (!isInitialized) {
            return Result.failure(
                SecurityException("安全存储未初始化")
            )
        }

        return try {
            val storageKey = buildStorageKey(prefix, key)
            encryptedPrefs?.edit()?.remove(storageKey)?.apply()
            ApplicationLoggingServiceManager.d(TAG, "凭据已删除: $prefix${hashKey(key)}")
            Result.success(Unit)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "删除凭据失败: $prefix", e)
            Result.failure(e)
        }
    }

    /**
     * 构建存储键名
     * 对原始键进行哈希处理，避免敏感信息泄露
     */
    private fun buildStorageKey(prefix: String, key: String): String {
        return prefix + hashKey(key)
    }

    /**
     * 对键进行哈希处理
     * 使用 SHA-256 哈希，避免在日志或调试中泄露服务器地址等信息
     */
    private fun hashKey(key: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }

    /**
     * 检查存储是否可用
     */
    fun isAvailable(): Boolean = isInitialized && encryptedPrefs != null

    /**
     * 获取存储状态
     */
    fun getState(): StorageState = _storageState.value

    /**
     * 清除所有凭据（用于登出或安全清理）
     */
    suspend fun clearAllCredentials(): Result<Unit> {
        if (!isInitialized) {
            return Result.failure(SecurityException("安全存储未初始化"))
        }

        return try {
            encryptedPrefs?.edit()?.clear()?.apply()
            ApplicationLoggingServiceManager.i(TAG, "所有凭据已清除")
            Result.success(Unit)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "清除凭据失败", e)
            Result.failure(e)
        }
    }
}
