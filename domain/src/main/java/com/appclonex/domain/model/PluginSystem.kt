/*
 * Copyright (c) 2024 AppCloneX Contributors
 *
 * 插件系统领域模型
 *
 * 定义插件系统的核心数据结构，包括插件标识、状态、权限、
 * 元数据、配置、运行时信息、依赖关系和清单等。
 * 遵循 Clean Architecture 原则，Domain 层不依赖任何外部框架。
 */
package com.appclonex.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

// ============================================================================
// 插件标识符
// ============================================================================

/**
 * 插件唯一标识符
 *
 * 使用 value class 包装 String 类型，提供类型安全的插件ID。
 * 可通过 [generate] 工厂方法自动生成唯一ID。
 *
 * @property value 插件ID的字符串值
 */
@Serializable
@JvmInline
value class PluginId(val value: String) {
    companion object {
        /**
         * 生成一个新的插件ID
         *
         * 格式为 "plugin_" + UUID，确保全局唯一性。
         *
         * @return 新生成的插件ID
         */
        fun generate(): PluginId = PluginId("plugin_${UUID.randomUUID()}")
    }
}

// ============================================================================
// 插件状态
// ============================================================================

/**
 * 插件生命周期状态枚举
 *
 * 描述插件在系统中的当前状态，用于状态管理和UI展示。
 */
@Serializable
enum class PluginState {
    /** 已禁用 - 插件已安装但未激活 */
    DISABLED,

    /** 已启用 - 插件正常运行中 */
    ENABLED,

    /** 已安装 - 插件已安装但尚未初始化 */
    INSTALLED,

    /** 错误状态 - 插件运行时发生错误 */
    ERROR,

    /** 更新中 - 插件正在执行更新操作 */
    UPDATING
}

// ============================================================================
// 插件权限
// ============================================================================

/**
 * 插件权限枚举
 *
 * 定义插件可请求的权限类型，用于权限管理和安全控制。
 * 每种权限对应插件可访问的系统功能或数据范围。
 */
@Serializable
enum class PluginPermission {
    /** 克隆应用访问权限 - 允许插件操作克隆应用 */
    CLONE_ACCESS,

    /** 冻结功能访问权限 - 允许插件使用应用冻结功能 */
    FREEZE_ACCESS,

    /** 备份访问权限 - 允许插件读取和创建备份 */
    BACKUP_ACCESS,

    /** 网络访问权限 - 允许插件进行网络通信 */
    NETWORK_ACCESS,

    /** 存储访问权限 - 允许插件读写设备存储 */
    STORAGE_ACCESS,

    /** 通知访问权限 - 允许插件发送系统通知 */
    NOTIFICATION_ACCESS,

    /** 系统级访问权限 - 允许插件执行系统级操作 */
    SYSTEM_ACCESS
}

// ============================================================================
// 插件元数据
// ============================================================================

/**
 * 插件元数据
 *
 * 包含插件的基本描述信息，用于插件展示、版本管理和兼容性检查。
 *
 * @property id 插件唯一标识符
 * @property name 插件显示名称
 * @property version 插件版本号，遵循语义化版本规范 (SemVer)
 * @property description 插件功能描述
 * @property author 插件作者
 * @property minAppVersion 最低兼容的应用版本号
 * @property permissions 插件请求的权限列表
 * @property iconUrl 插件图标URL，可选
 * @property homepageUrl 插件主页URL，可选
 * @property installDate 安装时间戳（毫秒）
 * @property lastUpdateDate 最后更新时间戳（毫秒）
 */
@Serializable
data class PluginMetadata(
    val id: PluginId,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val minAppVersion: String,
    val permissions: List<PluginPermission> = emptyList(),
    val iconUrl: String? = null,
    val homepageUrl: String? = null,
    val installDate: Long = System.currentTimeMillis(),
    val lastUpdateDate: Long = System.currentTimeMillis()
)

// ============================================================================
// 插件配置
// ============================================================================

/**
 * 插件用户配置
 *
 * 存储用户对插件的个性化设置，以键值对形式保存。
 *
 * @property pluginId 所属插件ID
 * @property settings 配置项键值对
 * @property isEnabled 是否启用
 */
