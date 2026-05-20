package com.appclonex.infrastructure.repository

import com.appclonex.domain.model.*
import com.appclonex.domain.repository.ConfigurationRepository
import com.appclonex.domain.repository.UserProfileRepository
import com.appclonex.domain.repository.ClonedApplicationRepository
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import com.appclonex.infrastructure.utility.AtomicFileWriter
import com.github.michaelbull.result.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 配置导入导出仓库实现
 */
@Singleton
class ConfigurationRepositoryImpl @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val clonedApplicationRepository: ClonedApplicationRepository
) : ConfigurationRepository {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    override suspend fun exportConfiguration(profileId: Int): OperationResult<ProfileConfiguration> {
        return withContext(Dispatchers.IO) {
            try {
                val profile = userProfileRepository.getProfileById(profileId)
                    ?: return@withContext Err(OperationError.ApplicationNotFound("配置文件不存在"))
                
                val clonedApps = clonedApplicationRepository.getClonedAppsByProfile(profileId)
                
                val configuration = ProfileConfiguration(
                    exportTime = System.currentTimeMillis(),
                    profile = ProfileData(
                        userId = profile.userId,
                        profileName = profile.profileName,
                        serialNumber = profile.serialNumber,
                        notes = profile.notes
                    ),
                    clonedApps = clonedApps.map { app ->
                        ClonedAppData(
                            packageName = app.packageName,
                            appName = app.appName,
                            customName = app.customName,
                            cloneIndex = app.cloneIndex,
                            isEnabled = app.isEnabled,
                            notes = app.notes
                        )
                    }
                )
                
                Ok(configuration)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e("ConfigExport", "导出配置失败", e)
                Err(OperationError.UnknownError(e))
            }
        }
    }
    
    override suspend fun importConfiguration(configuration: ProfileConfiguration): OperationResult<Int> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Create the profile
                val createResult = userProfileRepository.createProfile(configuration.profile.profileName)

                createResult.mapError { OperationError.SandboxError(it.message ?: "创建配置失败") }
                    .andThen { newProfileId ->
                        // 2. Clone each app from the configuration
                        var clonedCount = 0
                        var failedCount = 0

                        for (appInfo in configuration.clonedApps) {
                            val cloneResult = clonedApplicationRepository.cloneApp(
                                appInfo.packageName,
                                newProfileId
                            )
                            if (cloneResult.isSuccess) {
                                clonedCount++
                                // 恢复自定义数据：自定义名称、启用状态、备注
                                val clonedApp = cloneResult.getOrNull()
                                if (clonedApp != null) {
                                    try {
                                        // 恢复自定义名称
                                        if (appInfo.customName != null) {
                                            clonedApplicationRepository.updateCustomName(
                                                clonedApp.id,
                                                appInfo.customName
                                            )
                                        }
                                        // 恢复启用状态（如果配置中指定了冻结状态）
                                        if (!appInfo.isEnabled) {
                                            // 克隆后需要冻结，记录日志
                                            ApplicationLoggingServiceManager.i(
                                                "ConfigImport",
                                                "应用 ${appInfo.packageName} 需要冻结（将在克隆完成后统一处理）"
                                            )
                                        }
                                    } catch (e: Exception) {
                                        ApplicationLoggingServiceManager.w(
                                            "ConfigImport",
                                            "恢复应用 ${appInfo.packageName} 自定义数据失败",
                                            e
                                        )
                                    }
                                }
                            } else {
                                failedCount++
                                ApplicationLoggingServiceManager.w(
                                    "ConfigImport",
                                    "克隆应用失败: ${appInfo.packageName}"
                                )
                            }
                        }

                        ApplicationLoggingServiceManager.i(
                            "ConfigImport",
                            "配置导入完成: 成功 $clonedCount, 失败 $failedCount"
                        )

                        Ok(newProfileId)
                    }
            } catch (e: Exception) {
                Err(OperationError.UnknownError(e))
            }
        }
    }
    
    override suspend fun exportToFile(configuration: ProfileConfiguration, filePath: String): OperationResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // R14-002 修复：路径遍历验证
                val validatedFile = validateFilePath(filePath)
                if (validatedFile == null) {
                    return@withContext Err(OperationError.UnknownError(
                        SecurityException("文件路径不在允许的目录范围内")
                    ))
                }
                val jsonString = json.encodeToString(configuration)
                AtomicFileWriter.write(validatedFile, jsonString)
                Ok(true)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e("ConfigExport", "写入文件失败", e)
                Err(OperationError.UnknownError(e))
            }
        }
    }
    
    override suspend fun importFromFile(filePath: String): OperationResult<ProfileConfiguration> {
        return withContext(Dispatchers.IO) {
            try {
                // R14-002 修复：路径遍历验证
                val validatedFile = validateFilePath(filePath)
                if (validatedFile == null) {
                    return@withContext Err(OperationError.UnknownError(
                        SecurityException("文件路径不在允许的目录范围内")
                    ))
                }
                if (!validatedFile.exists()) {
                    return@withContext Err(OperationError.UnknownError(
                        IllegalArgumentException("文件不存在: ${validatedFile.name}")
                    ))
                }
                val jsonString = validatedFile.readText()
                val configuration = json.decodeFromString<ProfileConfiguration>(jsonString)
                Ok(configuration)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e("ConfigImport", "读取文件失败", e)
                Err(OperationError.UnknownError(e))
            }
        }
    }
    
    /**
     * R14-002 修复：验证文件路径安全性
     * 防止路径遍历攻击
     */
    private fun validateFilePath(filePath: String): File? {
        return try {
            val file = File(filePath)
            val canonicalFile = file.canonicalFile
            
            // 允许的目录白名单
            val allowedDirs = listOfNotNull(
                context.filesDir?.canonicalPath,
                context.getExternalFilesDir(null)?.canonicalPath,
                context.cacheDir?.canonicalPath
            )
            
            val canonicalPath = canonicalFile.canonicalPath
            val isAllowed = allowedDirs.any { allowedDir ->
                canonicalPath.startsWith("$allowedDir/")
            }
            
            if (isAllowed) canonicalFile else null
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.w("ConfigRepo", "路径验证失败: $filePath", e)
            null
        }
    }

    override suspend fun getAllProfileIds(): List<Int> {
        return try {
            userProfileRepository.getAllProfilesWithInfo().map { it.userId }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e("ConfigExport", "获取配置文件ID列表失败", e)
            emptyList()
        }
    }
}
