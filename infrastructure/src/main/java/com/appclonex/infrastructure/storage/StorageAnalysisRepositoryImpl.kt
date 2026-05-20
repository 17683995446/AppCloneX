package com.appclonex.infrastructure.storage

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.os.storage.StorageManager
import com.appclonex.domain.model.*
import com.appclonex.domain.repository.StorageAnalysisRepository
import com.appclonex.domain.repository.UserProfileRepository
import com.appclonex.domain.repository.ClonedApplicationRepository
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import com.github.michaelbull.result.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 存储分析仓库实现
 */
@Singleton
class StorageAnalysisRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userProfileRepository: UserProfileRepository,
    private val clonedApplicationRepository: ClonedApplicationRepository
) : StorageAnalysisRepository {
    
    private val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
    private val packageManager = context.packageManager
    private val storageTrendFlow = MutableStateFlow<List<StorageAnalysisResult>>(emptyList())
    
    override suspend fun analyzeProfileStorage(profileId: Int): OperationResult<StorageAnalysisResult> {
        return withContext(Dispatchers.IO) {
            try {
                val profile = userProfileRepository.getProfileById(profileId)
                    ?: return@withContext Err(OperationError.ApplicationNotFound("配置文件不存在"))
                
                val clonedApps = clonedApplicationRepository.getClonedAppsFlow(profileId)
                val appStorageInfos = mutableListOf<AppStorageInfo>()
                
                var totalAppData = 0L
                var totalCache = 0L
                var totalMedia = 0L
                
                // 获取克隆应用列表（一次性获取）
                val appsList = mutableListOf<ClonedApplicationInformationModel>()
                clonedApps.collect { apps ->
                    appsList.clear()
                    appsList.addAll(apps)
                }
                
                for (app in appsList) {
                    try {
                        val storageInfo = getAppStorageInfo(app.packageName, profileId)
                        if (storageInfo != null) {
                            appStorageInfos.add(storageInfo)
                            totalAppData += storageInfo.dataBytes
                            totalCache += storageInfo.cacheBytes
                        }
                    } catch (e: Exception) {
                        ApplicationLoggingServiceManager.w("StorageAnalysis", "无法获取应用存储: ${app.packageName}", e)
                    }
                }
                
                val result = StorageAnalysisResult(
                    profileId = profileId,
                    profileName = profile.profileName,
                    totalBytes = totalAppData + totalCache + totalMedia,
                    appDataBytes = totalAppData,
                    cacheBytes = totalCache,
                    mediaBytes = totalMedia,
                    otherBytes = 0,
                    apps = appStorageInfos.sortedByDescending { it.totalBytes }
                )
                
                Ok(result)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e("StorageAnalysis", "分析存储失败", e)
                Err(OperationError.UnknownError(e))
            }
        }
    }
    
    override suspend fun analyzeAllStorage(): OperationResult<List<StorageAnalysisResult>> {
        return withContext(Dispatchers.IO) {
            try {
                val profiles = userProfileRepository.getAllProfilesWithInfo()
                val results = mutableListOf<StorageAnalysisResult>()
                
                for (profileInfo in profiles) {
                    analyzeProfileStorage(profileInfo.profile.userId)
                        .onSuccess { results.add(it) }
                }
                
                Ok(results)
            } catch (e: Exception) {
                Err(OperationError.UnknownError(e))
            }
        }
    }
    
    override suspend fun getCleanupSuggestions(profileId: Int): OperationResult<List<CleanupSuggestion>> {
        return withContext(Dispatchers.IO) {
            try {
                val suggestions = mutableListOf<CleanupSuggestion>()
                
                // 分析缓存
                val storageResult = analyzeProfileStorage(profileId).getOrNull()
                if (storageResult != null && storageResult.cacheBytes > 10 * 1024 * 1024) {
                    suggestions.add(
                        CleanupSuggestion(
                            type = CleanupSuggestion.CleanupType.CACHE,
                            description = "清理应用缓存文件，可释放存储空间",
                            recoverableBytes = storageResult.cacheBytes,
                            risk = CleanupSuggestion.CleanupRisk.LOW
                        )
                    )
                }
                
                // 分析临时文件
                val tempFilesSize = estimateTempFilesSize(profileId)
                if (tempFilesSize > 5 * 1024 * 1024) {
                    suggestions.add(
                        CleanupSuggestion(
                            type = CleanupSuggestion.CleanupType.TEMP_FILES,
                            description = "清理临时文件和下载缓存",
                            recoverableBytes = tempFilesSize,
                            risk = CleanupSuggestion.CleanupRisk.LOW
                        )
                    )
                }
                
                Ok(suggestions)
            } catch (e: Exception) {
                Err(OperationError.UnknownError(e))
            }
        }
    }
    
    override suspend fun performCleanup(
        profileId: Int,
        types: List<CleanupSuggestion.CleanupType>
    ): OperationResult<Long> {
        return withContext(Dispatchers.IO) {
            try {
                var cleanedBytes = 0L

                for (type in types) {
                    when (type) {
                        CleanupSuggestion.CleanupType.CACHE -> {
                            cleanedBytes += cleanCache(profileId)
                        }
                        CleanupSuggestion.CleanupType.TEMP_FILES -> {
                            cleanedBytes += cleanTempFiles(profileId)
                        }
                        CleanupSuggestion.CleanupType.OLD_BACKUPS -> {
                            cleanedBytes += cleanOldBackups(profileId)
                        }
                        CleanupSuggestion.CleanupType.DUPLICATE_DATA -> {
                            cleanedBytes += cleanDuplicateData(profileId)
                        }
                        CleanupSuggestion.CleanupType.LOG_FILES -> {
                            cleanedBytes += cleanLogFiles(profileId)
                        }
                    }
                }

                Ok(cleanedBytes)
            } catch (e: Exception) {
                Err(OperationError.UnknownError(e))
            }
        }
    }
    
    override fun getStorageTrend(): Flow<List<StorageAnalysisResult>> {
        return storageTrendFlow
    }

    /**
     * 记录存储快照到趋势数据中
     * 应由定时任务周期性调用
     */
    suspend fun recordStorageSnapshot() {
        try {
            val currentAnalysis = analyzeAllStorage().getOrNull() ?: return
            val trend = storageTrendFlow.value.toMutableList()
            trend.addAll(currentAnalysis)
            // 仅保留最近 30 条记录
            while (trend.size > 30) {
                trend.removeAt(0)
            }
            storageTrendFlow.value = trend
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e("StorageAnalysis", "记录存储快照失败", e)
        }
    }
    
    private fun getAppStorageInfo(packageName: String, userId: Int): AppStorageInfo? {
        return try {
            val storageStatsManager = this.storageStatsManager ?: return null

            // 使用正确的 UserHandle 替代 Process.myUserHandle()
            val userHandle = try {
                val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
                UserHandle.getUserHandleForUid(UserHandle.getUid(userId, 0))
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.w("StorageAnalysis", "无法为 userId $userId 获取 UserHandle，回退到当前用户", e)
                Process.myUserHandle()
            }

            val storageStats: StorageStats = storageStatsManager.queryStatsForPackage(
                StorageManager.UUID_DEFAULT,
                packageName,
                userHandle
            )
            
            AppStorageInfo(
                packageName = packageName,
                appName = getAppName(packageName),
                totalBytes = storageStats.appBytes + storageStats.dataBytes + storageStats.cacheBytes,
                dataBytes = storageStats.dataBytes,
                cacheBytes = storageStats.cacheBytes,
                apkBytes = storageStats.appBytes
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
    
    private fun estimateTempFilesSize(userId: Int): Long {
        var totalSize = 0L
        val tempDirs = listOfNotNull(
            File(context.cacheDir, "temp_$userId"),
            File(context.filesDir, "temp_$userId"),
            context.externalCacheDir?.let { File(it, "temp_$userId") }
        )
        for (dir in tempDirs) {
            if (dir.exists()) {
                dir.walkTopDown().forEach { file ->
                    if (file.isFile) totalSize += file.length()
                }
            }
        }
        return totalSize
    }

    private suspend fun cleanCache(userId: Int): Long {
        return withContext(Dispatchers.IO) {
            var cleaned = 0L
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                cacheDir.walkTopDown().forEach { file ->
                    if (file.isFile && (file.name.endsWith(".tmp") || file.name.endsWith(".cache"))) {
                        val size = file.length()
                        if (file.delete()) cleaned += size
                    }
                }
            }
            context.externalCacheDir?.let { externalCache ->
                if (externalCache.exists()) {
                    externalCache.walkTopDown().forEach { file ->
                        if (file.isFile && (file.name.endsWith(".tmp") || file.name.endsWith(".cache"))) {
                            val size = file.length()
                            if (file.delete()) cleaned += size
                        }
                    }
                }
            }
            cleaned
        }
    }

    private suspend fun cleanTempFiles(userId: Int): Long {
        return withContext(Dispatchers.IO) {
            var cleaned = 0L
            val tempDirs = listOfNotNull(
                File(context.cacheDir, "temp_$userId"),
                File(context.filesDir, "temp_$userId"),
                context.externalCacheDir?.let { File(it, "temp_$userId") }
            )
            for (dir in tempDirs) {
                if (dir.exists()) {
                    dir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val size = file.length()
                            if (file.delete()) cleaned += size
                        }
                    }
                    dir.deleteRecursively()
                }
            }
            cleaned
        }
    }

    private suspend fun cleanOldBackups(userId: Int): Long {
        return withContext(Dispatchers.IO) {
            var cleaned = 0L
            val backupDir = File(context.filesDir, "backups/user_$userId")
            if (backupDir.exists()) {
                val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
                backupDir.walkTopDown().forEach { file ->
                    if (file.isFile && file.lastModified() < sevenDaysAgo) {
                        val size = file.length()
                        if (file.delete()) cleaned += size
                    }
                }
            }
            cleaned
        }
    }

    private suspend fun cleanDuplicateData(userId: Int): Long {
        return withContext(Dispatchers.IO) {
            var cleaned = 0L
            val dataDir = File(context.filesDir, "user_$userId")
            if (dataDir.exists()) {
                // 使用 SHA-256 哈希值进行文件去重，而非仅比较文件大小
                val seenFiles = mutableMapOf<String, MutableList<File>>()
                dataDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        try {
                            val hash = calculateFileHash(file)
                            seenFiles.getOrPut(hash) { mutableListOf() }.add(file)
                        } catch (e: Exception) {
                            ApplicationLoggingServiceManager.w("StorageAnalysis", "无法计算文件哈希: ${file.absolutePath}", e)
                        }
                    }
                }
                // 仅清理哈希完全相同且文件名相似的文件（保守策略）
                for ((_, files) in seenFiles) {
                    if (files.size > 1) {
                        val sorted = files.sortedByDescending { it.lastModified() }
                        val newest = sorted.first()
                        for (file in sorted.drop(1)) {
                            // 仅删除文件名相似的重复文件（不含扩展名部分相同）
                            val newestBaseName = newest.nameWithoutExtension
                            val fileBaseName = file.nameWithoutExtension
                            if (newestBaseName == fileBaseName ||
                                newestBaseName.contains(fileBaseName) ||
                                fileBaseName.contains(newestBaseName)) {
                                val size = file.length()
                                if (file.delete()) cleaned += size
                            }
                        }
                    }
                }
            }
            cleaned
        }
    }

    private fun calculateFileHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private suspend fun cleanLogFiles(userId: Int): Long {
        return withContext(Dispatchers.IO) {
            var cleaned = 0L
            val logDirs = listOfNotNull(
                File(context.filesDir, "logs/user_$userId"),
                context.externalCacheDir?.let { File(it, "logs/user_$userId") }
            )
            for (logDir in logDirs) {
                if (logDir.exists()) {
                    logDir.walkTopDown().forEach { file ->
                        if (file.isFile && (file.name.endsWith(".log") || file.name.endsWith(".trace"))) {
                            val size = file.length()
                            if (file.delete()) cleaned += size
                        }
                    }
                }
            }
            cleaned
        }
    }
}
