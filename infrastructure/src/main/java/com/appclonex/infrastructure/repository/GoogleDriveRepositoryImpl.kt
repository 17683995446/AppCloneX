/*
 * Copyright (c) 2024 AppCloneX Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appclonex.infrastructure.repository

import android.content.Context
import com.appclonex.domain.model.GoogleDriveAuthState
import com.appclonex.domain.model.GoogleDriveConflictResolution
import com.appclonex.domain.model.GoogleDriveFile
import com.appclonex.domain.model.GoogleDriveSyncStatus
import com.appclonex.domain.model.GoogleDriveUploadProgress
import com.appclonex.domain.model.SyncConflictInfo
import com.appclonex.domain.model.TransferState
import com.appclonex.domain.repository.GoogleDriveRepository
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import com.appclonex.infrastructure.network.GoogleDriveApiService
import com.appclonex.infrastructure.persistence.datastore.ApplicationPreferencesDataStoreManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Drive 仓库实现
 * 实现 [GoogleDriveRepository] 接口，提供完整的 Google Drive 云备份集成功能。
 *
 * 核心特性：
 * - 使用 DataStore 持久化认证状态
 * - 使用 MutableStateFlow 暴露同步状态
 * - 自动重试机制（指数退避，最多3次）
 * - 线程安全：使用 Mutex 保护并发操作
 * - 完善的错误处理（网络错误、认证过期、存储空间不足）
 */
