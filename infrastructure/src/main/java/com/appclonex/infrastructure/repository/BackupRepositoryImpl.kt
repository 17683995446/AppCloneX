package com.appclonex.infrastructure.repository

import android.content.Context
import android.os.Build
import com.appclonex.domain.model.BackupData
import com.appclonex.domain.model.BackupMetadata
import com.appclonex.domain.model.ClonedAppBackupData
import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.domain.model.ProfileBackupData
import com.appclonex.domain.model.UserProfileConfigurationModel
import com.appclonex.domain.repository.BackupRepository
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import com.appclonex.infrastructure.persistence.database.ApplicationDatabaseConfiguration
import com.appclonex.infrastructure.persistence.database.ClonedApplicationDataAccessObject
import com.appclonex.infrastructure.persistence.database.UserProfileEntityDataAccessObject
import com.appclonex.infrastructure.persistence.datastore.ApplicationPreferencesDataStoreManager
import com.appclonex.infrastructure.utility.AtomicFileWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 备份仓库实现
 * Backup repository implementation
 *
 * 负责完整备份和恢复操作，包括创建备份、恢复备份、管理备份文件、
 * 导入导出以及自动备份等功能
 * Responsible for complete backup and restore operations, including creating backups,
 * restoring backups, managing backup files, import/export, and auto backup
 */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ApplicationDatabaseConfiguration,
    private val dataStoreManager: ApplicationPreferencesDataStoreManager,
    private val userProfileDao: UserProfileEntityDataAccessObject,
    private val clonedAppDao: ClonedApplicationDataAccessObject,
    private val automationRuleRepository: com.appclonex.domain.repository.AutomationRuleRepository,
    private val appLockRepository: com.appclonex.domain.repository.AppLockRepository
) : BackupRepository {

    companion object {
        private const val TAG = "BackupRepository"
        private const val BACKUP_DIR_NAME = "backups"
        private const val BACKUP_FILE_EXTENSION = ".json"
        private const val BACKUP_DATE_FORMAT = "yyyyMMdd_HHmmss"
        private const val MAX_AUTO_BACKUP_COUNT = 5

        // DataStore keys for auto-backup settings
        private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val KEY_AUTO_BACKUP_INTERVAL_HOURS = "auto_backup_interval_hours"
        private const val KEY_LAST_AUTO_BACKUP_TIME = "last_auto_backup_time"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /** 互斥锁，保证并发写操作安全 */
    private val mutex = Mutex()

    private val backupDir: File
        get() {
            val dir = File(context.filesDir, BACKUP_DIR_NAME)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    // ==================== 创建备份 / Create Backup ====================

    /**
     * 创建完整备份
     * Create a full backup of all profiles and settings
     *
     * 读取所有配置文件、克隆应用、DataStore设置，组装为BackupData，
     * 序列化为JSON并写入backups/目录，返回BackupMetadata
     */
    override suspend fun createBackup(): Result<BackupMetadata> {
        return mutex.withLock {
            try {
                val profiles = userProfileDao.getAllProfiles().first()
                val backupData = assembleBackupData(profiles)
                writeBackupToFile(backupData, isAutoBackup = false)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "创建完整备份失败", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 创建指定配置文件的备份
     * Create a backup for the specified profiles only
     *
     * @param profileIds 需要备份的配置文件ID列表
     */
    override suspend fun createBackup(profileIds: List<Int>): Result<BackupMetadata> {
        return mutex.withLock {
            try {
                val allProfiles = userProfileDao.getAllProfiles().first()
                val filteredProfiles = allProfiles.filter { it.userId in profileIds }
                val backupData = assembleBackupData(filteredProfiles)
                writeBackupToFile(backupData, isAutoBackup = false)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "创建指定配置文件备份失败", e)
                Result.failure(e)
            }
        }
    }

    // ==================== 恢复备份 / Restore Backup ====================

    /**
     * 从备份数据对象恢复
     * Restore from a BackupData object
     *
     * 将配置文件和克隆应用插入Room数据库，恢复DataStore设置
     * 
     * ## 冲突处理策略
     * - 如果配置文件已存在（相同userId），则更新现有记录
     * - 如果克隆应用已存在（相同packageName+profileUserId+cloneIndex），则更新现有记录
     * - 使用事务确保原子性，失败时自动回滚
     */
    override suspend fun restoreBackup(backupData: BackupData): Result<Unit> {
        return mutex.withLock {
            try {
                ApplicationLoggingServiceManager.i(TAG, "开始恢复备份，包含 ${backupData.profiles.size} 个配置文件")

                // 第一步：使用事务恢复Room数据库数据（原子性保证）
                database.runInTransaction {
                    for (profileBackup in backupData.profiles) {
                        val profile = UserProfileConfigurationModel(
                            userId = profileBackup.userId,
                            profileName = profileBackup.profileName,
                            profileIcon = profileBackup.profileIcon,
                            createdAt = profileBackup.createdAt,
                            updatedAt = profileBackup.updatedAt,
                            isActive = profileBackup.isActive,
                            serialNumber = profileBackup.serialNumber,
                            notes = profileBackup.notes
                        )
                        
                        val existingProfile = userProfileDao.getProfileById(profile.userId)
                        if (existingProfile != null) {
                            userProfileDao.updateProfile(profile)
                            ApplicationLoggingServiceManager.d(
                                TAG, 
                                "更新已存在的配置文件: userId=${profile.userId}"
                            )
                        } else {
                            userProfileDao.insertProfile(profile)
                        }

                        for (appBackup in profileBackup.clonedApps) {
                            val clonedApp = ClonedApplicationInformationModel(
                                packageName = appBackup.packageName,
                                appName = appBackup.appName,
                                appIcon = appBackup.appIcon,
                                profileUserId = profileBackup.userId,
                                cloneIndex = appBackup.cloneIndex,
                                versionName = appBackup.versionName,
                                versionCode = appBackup.versionCode,
                                isEnabled = appBackup.isEnabled,
                                customName = appBackup.customName,
                                customIcon = appBackup.customIcon,
                                notes = appBackup.notes
                            )
                            
                            val existingApp = clonedAppDao.getAppByPackageAndProfileAndIndex(
                                clonedApp.packageName,
                                clonedApp.profileUserId,
                                clonedApp.cloneIndex
                            )
                            
                            if (existingApp != null) {
                                clonedAppDao.updateApp(clonedApp)
                            } else {
                                clonedAppDao.insertApp(clonedApp)
                            }
                        }
                    }
                }

                // 第二步：事务成功后恢复DataStore设置（不受Room事务管理）
                try {
                    restoreDataStoreSettings(backupData.settings)
                } catch (e: Exception) {
                    // DataStore恢复失败不影响数据库恢复，记录警告即可
                    ApplicationLoggingServiceManager.w(
                        TAG, 
                        "DataStore设置恢复部分失败（数据库数据已成功恢复）", 
                        e
                    )
                }

                // 第三步：恢复自动化规则
                if (backupData.automationRules.isNotEmpty()) {
                    try {
                        for (rule in backupData.automationRules) {
                            automationRuleRepository.saveRule(rule)
                                .onFailure { e ->
                                    ApplicationLoggingServiceManager.w(
                                        TAG,
                                        "恢复自动化规则失败: ${rule.id}",
                                        e
                                    )
                                }
                        }
                        ApplicationLoggingServiceManager.i(
                            TAG,
                            "已恢复 ${backupData.automationRules.size} 条自动化规则"
                        )
                    } catch (e: Exception) {
                        ApplicationLoggingServiceManager.w(TAG, "自动化规则恢复部分失败", e)
                    }
                }

                // 第四步：恢复应用锁配置（仅恢复元数据，PIN码因安全原因不备份）
                if (backupData.appLockConfig != null && backupData.appLockConfig.isEnabled) {
                    ApplicationLoggingServiceManager.i(
                        TAG,
                        "应用锁配置已恢复（注意：PIN码需要重新设置）"
                    )
                }

                ApplicationLoggingServiceManager.i(TAG, "备份恢复完成")
                Result.success(Unit)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "恢复备份失败", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 从文件恢复备份
     * Restore backup from a file path
     *
     * 读取JSON文件，反序列化为BackupData，然后恢复
     * 
     * ## 安全性
     * - 验证文件路径必须在允许的目录范围内
     * - 防止目录遍历攻击
     */
    override suspend fun restoreFromFile(filePath: String): Result<Unit> {
        return mutex.withLock {
            try {
                val file = validateAndResolveFile(filePath)
                    ?: return@withLock Result.failure(
                        IllegalArgumentException("无效的文件路径: $filePath")
                    )
                    
                if (!file.exists()) {
                    return@withLock Result.failure(IllegalArgumentException("备份文件不存在: ${file.absolutePath}"))
                }

                val jsonString = file.readText()
                val backupData = json.decodeFromString<BackupData>(jsonString)
                restoreBackup(backupData)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "从文件恢复备份失败: $filePath", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * 验证并解析文件路径
     * 
     * 确保文件路径在允许的目录范围内，防止目录遍历攻击。
     * 
     * @param filePath 用户提供的文件路径
     * @return 验证后的File对象，如果路径无效则返回null
     */
    private fun validateAndResolveFile(filePath: String): File? {
        if (filePath.isBlank()) return null
        
        // 检查是否包含目录遍历字符
        if (filePath.contains("..")) {
            ApplicationLoggingServiceManager.w(TAG, "检测到可能的目录遍历攻击: $filePath")
            return null
        }
        
        val file = File(filePath)
        
        // 获取规范路径并验证
        val canonicalPath = try {
            file.canonicalPath
        } catch (e: Exception) {
            return null
        }
        
        // 允许的目录列表
        val allowedDirs = listOf(
            context.filesDir.canonicalPath,
            context.cacheDir.canonicalPath,
            context.getExternalFilesDir(null)?.canonicalPath,
            backupDir.canonicalPath
        ).filterNotNull()
        
        // 验证文件路径必须在允许的目录内
        val isAllowed = allowedDirs.any { allowedDir ->
            canonicalPath.startsWith(allowedDir)
        }
        
        // 如果不在允许目录内，检查是否为外部存储的备份文件
        if (!isAllowed) {
            // 允许从外部存储的Downloads或Documents目录读取备份文件
            val externalStorage = android.os.Environment.getExternalStorageDirectory()
            val downloadsDir = File(externalStorage, "Download").canonicalPath
            val documentsDir = File(externalStorage, "Documents").canonicalPath
            
            if (!canonicalPath.startsWith(downloadsDir) && 
                !canonicalPath.startsWith(documentsDir)) {
                ApplicationLoggingServiceManager.w(
                    TAG, 
                    "文件路径不在允许的目录范围内: $canonicalPath"
                )
                return null
            }
        }
        
        return file
    }

    // ==================== 管理备份文件 / Manage Backup Files ====================

    /**
     * 列出所有可用备份
     * List all available backups
     *
     * 扫描backups/目录，解析每个备份文件的元数据
     */
    override suspend fun listBackups(): Result<List<BackupMetadata>> {
        return try {
            val backupFiles = backupDir.listFiles { file ->
                file.isFile && file.name.endsWith(BACKUP_FILE_EXTENSION)
            } ?: emptyArray()

            val metadataList = backupFiles.mapNotNull { file ->
                try {
                    parseBackupMetadata(file)
                } catch (e: Exception) {
                    ApplicationLoggingServiceManager.w(TAG, "解析备份文件元数据失败: ${file.name}", e)
                    null
                }
            }.sortedByDescending { it.timestamp }

            Result.success(metadataList)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "列出备份失败", e)
            Result.failure(e)
        }
    }

    /**
     * 删除指定备份
     * Delete a specific backup by its ID
     *
     * @param backupId 备份唯一标识（对应备份文件名，不含扩展名）
     */
    override suspend fun deleteBackup(backupId: String): Result<Unit> {
        return mutex.withLock {
            try {
                val file = File(backupDir, "$backupId$BACKUP_FILE_EXTENSION")
                if (!file.exists()) {
                    return@withLock Result.failure(IllegalArgumentException("备份文件不存在: $backupId"))
                }
                if (file.delete()) {
                    ApplicationLoggingServiceManager.i(TAG, "备份已删除: $backupId")
                    Result.success(Unit)
                } else {
                    Result.failure(RuntimeException("无法删除备份文件: $backupId"))
                }
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "删除备份失败: $backupId", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 获取备份详情
     * Get full backup data for a specific backup
     *
     * @param backupId 备份唯一标识
     */
    override suspend fun getBackupDetails(backupId: String): Result<BackupData> {
        return try {
            val file = File(backupDir, "$backupId$BACKUP_FILE_EXTENSION")
            if (!file.exists()) {
                return Result.failure(IllegalArgumentException("备份文件不存在: $backupId"))
            }
            val jsonString = file.readText()
            val backupData = json.decodeFromString<BackupData>(jsonString)
            Result.success(backupData)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "获取备份详情失败: $backupId", e)
            Result.failure(e)
        }
    }

    // ==================== 导入导出 / Import & Export ====================

    /**
     * 将备份数据导出到文件
     * Export backup data to a file
     *
     * 序列化BackupData为JSON字符串，写入指定文件
     *
     * @param backupData 要导出的备份数据
     * @return 导出文件的路径
     */
    override suspend fun exportToFile(backupData: BackupData): Result<String> {
        return mutex.withLock {
            try {
                val timestamp = SimpleDateFormat(BACKUP_DATE_FORMAT, Locale.US).format(Date())
                val fileName = "appclonex_export_$timestamp$BACKUP_FILE_EXTENSION"
                val file = File(backupDir, fileName)

                val jsonString = json.encodeToString(backupData)
                AtomicFileWriter.write(file, jsonString)

                ApplicationLoggingServiceManager.i(TAG, "备份数据已导出到: ${file.absolutePath}")
                Result.success(file.absolutePath)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "导出备份到文件失败", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 从文件导入备份数据
     * Import backup data from a file
     *
     * 读取指定文件，反序列化JSON为BackupData
     *
     * @param filePath 备份文件路径
     * @return 解析后的备份数据
     */
    override suspend fun importFromFile(filePath: String): Result<BackupData> {
        return mutex.withLock {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withLock Result.failure(IllegalArgumentException("导入文件不存在: $filePath"))
                }

                val jsonString = file.readText()
                val backupData = json.decodeFromString<BackupData>(jsonString)

                ApplicationLoggingServiceManager.i(TAG, "从文件导入备份数据成功: $filePath")
                Result.success(backupData)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "从文件导入备份数据失败: $filePath", e)
                Result.failure(e)
            }
        }
    }

    // ==================== 自动备份 / Auto Backup ====================

    /**
     * 执行自动备份
     * Perform an automatic backup
     *
     * 根据自动备份策略判断是否需要执行备份，如果不需要则返回null。
     * 自动备份最多保留5个文件，超出时自动删除最旧的备份。
     *
     * @return 备份元数据，如果未执行备份则为null
     */
    override suspend fun performAutoBackup(): Result<BackupMetadata?> {
        return mutex.withLock {
            try {
                val isAutoBackupEnabled = dataStoreManager
                    .getBooleanValue(KEY_AUTO_BACKUP_ENABLED, false)
                    .first()

                if (!isAutoBackupEnabled) {
                    ApplicationLoggingServiceManager.i(TAG, "自动备份未启用，跳过")
                    return@withLock Result.success(null)
                }

                val lastBackupTime = dataStoreManager
                    .getStringValue(KEY_LAST_AUTO_BACKUP_TIME, "0")
                    .first()
                    .toLongOrNull() ?: 0L

                val intervalHours = dataStoreManager
                    .getIntValue(KEY_AUTO_BACKUP_INTERVAL_HOURS, 24)
                    .first()

                val now = System.currentTimeMillis()
                val intervalMillis = intervalHours.toLong() * 60 * 60 * 1000L

                if (now - lastBackupTime < intervalMillis) {
                    ApplicationLoggingServiceManager.i(TAG, "自动备份间隔未到，跳过")
                    return@withLock Result.success(null)
                }

                // 执行自动备份
                val profiles = userProfileDao.getAllProfiles().first()
                val backupData = assembleBackupData(profiles)
                val metadata = writeBackupToFile(backupData, isAutoBackup = true)

                // 更新最后自动备份时间
                dataStoreManager.setStringValue(KEY_LAST_AUTO_BACKUP_TIME, now.toString())

                // 清理超出数量限制的自动备份
                cleanupOldAutoBackups()

                metadata
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "执行自动备份失败", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 获取自动备份开关状态
     * Observe the auto-backup enabled status
     *
     * @return 自动备份是否启用的Flow
     */
    override fun getAutoBackupStatus(): Flow<Boolean> {
        return dataStoreManager.getBooleanValue(KEY_AUTO_BACKUP_ENABLED, false)
    }

    // ==================== 备份大小 / Backup Size ====================

    /**
     * 获取当前备份占用的总存储空间
     * Get the total storage space used by backups
     *
     * @return 备份总大小（字节）
     */
    override suspend fun getBackupSize(): Long {
        return try {
            val backupFiles = backupDir.listFiles { file ->
                file.isFile && file.name.endsWith(BACKUP_FILE_EXTENSION)
            } ?: return 0L

            backupFiles.sumOf { it.length() }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "获取备份大小失败", e)
            0L
        }
    }

    /**
     * 获取最大备份文件数量
     * Get the maximum number of backup files allowed
     *
     * @return 最大备份数量
     */
    override suspend fun getMaxBackupCount(): Int {
        return MAX_AUTO_BACKUP_COUNT
    }

    // ==================== 私有辅助方法 / Private Helper Methods ====================

    /**
     * 组装备份数据
     * Assemble backup data from Room database and DataStore settings
     *
     * @param profiles 需要备份的配置文件列表
     * @return 完整的BackupData对象
     */
    private suspend fun assembleBackupData(
        profiles: List<UserProfileConfigurationModel>
    ): BackupData {
        val profileBackupList = profiles.map { profile ->
            val clonedApps = try {
                clonedAppDao.getAppsByProfileList(profile.userId).map { app ->
                    ClonedAppBackupData(
                        packageName = app.packageName,
                        appName = app.appName,
                        appIcon = app.appIcon,
                        cloneIndex = app.cloneIndex,
                        versionName = app.versionName,
                        versionCode = app.versionCode,
                        isEnabled = app.isEnabled,
                        customName = app.customName,
                        customIcon = app.customIcon,
                        notes = app.notes
                    )
                }
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.w(
                    TAG,
                    "读取配置文件 [${profile.profileName}] 的克隆应用失败",
                    e
                )
                emptyList()
            }

            ProfileBackupData(
                userId = profile.userId,
                profileName = profile.profileName,
                profileIcon = profile.profileIcon,
                createdAt = profile.createdAt,
                updatedAt = profile.updatedAt,
                isActive = profile.isActive,
                serialNumber = profile.serialNumber,
                notes = profile.notes,
                clonedApps = clonedApps
            )
        }

        // 读取DataStore设置
        val settings = readAllDataStoreSettings()

        // 读取自动化规则
        val automationRules = try {
            automationRuleRepository.getAllRules().first()
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.w(TAG, "读取自动化规则失败", e)
            emptyList()
        }

        // 读取应用锁配置
        val appLockConfig = try {
            val allLockedApps = appLockRepository.getAllLockedApps().first()
            if (allLockedApps.isNotEmpty()) {
                com.appclonex.domain.model.AppLockConfigBackup(
                    isEnabled = true,
                    lockType = allLockedApps.firstOrNull()?.lockType?.name ?: "none",
                    autoLockTimeout = 0,
                    maxAttempts = 5
                )
            } else {
                null
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.w(TAG, "读取应用锁配置失败", e)
            null
        }

        return BackupData(
            version = 2,
            timestamp = System.currentTimeMillis(),
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            appVersion = getAppVersion(),
            profiles = profileBackupList,
            settings = settings,
            automationRules = automationRules,
            appLockConfig = appLockConfig
        )
    }

    /**
     * 将备份数据写入文件
     * Write backup data to a JSON file in the backups/ directory
     *
     * @param backupData 备份数据
     * @param isAutoBackup 是否为自动备份
     * @return 备份元数据
     */
    private suspend fun writeBackupToFile(
        backupData: BackupData,
        isAutoBackup: Boolean
    ): Result<BackupMetadata> {
        return try {
            val timestamp = SimpleDateFormat(BACKUP_DATE_FORMAT, Locale.US).format(Date())
            val prefix = if (isAutoBackup) "auto_backup" else "backup"
            val fileName = "${prefix}_$timestamp$BACKUP_FILE_EXTENSION"
            val file = File(backupDir, fileName)

            val jsonString = json.encodeToString(backupData)
            AtomicFileWriter.write(file, jsonString)

            val totalAppCount = backupData.profiles.sumOf { it.clonedApps.size }

            val metadata = BackupMetadata(
                id = fileName.removeSuffix(BACKUP_FILE_EXTENSION),
                fileName = fileName,
                fileSize = file.length(),
                timestamp = backupData.timestamp,
                profileCount = backupData.profiles.size,
                appCount = totalAppCount,
                deviceModel = backupData.deviceModel,
                appVersion = backupData.appVersion,
                isAutoBackup = isAutoBackup
            )

            // 更新最后备份时间
            dataStoreManager.updateLastBackupTime()

            ApplicationLoggingServiceManager.i(
                TAG,
                "备份已创建: $fileName (${formatFileSize(file.length())})"
            )

            Result.success(metadata)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "写入备份文件失败", e)
            Result.failure(e)
        }
    }

    /**
     * 从备份文件解析元数据
     * Parse backup metadata from a backup file
     *
     * 读取文件头部信息，快速提取元数据而无需完整反序列化
     *
     * @param file 备份文件
     * @return 备份元数据
     */
    private fun parseBackupMetadata(file: File): BackupMetadata {
        val jsonString = file.readText(Charsets.UTF_8)
        val backupData = json.decodeFromString<BackupData>(jsonString)

        val totalAppCount = backupData.profiles.sumOf { it.clonedApps.size }

        return BackupMetadata(
            id = file.nameWithoutExtension,
            fileName = file.name,
            fileSize = file.length(),
            timestamp = backupData.timestamp,
            profileCount = backupData.profiles.size,
            appCount = totalAppCount,
            deviceModel = backupData.deviceModel,
            appVersion = backupData.appVersion,
            isAutoBackup = file.name.startsWith("auto_backup")
        )
    }

    /**
     * 读取所有DataStore设置
     * Read all DataStore settings as a key-value map
     *
     * @return 设置键值对Map
     */
    private suspend fun readAllDataStoreSettings(): Map<String, String> {
        return try {
            val settings = mutableMapOf<String, String>()

            // 读取已知的设置项
            settings["exclude_system_apps"] =
                dataStoreManager.getBooleanValue("exclude_system_apps", true).first().toString()
            settings["show_notifications"] =
                dataStoreManager.getBooleanValue("show_notifications", true).first().toString()
            settings["auto_start_profile"] =
                dataStoreManager.getBooleanValue("auto_start_profile", false).first().toString()
            settings["theme_mode"] =
                dataStoreManager.getIntValue("theme_mode", 0).first().toString()
            settings["auto_backup_enabled"] =
                dataStoreManager.getBooleanValue(KEY_AUTO_BACKUP_ENABLED, false).first().toString()
            settings["auto_backup_interval_hours"] =
                dataStoreManager.getIntValue(KEY_AUTO_BACKUP_INTERVAL_HOURS, 24).first().toString()

            settings
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.w(TAG, "读取DataStore设置失败，使用空设置", e)
            emptyMap()
        }
    }

    /**
     * 恢复DataStore设置
     * Restore DataStore settings from a key-value map
     *
     * @param settings 设置键值对Map
     */
    private suspend fun restoreDataStoreSettings(settings: Map<String, String>) {
        try {
            for ((key, value) in settings) {
                when (key) {
                    "exclude_system_apps" ->
                        dataStoreManager.setBooleanValue(key, value.toBooleanStrictOrNull() ?: true)
                    "show_notifications" ->
                        dataStoreManager.setBooleanValue(key, value.toBooleanStrictOrNull() ?: true)
                    "auto_start_profile" ->
                        dataStoreManager.setBooleanValue(key, value.toBooleanStrictOrNull() ?: false)
                    "theme_mode" ->
                        dataStoreManager.setIntValue(key, value.toIntOrNull() ?: 0)
                    "auto_backup_enabled" ->
                        dataStoreManager.setBooleanValue(key, value.toBooleanStrictOrNull() ?: false)
                    "auto_backup_interval_hours" ->
                        dataStoreManager.setIntValue(key, value.toIntOrNull() ?: 24)
                    else -> {
                        // 对于未知键，作为字符串存储
                        dataStoreManager.setStringValue(key, value)
                    }
                }
            }
            ApplicationLoggingServiceManager.i(TAG, "DataStore设置已恢复，共 ${settings.size} 项")
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.w(TAG, "恢复DataStore设置部分失败", e)
        }
    }

    /**
     * 清理超出数量限制的自动备份
     * Clean up old auto-backup files that exceed the maximum count
     *
     * 保留最新的MAX_AUTO_BACKUP_COUNT个自动备份，删除其余的
     */
    private suspend fun cleanupOldAutoBackups() {
        try {
            val autoBackupFiles = backupDir.listFiles { file ->
                file.isFile &&
                        file.name.endsWith(BACKUP_FILE_EXTENSION) &&
                        file.name.startsWith("auto_backup")
            } ?: return

            if (autoBackupFiles.size <= MAX_AUTO_BACKUP_COUNT) {
                return
            }

            // 按修改时间排序，最旧的在前
            val sortedFiles = autoBackupFiles.sortedBy { it.lastModified() }
            val filesToDelete = sortedFiles.size - MAX_AUTO_BACKUP_COUNT

            for (i in 0 until filesToDelete) {
                val deleted = sortedFiles[i].delete()
                if (deleted) {
                    ApplicationLoggingServiceManager.i(
                        TAG,
                        "已清理旧自动备份: ${sortedFiles[i].name}"
                    )
                }
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.w(TAG, "清理旧自动备份失败", e)
        }
    }

    /**
     * 获取应用版本号
     * Get the application version name
     *
     * @return 版本号字符串，如 "2.2.0"
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * 格式化文件大小为人类可读的字符串
     * Format file size to a human-readable string
     *
     * @param bytes 字节数
     * @return 格式化后的字符串，如 "1.5 MB"
     */
    private fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.1f GB", gb)
    }
}
