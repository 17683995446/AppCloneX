package com.appclonex.domain.model

/**
 * 存储分析结果
 */
data class StorageAnalysisResult(
    val profileId: Int,
    val profileName: String,
    val totalBytes: Long,
    val appDataBytes: Long,
    val cacheBytes: Long,
    val mediaBytes: Long,
    val otherBytes: Long,
    val apps: List<AppStorageInfo>
) {
    val formattedTotalSize: String
        get() = formatSize(totalBytes)
    
    val formattedAppData: String
        get() = formatSize(appDataBytes)
    
    val formattedCache: String
        get() = formatSize(cacheBytes)
    
    val appDataPercent: Int
        get() = if (totalBytes > 0) ((appDataBytes * 100) / totalBytes).toInt() else 0
    
    val cachePercent: Int
        get() = if (totalBytes > 0) ((cacheBytes * 100) / totalBytes).toInt() else 0
    
    companion object {
        fun formatSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
                else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
            }
        }
    }
}

/**
 * 单个应用存储信息
 */
data class AppStorageInfo(
    val packageName: String,
    val appName: String,
    val totalBytes: Long,
    val dataBytes: Long,
    val cacheBytes: Long,
    val apkBytes: Long
) {
    val formattedTotalSize: String
        get() = StorageAnalysisResult.formatSize(totalBytes)
}

/**
 * 清理建议
 */
data class CleanupSuggestion(
    val type: CleanupType,
    val description: String,
    val recoverableBytes: Long,
    val risk: CleanupRisk
) {
    enum class CleanupType {
        CACHE,          // 缓存文件
        TEMP_FILES,     // 临时文件
        OLD_BACKUPS,    // 旧备份
        DUPLICATE_DATA, // 重复数据
        LOG_FILES       // 日志文件
    }
    
    enum class CleanupRisk {
        LOW,    // 低风险，可安全清理
        MEDIUM, // 中风险，可能影响功能
        HIGH    // 高风险，可能导致数据丢失
    }
    
    val formattedSize: String
        get() = StorageAnalysisResult.formatSize(recoverableBytes)
}
