package com.appclonex.domain.model

import kotlinx.serialization.Serializable

/**
 * 云备份配置
 * 定义云备份的行为参数和同步策略
 *
 * @property provider 云存储提供商
 * @property autoSync 是否启用自动同步
 * @property syncIntervalHours 自动同步间隔（小时）
 * @property wifiOnly 是否仅在WiFi下同步
 * @property encryptBackup 是否加密备份
 * @property lastSyncTime 上次同步时间戳
 * @property lastSyncResult 上次同步结果
 */
@Serializable
data class CloudBackupConfig(
    val provider: CloudProvider = CloudProvider.LOCAL,
    val autoSync: Boolean = false,
    val syncIntervalHours: Int = 24,
    val wifiOnly: Boolean = true,
    val encryptBackup: Boolean = true,
    val lastSyncTime: Long = 0,
    val lastSyncResult: SyncResult = SyncResult.IDLE
)

/**
 * 云存储提供商枚举
 */
enum class CloudProvider {
    LOCAL,         // 本地存储
    GOOGLE_DRIVE,  // Google Drive
    WEBDAV,        // WebDAV
    SMB,           // SMB/CIFS
    NEXTCLOUD      // Nextcloud
}

/**
 * 同步结果枚举
 */
enum class SyncResult {
    IDLE,          // 空闲
    SUCCESS,       // 成功
    FAILED,        // 失败
    IN_PROGRESS    // 同步中
}

/**
 * 云备份信息
 * 描述远程备份的状态和元数据
 *
 * @property provider 云存储提供商
 * @property remotePath 远程路径
 * @property fileSize 文件大小（字节）
 * @property lastModified 最后修改时间戳
 * @property isAvailable 是否可用
 */
@Serializable
data class CloudBackupInfo(
    val provider: CloudProvider = CloudProvider.LOCAL,
    val remotePath: String = "",
    val fileSize: Long = 0,
    val lastModified: Long = 0,
    val isAvailable: Boolean = false
)

// 注意：GoogleDriveAuthState 和 SyncConflictInfo 已统一在 GoogleDriveModels.kt 中定义
// 此处不再重复定义，避免编译冲突
