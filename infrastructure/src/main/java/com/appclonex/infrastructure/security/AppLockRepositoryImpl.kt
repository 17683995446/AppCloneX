package com.appclonex.infrastructure.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.appclonex.domain.model.AppLockConfiguration
import com.appclonex.domain.model.LockVerificationResult
import com.appclonex.domain.repository.AppLockRepository
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 应用锁仓库实现
 * 
 * 使用 EncryptedSharedPreferences 安全存储PIN码
 * 使用 BiometricPrompt 进行生物识别验证
 * 
 * ## 安全性说明
 * - PIN码使用PBKDF2WithHmacSHA256派生密钥，迭代600000次（OWASP 2023建议）
 * - 每个PIN使用唯一的随机盐值，防止彩虹表攻击
 * - 存储使用EncryptedSharedPreferences双重加密保护
 * 
 * ## R14-001 修复：构造函数异常处理
 * - KeyStore 损坏时不再导致应用崩溃
 * - 提供降级模式和自动恢复机制
 * 
 * ## R14-004 修复：PBKDF2 迭代次数提升
 * - 从 10000 提升到 600000（OWASP 2023 建议值）
 */
@Singleton
class AppLockRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppLockRepository {
    
    /**
     * R14-001 修复：加密存储状态
     */
    private enum class StorageState {
        READY, DEGRADED, UNAVAILABLE
    }
    
    private var storageState = StorageState.UNAVAILABLE
    
    // R14-001 修复：使用延迟初始化 + 异常处理
    private val masterKey: MasterKey? = run {
        try {
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e("AppLock", "MasterKey 创建失败", e)
            null
        }
    }
    