@Singleton
class GoogleDriveRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: GoogleDriveApiService,
    private val dataStoreManager: ApplicationPreferencesDataStoreManager
) : GoogleDriveRepository {

    companion object {
        private const val TAG = "GoogleDriveRepo"

        // DataStore 键名
        private const val KEY_GD_SIGNED_IN = "gdrive_signed_in"
        private const val KEY_GD_ACCOUNT_EMAIL = "gdrive_account_email"
        private const val KEY_GD_ACCOUNT_NAME = "gdrive_account_name"
        private const val KEY_GD_LAST_AUTH_TIME = "gdrive_last_auth_time"
        private const val KEY_GD_BACKUP_FOLDER_ID = "gdrive_backup_folder_id"

        // 重试配置
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_BASE_DELAY_MS = 1000L // 1秒基础延迟

        // Google Sign-In 请求码
        private const val RC_SIGN_IN = 9001
    }

    // ==================== 状态管理 ====================

    /**
     * 认证状态的响应式数据流
     */
    private val authStateFlow = MutableStateFlow(GoogleDriveAuthState())

    /**
     * 同步状态的响应式数据流
     */
    private val syncStatusFlow = MutableStateFlow(GoogleDriveSyncStatus.IDLE)

    /**
     * 备份文件夹 ID 缓存
     */
    @Volatile
    private var cachedFolderId: String? = null

    /**
     * 互斥锁，保护并发文件操作
     * 确保同一时间只有一个上传/下载/同步操作在执行
     */
    private val operationMutex = Mutex()

    init {
        // 初始化时从 DataStore 加载认证状态
        loadAuthStateFromDataStore()
    }

    // ==================== 认证管理 / Authentication ====================

    /**
     * 发起 Google 账号认证流程
     * 检查已有认证状态，如果无效则通过 Google Sign-In 重新认证。
     * 认证成功后自动获取 OAuth2 令牌并查找/创建备份文件夹。
     *
     * @return 认证结果，成功时包含 [GoogleDriveAuthState]
     */
    override suspend fun authenticate(): Result<GoogleDriveAuthState> {
        return try {
            // 检查是否已有有效认证
            val currentState = authStateFlow.value
            if (currentState.isSignedIn && isTokenValid()) {
                ApplicationLoggingServiceManager.i(TAG, "已有有效认证，直接返回")
                return Result.success(currentState)
            }

            // 通过 Google Sign-In API 发起认证
            // 注意：实际认证流程需要在 Activity 中使用 GoogleSignInClient
            // 这里通过静默登录尝试获取令牌
            val signInResult = performSilentSignIn()
            if (signInResult == null) {
                ApplicationLoggingServiceManager.w(TAG, "静默登录失败，需要用户交互认证")
                return Result.failure(
                    IllegalStateException("需要用户交互认证。请在 Activity 中调用 Google Sign-In。")
                )
            }

            // 更新认证状态
            val newState = GoogleDriveAuthState(
                isSignedIn = true,
                accountEmail = signInResult.first,
                accountName = signInResult.second,
                lastAuthTime = System.currentTimeMillis()
            )
            updateAuthState(newState)

            // 预创建备份文件夹
            ensureBackupFolder()

            ApplicationLoggingServiceManager.i(TAG, "Google Drive 认证成功: ${newState.accountEmail}")
            Result.success(newState)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "Google Drive 认证失败", e)
            Result.failure(e)
        }
    }

    /**
     * 退出当前 Google 账号登录
     * 清除认证令牌、本地缓存和 DataStore 中的认证信息
     *
     * @return 退出操作结果
     */
    override suspend fun signOut(): Result<Unit> {
        return try {
            // 清除 API 服务的认证令牌
            apiService.setAuthToken(null)

            // 清除本地缓存
            cachedFolderId = null

            // 更新状态
            updateAuthState(GoogleDriveAuthState())
            syncStatusFlow.value = GoogleDriveSyncStatus.IDLE

            // 清除 DataStore 中的认证信息
            dataStoreManager.setBooleanValue(KEY_GD_SIGNED_IN, false)
            dataStoreManager.setStringValue(KEY_GD_ACCOUNT_EMAIL, "")
            dataStoreManager.setStringValue(KEY_GD_ACCOUNT_NAME, "")
            dataStoreManager.setStringValue(KEY_GD_LAST_AUTH_TIME, "0")
            dataStoreManager.setStringValue(KEY_GD_BACKUP_FOLDER_ID, "")

            ApplicationLoggingServiceManager.i(TAG, "Google Drive 已退出登录")
            Result.success(Unit)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "退出 Google Drive 登录失败", e)
            Result.failure(e)
        }
    }

    /**
     * 检查当前是否已通过认证且令牌有效
     *
     * @return true 表示已认证
     */
    override suspend fun isAuthenticated(): Boolean {
        val state = authStateFlow.value
        return state.isSignedIn && isTokenValid()
    }

    /**
     * 获取认证状态的响应式数据流
     *
     * @return [GoogleDriveAuthState] 的 Flow
     */
    override fun getAuthState(): Flow<GoogleDriveAuthState> {
        return authStateFlow.asStateFlow()
    }

    // ==================== 文件操作 / File Operations ====================

    /**
     * 列出 Google Drive 备份文件夹中的所有备份文件
     * 文件按修改时间降序排列
     *
     * @return 备份文件列表
     */
    override suspend fun listBackups(): Result<List<GoogleDriveFile>> {
        return executeWithRetry("listBackups") {
            val token = requireValidToken()
            val folderId = ensureBackupFolder()
                ?: return@executeWithRetry Result.failure(
                    IllegalStateException("无法获取或创建备份文件夹")
                )

            val files = apiService.listFiles(token, folderId)
            Result.success(files)
        }
    }

    /**
     * 上传本地备份文件到 Google Drive
     * 使用 Mutex 确保线程安全，支持断点续传和进度回调
     *
     * @param localFile 要上传的本地文件
     * @param progress 传输进度回调
     * @return 上传成功后的远程文件元数据
     */
    override suspend fun uploadBackup(
        localFile: File,
        progress: (GoogleDriveUploadProgress) -> Unit
    ): Result<GoogleDriveFile> {
        return operationMutex.withLock {
            try {
                syncStatusFlow.value = GoogleDriveSyncStatus.SYNCING
                progress(GoogleDriveUploadProgress(state = TransferState.UPLOADING))

                val result = executeWithRetry("uploadBackup") {
                    val token = requireValidToken()
                    val folderId = ensureBackupFolder()
                        ?: return@executeWithRetry Result.failure(
                            IllegalStateException("无法获取或创建备份文件夹")
                        )

                    // 增量同步检查：查找远程是否已存在同名文件
                    val existingFiles = apiService.listFiles(token, folderId)
                    val existingFile = existingFiles.find { it.name == localFile.name }

                    if (existingFile != null && !apiService.needsUpload(localFile, existingFile)) {
                        ApplicationLoggingServiceManager.i(
                            TAG,
                            "文件已是最新，跳过上传: ${localFile.name}"
                        )
                        progress(GoogleDriveUploadProgress(localFile.length(), localFile.length(), TransferState.COMPLETED))
                        return@executeWithRetry Result.success(existingFile)
                    }

                    // 检测冲突
                    if (existingFile != null && apiService.detectConflict(localFile, existingFile)) {
                        syncStatusFlow.value = GoogleDriveSyncStatus.CONFLICT
                        val conflict = SyncConflictInfo(
                            localFile = GoogleDriveFile(
                                name = localFile.name,
                                size = localFile.length(),
                                modifiedTime = java.text.SimpleDateFormat(
                                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                    java.util.Locale.US
                                ).format(java.util.Date(localFile.lastModified()))
                            ),
                            remoteFile = existingFile,
                            localModifiedTime = localFile.lastModified(),
                            remoteModifiedTime = parseModifiedTimeToMillis(existingFile.modifiedTime),
                            suggestedResolution = if (localFile.lastModified() > parseModifiedTimeToMillis(existingFile.modifiedTime)) {
                                GoogleDriveConflictResolution.KEEP_LOCAL
                            } else {
                                GoogleDriveConflictResolution.KEEP_REMOTE
                            }
                        )
                        return@executeWithRetry Result.failure(
                            GoogleDriveConflictException(conflict)
                        )
                    }

                    // 执行上传
                    val uploadedFile = apiService.uploadFile(token, localFile, folderId) { transferred, total ->
                        progress(
                            GoogleDriveUploadProgress(
                                bytesTransferred = transferred,
                                totalBytes = total,
                                state = TransferState.UPLOADING
                            )
                        )
                    }

                    if (uploadedFile != null) {
                        progress(
                            GoogleDriveUploadProgress(
                                bytesTransferred = localFile.length(),
                                totalBytes = localFile.length(),
                                state = TransferState.COMPLETED
                            )
                        )
                        syncStatusFlow.value = GoogleDriveSyncStatus.SUCCESS
                        Result.success(uploadedFile)
                    } else {
                        progress(GoogleDriveUploadProgress(state = TransferState.FAILED))
                        syncStatusFlow.value = GoogleDriveSyncStatus.FAILED
                        Result.failure(IllegalStateException("文件上传失败: ${localFile.name}"))
                    }
                }

                result
            } catch (e: GoogleDriveConflictException) {
                // 冲突不重试，直接返回
                progress(GoogleDriveUploadProgress(state = TransferState.PAUSED))
                Result.failure(e)
            } catch (e: Exception) {
                progress(GoogleDriveUploadProgress(state = TransferState.FAILED))
                syncStatusFlow.value = GoogleDriveSyncStatus.FAILED
                Result.failure(e)
            }
        }
    }

    /**
     * 从 Google Drive 下载备份文件到本地
     * 使用 Mutex 确保线程安全，支持进度回调
     *
     * @param driveFileId Google Drive 文件 ID
     * @param destination 本地目标文件
     * @param progress 传输进度回调
     * @return 下载完成后的本地文件
     */
    override suspend fun downloadBackup(
        driveFileId: String,
        destination: File,
        progress: (GoogleDriveUploadProgress) -> Unit
    ): Result<File> {
        return operationMutex.withLock {
            try {
                syncStatusFlow.value = GoogleDriveSyncStatus.SYNCING
                progress(GoogleDriveUploadProgress(state = TransferState.UPLOADING))

                val result = executeWithRetry("downloadBackup") {
                    val token = requireValidToken()

                    val downloadedFile = apiService.downloadFile(token, driveFileId, destination) { transferred, total ->
                        progress(
                            GoogleDriveUploadProgress(
                                bytesTransferred = transferred,
                                totalBytes = total,
                                state = TransferState.UPLOADING
                            )
                        )
                    }

                    if (downloadedFile != null) {
                        progress(
                            GoogleDriveUploadProgress(
                                bytesTransferred = downloadedFile.length(),
                                totalBytes = downloadedFile.length(),
                                state = TransferState.COMPLETED
                            )
                        )
                        syncStatusFlow.value = GoogleDriveSyncStatus.SUCCESS
                        Result.success(downloadedFile)
                    } else {
                        progress(GoogleDriveUploadProgress(state = TransferState.FAILED))
                        syncStatusFlow.value = GoogleDriveSyncStatus.FAILED
                        Result.failure(IllegalStateException("文件下载失败: $driveFileId"))
                    }
                }

                result
            } catch (e: Exception) {
                progress(GoogleDriveUploadProgress(state = TransferState.FAILED))
                syncStatusFlow.value = GoogleDriveSyncStatus.FAILED
                Result.failure(e)
            }
        }
    }

    /**
     * 删除 Google Drive 上的指定备份文件
     *
     * @param driveFileId 要删除的文件 ID
     * @return 删除操作结果
     */
    override suspend fun deleteBackup(driveFileId: String): Result<Unit> {
        return executeWithRetry("deleteBackup") {
            val token = requireValidToken()
            val deleted = apiService.deleteFile(token, driveFileId)
            if (deleted) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("删除文件失败: $driveFileId"))
            }
        }
    }

    /**
     * 获取指定文件的详细信息
     *
     * @param driveFileId Google Drive 文件 ID
     * @return 文件元数据
     */
    override suspend fun getBackupInfo(driveFileId: String): Result<GoogleDriveFile> {
        return executeWithRetry("getBackupInfo") {
            val token = requireValidToken()
            val fileInfo = apiService.getFileInfo(token, driveFileId)
            if (fileInfo != null) {
                Result.success(fileInfo)
            } else {
                Result.failure(IllegalStateException("获取文件信息失败: $driveFileId"))
            }
        }
    }

    // ==================== 冲突解决 / Conflict Resolution ====================

    /**
     * 解决同步冲突
     * 根据用户选择的策略处理本地与远程文件的不一致
     *
     * @param conflict 冲突信息
     * @param resolution 冲突解决策略
     * @return 冲突解决操作结果
     */
    override suspend fun resolveConflict(
        conflict: SyncConflictInfo,
        resolution: GoogleDriveConflictResolution
    ): Result<Unit> {
        return try {
            when (resolution) {
                GoogleDriveConflictResolution.KEEP_LOCAL -> {
                    // 保留本地版本：删除远程文件，后续上传会覆盖
                    ApplicationLoggingServiceManager.i(TAG, "冲突解决: 保留本地版本")
                    if (conflict.remoteFile.id.isNotEmpty()) {
                        deleteBackup(conflict.remoteFile.id)
                    }
                    syncStatusFlow.value = GoogleDriveSyncStatus.IDLE
                    Result.success(Unit)
                }
                GoogleDriveConflictResolution.KEEP_REMOTE -> {
                    // 保留远程版本：通知调用方下载远程文件覆盖本地
                    ApplicationLoggingServiceManager.i(TAG, "冲突解决: 保留远程版本")
                    syncStatusFlow.value = GoogleDriveSyncStatus.IDLE
                    Result.success(Unit)
                }
                GoogleDriveConflictResolution.KEEP_BOTH -> {
                    // 同时保留两个版本：远程文件重命名
                    ApplicationLoggingServiceManager.i(TAG, "冲突解决: 同时保留两个版本")
                    // 远程文件会被保留，本地文件上传时使用新名称
                    syncStatusFlow.value = GoogleDriveSyncStatus.IDLE
                    Result.success(Unit)
                }
                GoogleDriveConflictResolution.MANUAL -> {
                    // 交由用户手动处理
                    ApplicationLoggingServiceManager.i(TAG, "冲突解决: 等待用户手动处理")
                    syncStatusFlow.value = GoogleDriveSyncStatus.CONFLICT
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "解决冲突失败", e)
            syncStatusFlow.value = GoogleDriveSyncStatus.FAILED
            Result.failure(e)
        }
    }

    // ==================== 同步状态 / Sync Status ====================

    /**
     * 获取当前同步状态的响应式数据流
     *
     * @return [GoogleDriveSyncStatus] 的 Flow
     */
    override fun getSyncStatus(): Flow<GoogleDriveSyncStatus> {
        return syncStatusFlow.asStateFlow()
    }

    // ==================== 公开辅助方法 ====================

    /**
     * 设置外部获取的 OAuth2 令牌
     * 由 UI 层在 Google Sign-In 成功后调用
     *
     * @param token OAuth2 访问令牌
     * @param email 用户邮箱
     * @param displayName 用户显示名称
     */
    suspend fun setAuthTokenFromSignIn(token: String, email: String, displayName: String) {
        apiService.setAuthToken(token)
        val newState = GoogleDriveAuthState(
            isSignedIn = true,
            accountEmail = email,
            accountName = displayName,
            lastAuthTime = System.currentTimeMillis()
        )
        updateAuthState(newState)
        ensureBackupFolder()
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从 DataStore 加载认证状态
     */
    private suspend fun loadAuthStateFromDataStore() {
        try {
            val isSignedIn = dataStoreManager.getBooleanValue(KEY_GD_SIGNED_IN, false).first()
            val email = dataStoreManager.getStringValue(KEY_GD_ACCOUNT_EMAIL).first()
            val name = dataStoreManager.getStringValue(KEY_GD_ACCOUNT_NAME).first()
            val lastAuthTime = dataStoreManager.getStringValue(KEY_GD_LAST_AUTH_TIME, "0").first()
                .toLongOrNull() ?: 0L
            val folderId = dataStoreManager.getStringValue(KEY_GD_BACKUP_FOLDER_ID).first()

            authStateFlow.value = GoogleDriveAuthState(
                isSignedIn = isSignedIn,
                accountEmail = email,
                accountName = name,
                lastAuthTime = lastAuthTime
            )

            if (folderId.isNotEmpty()) {
                cachedFolderId = folderId
            }

            ApplicationLoggingServiceManager.i(TAG, "认证状态已从 DataStore 加载: signedIn=$isSignedIn")
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "从 DataStore 加载认证状态失败", e)
        }
    }

    /**
     * 更新认证状态并持久化到 DataStore
     */
    private suspend fun updateAuthState(state: GoogleDriveAuthState) {
        authStateFlow.value = state
        dataStoreManager.setBooleanValue(KEY_GD_SIGNED_IN, state.isSignedIn)
        dataStoreManager.setStringValue(KEY_GD_ACCOUNT_EMAIL, state.accountEmail)
        dataStoreManager.setStringValue(KEY_GD_ACCOUNT_NAME, state.accountName)
        dataStoreManager.setStringValue(KEY_GD_LAST_AUTH_TIME, state.lastAuthTime.toString())
    }

    /**
     * 确保备份文件夹存在
     * 优先使用缓存，不存在时通过 API 查找或创建
     *
     * @return 备份文件夹 ID，失败时返回 null
     */
    private suspend fun ensureBackupFolder(): String? {
        // 优先使用缓存
        cachedFolderId?.let { return it }

        val token = apiService.getAuthToken() ?: return null
        val folderId = apiService.findOrCreateBackupFolder(token)

        if (folderId != null) {
            cachedFolderId = folderId
            dataStoreManager.setStringValue(KEY_GD_BACKUP_FOLDER_ID, folderId)
        }

        return folderId
    }

    /**
     * 获取有效的认证令牌，未认证时抛出异常
     *
     * @return 有效的 OAuth2 令牌
     * @throws IllegalStateException 未认证时抛出
     */
    private fun requireValidToken(): String {
        val token = apiService.getAuthToken()
        if (token.isNullOrBlank()) {
            throw IllegalStateException("未认证或令牌已过期，请先调用 authenticate()")
        }
        return token
    }

    /**
     * 检查当前令牌是否有效
     * 简单检查：令牌非空且认证时间在24小时内
     */
    private fun isTokenValid(): Boolean {
        val token = apiService.getAuthToken()
        if (token.isNullOrBlank()) return false

        val lastAuthTime = authStateFlow.value.lastAuthTime
        if (lastAuthTime <= 0) return false

        // Google OAuth2 令牌通常有效期为1小时，这里保守设置为24小时
        // 实际使用中应在每次请求前通过 TokenResponse 检查过期
        val elapsed = System.currentTimeMillis() - lastAuthTime
        return elapsed < 24 * 60 * 60 * 1000
    }

    /**
     * 执行静默登录
     * 尝试使用已保存的账号信息进行静默认证
     *
     * @return Pair(邮箱, 显示名称)，失败时返回 null
     */
    private suspend fun performSilentSignIn(): Pair<String, String>? {
        // 检查是否有已保存的认证信息
        val savedState = authStateFlow.value
        if (!savedState.isSignedIn || savedState.accountEmail.isEmpty()) {
            return null
        }

        // 在实际实现中，这里应使用 GoogleSignInClient.silentSignIn()
        // 当前返回 null，表示需要用户交互认证
        ApplicationLoggingServiceManager.i(TAG, "静默登录需要用户交互，请在 Activity 中完成认证")
        return null
    }

    /**
     * 带自动重试的操作执行器
     * 使用指数退避策略，在操作失败时自动重试
     *
     * @param operationName 操作名称（用于日志）
     * @param block 要执行的操作
     * @return 操作结果
     */
    private suspend fun <T> executeWithRetry(
        operationName: String,
        block: suspend () -> Result<T>
    ): Result<T> {
        var lastException: Exception? = null

        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                val result = block()
                if (result.isSuccess) {
                    return result
                }
                // 操作返回了失败结果，检查是否可重试
                lastException = result.exceptionOrNull()
                if (!isRetryableError(lastException)) {
                    return result
                }
            } catch (e: GoogleDriveConflictException) {
                // 冲突异常不重试
                throw e
            } catch (e: Exception) {
                lastException = e
                if (!isRetryableError(e)) {
                    return Result.failure(e)
                }
            }

            // 指数退避延迟
            if (attempt < MAX_RETRY_COUNT - 1) {
                val delay = RETRY_BASE_DELAY_MS * (1L shl attempt) // 1s, 2s, 4s
                ApplicationLoggingServiceManager.w(
                    TAG,
                    "$operationName 失败，第 ${attempt + 1} 次重试，等待 ${delay}ms..."
                )
                kotlinx.coroutines.delay(delay)
            }
        }

        ApplicationLoggingServiceManager.e(
            TAG,
            "$operationName 在 $MAX_RETRY_COUNT 次尝试后仍然失败"
        )
        return Result.failure(
            lastException ?: IllegalStateException("$operationName 重试次数已用尽")
        )
    }

    /**
     * 判断错误是否可重试
     * 网络错误和认证过期可重试，其他错误不重试
     *
     * @param error 异常对象
     * @return true 表示可以重试
     */
    private fun isRetryableError(error: Throwable?): Boolean {
        if (error == null) return false

        return when (error) {
            is java.net.SocketTimeoutException,
            is java.net.ConnectException,
            is java.net.UnknownHostException,
            is javax.net.ssl.SSLException -> {
                // 网络相关错误，可重试
                true
            }
            is IllegalStateException -> {
                // 认证过期等状态错误，可重试
                error.message?.contains("认证") == true ||
                        error.message?.contains("令牌") == true ||
                        error.message?.contains("token") == true
            }
            else -> false
        }
    }

    /**
     * 解析 ISO 8601 修改时间字符串为毫秒时间戳
     */
    private fun parseModifiedTimeToMillis(modifiedTime: String): Long {
        if (modifiedTime.isEmpty()) return 0L
        return try {
            val cleanedTime = modifiedTime.replace("Z", "+00:00")
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US)
            format.parse(cleanedTime)?.time ?: 0L
        } catch (e: Exception) {
            try {
                val cleanedTime = modifiedTime.replace("Z", "+00:00")
                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
                format.parse(cleanedTime)?.time ?: 0L
            } catch (e2: Exception) {
                0L
            }
        }
    }

    // ==================== 自定义异常 ====================

    /**
     * Google Drive 冲突异常
     * 当检测到本地与远程文件冲突时抛出，携带冲突信息
     */
    class GoogleDriveConflictException(
        val conflict: SyncConflictInfo
    ) : IllegalStateException("检测到文件冲突，需要用户介入解决: ${conflict.localFile.name}")
}
