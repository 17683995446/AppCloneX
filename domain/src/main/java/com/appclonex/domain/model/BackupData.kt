package com.appclonex.domain.model

import kotlinx.serialization.Serializable

/**
 * 完整备份数据模型
 * Complete backup data model
 *
 * 包含所有需要备份的数据：配置文件、克隆应用、设置、自动化规则、应用锁配置
 * Contains all data to be backed up: profiles, cloned apps, settings, automation rules, and app lock config
 */
@Serializable
data class BackupData(
    /** 备份格式版本 / Backup format version */
    val version: Int = 2,
    /** 备份时间戳（毫秒） / Backup timestamp in milliseconds */
    val timestamp: Long = System.currentTimeMillis(),
    /** 设备型号 / Device model */
    val deviceModel: String = "",
    /** 应用版本号 / Application version */
    val appVersion: String = "",
    /** 配置文件备份列表 / List of profile backup data */
    val profiles: List<ProfileBackupData> = emptyList(),
    /** 全局设置键值对 / Global settings key-value pairs */
    val settings: Map<String, String> = emptyMap(),
    /** 自动化规则列表 / List of automation rules */
    val automationRules: List<AutomationRule> = emptyList(),
    /** 应用锁配置 / App lock configuration */
    val appLockConfig: AppLockConfigBackup? = null
)

/**
 * 配置文件备份数据
 * Profile backup data
 *
 * 包含单个用户配置文件及其关联的克隆应用信息
 * Contains a single user profile and its associated cloned app information
 */
@Serializable
data class ProfileBackupData(
    /** 用户ID / User ID */
    val userId: Int,
    /** 配置文件名称 / Profile name */
    val profileName: String,
    /** 配置文件图标（Base64编码） / Profile icon (Base64 encoded) */
    val profileIcon: String? = null,
    /** 创建时间戳 / Creation timestamp */
    val createdAt: Long,
    /** 更新时间戳 / Last update timestamp */
    val updatedAt: Long,
    /** 是否为当前激活的配置 / Whether this is the currently active profile */
    val isActive: Boolean,
    /** 序列号 / Serial number */
    val serialNumber: Int,
    /** 备注 / Notes */
    val notes: String? = null,
    /** 该配置下的克隆应用列表 / List of cloned apps under this profile */
    val clonedApps: List<ClonedAppBackupData> = emptyList()
)

/**
 * 克隆应用备份数据
 * Cloned app backup data
 *
 * 记录单个克隆应用的完整信息，包括包名、版本、自定义名称和图标等
 * Records complete information for a single cloned app, including package name, version, custom name and icon
 */
@Serializable
data class ClonedAppBackupData(
    /** 应用包名 / Application package name */
    val packageName: String,
    /** 应用名称 / Application name */
    val appName: String,
    /** 应用图标（Base64编码） / App icon (Base64 encoded) */
    val appIcon: String? = null,
    /** 克隆索引号 / Clone index */
    val cloneIndex: Int = 1,
    /** 版本名称 / Version name (e.g. "1.2.3") */
    val versionName: String? = null,
    /** 版本代码 / Version code */
    val versionCode: Long = 0,
    /** 是否启用 / Whether the clone is enabled */
    val isEnabled: Boolean = true,
    /** 自定义名称 / Custom display name */
    val customName: String? = null,
    /** 自定义图标（Base64编码） / Custom icon (Base64 encoded) */
    val customIcon: String? = null,
    /** 备注 / Notes */
    val notes: String? = null
)

/**
 * 应用锁配置备份
 * App lock configuration backup
 *
 * 保存应用锁的类型、超时和最大尝试次数等设置
 * Stores app lock settings including type, timeout, and max attempts
 */
@Serializable
data class AppLockConfigBackup(
    /** 是否启用应用锁 / Whether app lock is enabled */
    val isEnabled: Boolean = false,
    /** 锁定类型：none / pin / biometric / pattern / Lock type */
    val lockType: String = "none",
    /** 自动锁定超时时间（分钟），0表示立即锁定 / Auto-lock timeout in minutes, 0 = immediate */
    val autoLockTimeout: Int = 0,
    /** 最大尝试次数 / Maximum unlock attempts */
    val maxAttempts: Int = 5
)

/**
 * 备份元数据
 * Backup metadata
 *
 * 用于展示备份列表和备份详情，不包含实际备份数据内容
 * Used for displaying backup list and details, does not contain actual backup data content
 */
@Serializable
data class BackupMetadata(
    /** 备份唯一标识 / Unique backup identifier */
    val id: String = "",
    /** 备份文件名 / Backup file name */
    val fileName: String = "",
    /** 备份文件大小（字节） / Backup file size in bytes */
    val fileSize: Long = 0,
    /** 备份时间戳 / Backup timestamp */
    val timestamp: Long = 0,
    /** 包含的配置文件数量 / Number of profiles included */
    val profileCount: Int = 0,
    /** 包含的克隆应用总数 / Total number of cloned apps included */
    val appCount: Int = 0,
    /** 创建备份时的设备型号 / Device model when backup was created */
    val deviceModel: String = "",
    /** 创建备份时的应用版本 / App version when backup was created */
    val appVersion: String = "",
    /** 是否为自动备份 / Whether this is an automatic backup */
    val isAutoBackup: Boolean = false
)