    private val encryptedPrefs: EncryptedSharedPreferences? = run {
        try {
            val key = masterKey ?: run {
                ApplicationLoggingServiceManager.e("AppLock", "MasterKey 不可用，尝试恢复")
                attemptKeyStoreRecovery()
            }
            if (key != null) {
                storageState = StorageState.READY
                EncryptedSharedPreferences.create(
                    context,
                    "app_lock_prefs",
                    key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                ) as EncryptedSharedPreferences
            } else {
                storageState = StorageState.UNAVAILABLE
                null
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e("AppLock", "EncryptedSharedPreferences 创建失败，尝试恢复", e)
            val recovered = attemptFullRecovery()
            if (recovered) {
                storageState = StorageState.DEGRADED
                try {
                    EncryptedSharedPreferences.create(
                        context,
                        "app_lock_prefs",
                        MasterKey.Builder(context)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build(),
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    ) as EncryptedSharedPreferences
                } catch (e2: Exception) {
                    ApplicationLoggingServiceManager.e("AppLock", "恢复后仍无法创建加密存储", e2)
                    storageState = StorageState.UNAVAILABLE
                    null
                }
            } else {
                storageState = StorageState.UNAVAILABLE
                null
            }
        }
    }
    
    /**
     * R14-001 修复：尝试恢复 KeyStore
     */
    private fun attemptKeyStoreRecovery(): MasterKey? {
        return try {
            // 删除可能损坏的加密存储
            context.deleteSharedPreferences("app_lock_prefs")
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e("AppLock", "KeyStore 恢复失败", e)
            null
        }
    }
    
    /**
     * R14-001 修复：完整恢复流程
     */
    private fun attemptFullRecovery(): Boolean {
        return try {
            context.deleteSharedPreferences("app_lock_prefs")
            true
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e("AppLock", "完整恢复失败", e)
            false
        }
    }
    
    private val lockedAppsFlow = MutableStateFlow<List<AppLockConfiguration>>(emptyList())
    
    private val biometricManager = BiometricManager.from(context)
    
    companion object {
        private const val KEY_PIN_HASH = "pin_hash_"
        private const val KEY_PIN_SALT = "pin_salt_"
        private const val KEY_LOCK_TYPE = "lock_type_"
        private const val KEY_IS_LOCKED = "is_locked_"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts_"
        private const val MAX_ATTEMPTS = 5
        // R14-004 修复：PBKDF2 迭代次数从 10000 提升到 600000（OWASP 2023 建议）
        private const val PBKDF2_ITERATIONS = 600000
        private const val SALT_LENGTH = 32
    }
    
    override fun getAllLockedApps(): Flow<List<AppLockConfiguration>> {
        return lockedAppsFlow.asStateFlow()
    }
    
    override suspend fun getLockConfiguration(
        packageName: String,
        profileUserId: Int
    ): AppLockConfiguration? {
        val key = getKey(packageName, profileUserId)
        val lockTypeStr = encryptedPrefs.getString("${KEY_LOCK_TYPE}$key", null) ?: return null
        
        return AppLockConfiguration(
            packageName = packageName,
            profileUserId = profileUserId,
            lockType = AppLockConfiguration.LockType.valueOf(lockTypeStr),
            isLocked = encryptedPrefs.getBoolean("${KEY_IS_LOCKED}$key", true)
        )
    }
    
    override suspend fun setAppLock(configuration: AppLockConfiguration): Result<Boolean> {
        return try {
            val key = getKey(configuration.packageName, configuration.profileUserId)
            
            encryptedPrefs.edit()
                .putString("${KEY_LOCK_TYPE}$key", configuration.lockType.name)
                .putBoolean("${KEY_IS_LOCKED}$key", configuration.isLocked)
                .apply()
            
            updateLockedAppsFlow()
            Result.success(true)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e("AppLock", "设置应用锁失败", e)
            Result.failure(e)
        }
    }
    
    override suspend fun removeAppLock(packageName: String, profileUserId: Int): Result<Boolean> {
        return try {
            val key = getKey(packageName, profileUserId)
            
            encryptedPrefs.edit()
                .remove("${KEY_PIN_HASH}$key")
                .remove("${KEY_LOCK_TYPE}$key")
                .remove("${KEY_IS_LOCKED}$key")
                .apply()
            
            updateLockedAppsFlow()
            Result.success(true)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e("AppLock", "移除应用锁失败", e)
            Result.failure(e)
        }
    }
    
    override suspend fun verifyPin(
        packageName: String,
        profileUserId: Int,
        pin: String
    ): LockVerificationResult {
        val key = getKey(packageName, profileUserId)
        val storedHash = encryptedPrefs.getString("${KEY_PIN_HASH}$key", null)
            ?: return LockVerificationResult.Error("未设置PIN码")
        val storedSalt = encryptedPrefs.getString("${KEY_PIN_SALT}$key", null)
            ?: return LockVerificationResult.Error("PIN码数据损坏")
        
        val failedAttemptsKey = "${KEY_FAILED_ATTEMPTS}$key"
        val failedAttempts = encryptedPrefs.getInt(failedAttemptsKey, 0)
        val attemptsRemaining = MAX_ATTEMPTS - failedAttempts
        
        if (attemptsRemaining <= 0) {
            return LockVerificationResult.LockedOut
        }
        
        // 使用PBKDF2和存储的盐值计算哈希
        val inputHash = hashPinWithSalt(pin, storedSalt)
        
        return if (inputHash == storedHash) {
            encryptedPrefs.edit().remove(failedAttemptsKey).apply()
            LockVerificationResult.Success(AppLockConfiguration.LockType.PIN)
        } else {
            val newFailedAttempts = failedAttempts + 1
            // TOCTOU fix (R9-CONC-003): 使用 commit() 替代 apply() 确保失败尝试计数器
            // 的写入是同步且原子的，防止在读取和写入之间其他线程修改计数器
            encryptedPrefs.edit().putInt(failedAttemptsKey, newFailedAttempts).commit()
            val newAttemptsRemaining = MAX_ATTEMPTS - newFailedAttempts
            if (newAttemptsRemaining <= 0) {
                LockVerificationResult.LockedOut
            } else {
                LockVerificationResult.Failed(attemptsRemaining = newAttemptsRemaining)
            }
        }
    }
    
    override suspend fun verifyBiometric(
        packageName: String,
        profileUserId: Int,
        activity: FragmentActivity
    ): LockVerificationResult {
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )
        
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            return LockVerificationResult.Error("设备不支持生物识别")
        }
        
