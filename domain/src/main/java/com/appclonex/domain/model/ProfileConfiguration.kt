package com.appclonex.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * 配置文件导出数据模型
 * 
 * 包含完整的配置文件设置和克隆应用列表
 * 用于导入导出功能
 */
@Serializable
data class ProfileConfiguration(
    @SerialName("version")
    val version: Int = 1,
    
    @SerialName("export_time")
    val exportTime: Long,
    
    @SerialName("profile")
    val profile: ProfileData,
    
    @SerialName("cloned_apps")
    val clonedApps: List<ClonedAppData>,
    
    @SerialName("settings")
    val settings: SettingsData = SettingsData()
)

@Serializable
data class ProfileData(
    @SerialName("user_id")
    val userId: Int,
    
    @SerialName("profile_name")
    val profileName: String,
    
    @SerialName("serial_number")
    val serialNumber: Int = 0,
    
    @SerialName("notes")
    val notes: String? = null
)

@Serializable
data class ClonedAppData(
    @SerialName("package_name")
    val packageName: String,
    
    @SerialName("app_name")
    val appName: String,
    
    @SerialName("custom_name")
    val customName: String? = null,
    
    @SerialName("clone_index")
    val cloneIndex: Int = 1,
    
    @SerialName("is_enabled")
    val isEnabled: Boolean = true,
    
    @SerialName("notes")
    val notes: String? = null
)

@Serializable
data class SettingsData(
    @SerialName("exclude_system_apps")
    val excludeSystemApps: Boolean = false,
    
    @SerialName("theme_mode")
    val themeMode: String = "SYSTEM",
    
    @SerialName("auto_freeze_enabled")
    val autoFreezeEnabled: Boolean = false
)