@Serializable
data class PluginConfig(
    val pluginId: PluginId,
    val settings: Map<String, String> = emptyMap(),
    val isEnabled: Boolean = true
)

// ============================================================================
// 插件运行时信息
// ============================================================================

/**
 * 插件运行时信息
 *
 * 记录插件在运行时的资源使用情况和状态信息，
 * 用于性能监控和问题诊断。
 *
 * @property pluginId 所属插件ID
 * @property memoryUsage 内存使用量（字节）
 * @property cpuTime CPU累计使用时间（毫秒）
 * @property isRunning 是否正在运行
 * @property lastError 最后一次错误信息，无错误时为null
 * @property startTime 启动时间戳（毫秒）
 */
@Serializable
data class PluginRuntimeInfo(
    val pluginId: PluginId,
    val memoryUsage: Long = 0L,
    val cpuTime: Long = 0L,
    val isRunning: Boolean = false,
    val lastError: String? = null,
    val startTime: Long = 0L
)

// ============================================================================
// 插件依赖
// ============================================================================

/**
 * 插件依赖声明
 *
 * 描述插件对其他插件的版本依赖关系。
 *
 * @property pluginId 依赖的插件ID
 * @property minVersion 最低兼容版本
 * @property maxVersion 最高兼容版本，null表示不限制上限
 */
@Serializable
data class PluginDependency(
    val pluginId: String,
    val minVersion: String,
    val maxVersion: String? = null
)

// ============================================================================
// 配置字段类型
// ============================================================================

/**
 * 配置字段类型枚举
 *
 * 定义插件配置项支持的数据类型，用于配置表单的渲染和验证。
 */
@Serializable
enum class ConfigFieldType {
    /** 文本输入 */
    TEXT,

    /** 数字输入 */
    NUMBER,

    /** 布尔开关 */
    BOOLEAN,

    /** 单选下拉 */
    SELECT,

    /** 多选列表 */
    MULTISELECT,

    /** 密码输入 */
    PASSWORD
}

// ============================================================================
// 配置字段定义
// ============================================================================

/**
 * 插件配置字段定义
 *
 * 描述插件配置项的元信息，用于动态生成配置界面。
 *
 * @property key 配置项键名
 * @property label 配置项显示标签
 * @property type 配置项数据类型
 * @property defaultValue 默认值，可选
 * @property description 配置项说明，可选
 * @property required 是否为必填项
 */
@Serializable
data class ConfigField(
    val key: String,
    val label: String,
    val type: ConfigFieldType,
    val defaultValue: String? = null,
    val description: String? = null,
    val required: Boolean = false
)

// ============================================================================
// 插件清单
// ============================================================================

/**
 * 插件清单
 *
 * 插件的完整描述文件，包含元数据、依赖关系和配置 schema。
 * 在插件安装时解析和验证。
 *
 * @property metadata 插件元数据
 * @property dependencies 插件依赖列表
 * @property configSchema 配置字段定义映射
 */
@Serializable
data class PluginManifest(
    val metadata: PluginMetadata,
    val dependencies: List<PluginDependency> = emptyList(),
    val configSchema: Map<String, ConfigField> = emptyMap()
)

// ============================================================================
// 插件安装结果
// ============================================================================

/**
 * 插件安装结果
 *
 * 封装插件安装操作的结果，包含成功/失败状态和冲突信息。
 *
 * @property success 是否安装成功
 * @property pluginId 安装成功时的插件ID
 * @property errorMessage 安装失败时的错误信息
 * @property conflicts 冲突的插件ID列表
 */
@Serializable
data class PluginInstallResult(
    val success: Boolean,
    val pluginId: PluginId? = null,
    val errorMessage: String? = null,
    val conflicts: List<PluginId> = emptyList()
)

// ============================================================================
// 插件搜索结果
// ============================================================================

/**
 * 插件搜索结果
 *
 * 封装插件搜索操作的分页结果。
 *
 * @property plugins 匹配的插件元数据列表
 * @property totalCount 总匹配数量
 * @property page 当前页码
 * @property pageSize 每页数量
 */
@Serializable
data class PluginSearchResult(
    val plugins: List<PluginMetadata> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20
)