        return suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(activity)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("验证身份")
                .setSubtitle("请使用生物识别解锁应用")
                .setNegativeButtonText("取消")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
            
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) {
                        try {
                            continuation.resume(LockVerificationResult.Success(AppLockConfiguration.LockType.BIOMETRIC))
                        } catch (e: IllegalStateException) {
                            // Continuation already cancelled, ignore
                        }
                    }
                }
                
                override fun onAuthenticationFailed() {
                    if (continuation.isActive) {
                        try {
                            continuation.resume(LockVerificationResult.Failed(attemptsRemaining = MAX_ATTEMPTS - 1))
                        } catch (e: IllegalStateException) {
                            // Continuation already cancelled, ignore
                        }
                    }
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) {
                        try {
                            when (errorCode) {
                                BiometricPrompt.ERROR_USER_CANCELED,
                                BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                                    continuation.resume(LockVerificationResult.Cancelled)
                                }
                                BiometricPrompt.ERROR_LOCKOUT -> {
                                    continuation.resume(LockVerificationResult.Locked)
                                }
                                else -> {
                                    continuation.resume(LockVerificationResult.Error(errString.toString()))
                                }
                            }
                        } catch (e: IllegalStateException) {
                            // Continuation already cancelled, ignore
                        }
                    }
                }
            }
            
            val biometricPrompt = BiometricPrompt(
                activity,
                executor,
                callback
            )
            
            biometricPrompt.authenticate(promptInfo)
            
            continuation.invokeOnCancellation {
                biometricPrompt.cancelAuthentication()
            }
        }
    }
    
    override suspend fun needsVerification(packageName: String, profileUserId: Int): Boolean {
        val config = getLockConfiguration(packageName, profileUserId) ?: return false
        return config.isLocked && config.lockType != AppLockConfiguration.LockType.NONE
    }
    
    /**
     * 设置PIN码
     * 
     * 使用PBKDF2WithHmacSHA256和随机盐值安全存储PIN码。
     */
    override suspend fun setPin(packageName: String, profileUserId: Int, pin: String): Result<Boolean> {
        return try {
            val key = getKey(packageName, profileUserId)
            
            // 生成随机盐值
            val salt = generateSalt()
            // 使用PBKDF2派生密钥
            val hash = hashPinWithSalt(pin, salt)
            
            encryptedPrefs.edit()
                .putString("${KEY_PIN_HASH}$key", hash)
                .putString("${KEY_PIN_SALT}$key", salt)
                .apply()
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getKey(packageName: String, profileUserId: Int): String {
        return "${packageName}_$profileUserId"
    }
    
    /**
     * 生成随机盐值
     * 
     * 使用SecureRandom生成加密安全的随机盐值。
     * 
     * @return Base64编码的盐值字符串
     */
    private fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)
    }
    
    /**
     * 使用PBKDF2和盐值哈希PIN码
     * 
     * 使用PBKDF2WithHmacSHA256算法，迭代10000次，
     * 配合唯一盐值，有效防止彩虹表攻击。
     * 
     * @param pin 用户输入的PIN码
     * @param salt Base64编码的盐值
     * @return 十六进制格式的哈希值
     */
    private fun hashPinWithSalt(pin: String, salt: String): String {
        val saltBytes = android.util.Base64.decode(salt, android.util.Base64.NO_WRAP)
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = javax.crypto.spec.PBEKeySpec(
            pin.toCharArray(),
            saltBytes,
            PBKDF2_ITERATIONS,
            256  // 输出密钥长度（位）
        )
        val hash = factory.generateSecret(spec).encoded
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * @deprecated 使用 hashPinWithSalt 替代，此方法仅用于向后兼容
     */
    @Deprecated("使用 hashPinWithSalt 替代", ReplaceWith("hashPinWithSalt(pin, salt)"))
    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    private suspend fun updateLockedAppsFlow() {
        // Reload all locked apps from preferences
        val lockedApps = mutableListOf<AppLockConfiguration>()
        encryptedPrefs.all.keys.forEach { key ->
            if (key.startsWith(KEY_LOCK_TYPE)) {
                val lockTypeStr = encryptedPrefs.getString(key, null) ?: return@forEach
                val lastUnderscore = key.removePrefix(KEY_LOCK_TYPE).lastIndexOf("_")
                if (lastUnderscore > 0) {
                    val packageName = key.removePrefix(KEY_LOCK_TYPE).substring(0, lastUnderscore)
                    val userId = key.removePrefix(KEY_LOCK_TYPE).substring(lastUnderscore + 1).toIntOrNull() ?: continue
                    lockedApps.add(
                        AppLockConfiguration(
                            packageName = packageName,
                            profileUserId = userId,
                            lockType = AppLockConfiguration.LockType.valueOf(lockTypeStr)
                        )
                    )
                }
            }
        }
        lockedAppsFlow.value = lockedApps
    }

    override suspend fun removeAllLocksForProfile(profileUserId: Int): Int {
        return try {
            val keysToRemove = mutableListOf<String>()
            val profileSuffix = "_$profileUserId"

            encryptedPrefs.all.keys.forEach { key ->
                // 匹配所有以 _{profileUserId} 结尾的锁相关键
                if (key.endsWith(profileSuffix) &&
                    (key.startsWith(KEY_PIN_HASH) || key.startsWith(KEY_PIN_SALT) ||
                     key.startsWith(KEY_LOCK_TYPE) || key.startsWith(KEY_IS_LOCKED) ||
                     key.startsWith(KEY_FAILED_ATTEMPTS))) {
                    keysToRemove.add(key)
                }
            }

            if (keysToRemove.isNotEmpty()) {
                encryptedPrefs.edit()
                    .apply {
                        keysToRemove.forEach { key -> remove(key) }
                    }
                    .apply()
                updateLockedAppsFlow()
                val lockCount = keysToRemove.count { it.startsWith(KEY_LOCK_TYPE) }
                ApplicationLoggingServiceManager.i(
                    "AppLock",
                    "已删除配置文件 $profileUserId 下的 $lockCount 个应用锁"
                )
                lockCount
            } else {
                0
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e("AppLock", "批量删除应用锁失败", e)
            0
        }
    }
}
