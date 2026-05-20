/*
 * AppCloneX - 智能通知中心模型
 * AppCloneX - Smart Notification Center Models
 *
 * Copyright (c) 2024 AppCloneX Team. All rights reserved.
 *
 * 定义通知系统的核心数据模型，包括通知实体、分组、筛选条件、
 * 偏好设置和统计摘要等。
 * Defines core data models for the notification system, including notification entities,
 * groups, filter criteria, preferences, and statistical summaries.
 */
package com.appclonex.domain.model

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

// ==================== 通知ID / Notification ID ====================

/**
 * 通知唯一标识符
 * Unique notification identifier
 *
 * 使用原子计数器生成全局唯一的递增ID，保证线程安全。
 * Uses an atomic counter to generate globally unique incremental IDs, ensuring thread safety.
 *
 * @property value 通知ID的原始值 / Raw value of the notification ID
 */
@Serializable(with = AppNotificationIdSerializer::class)
value class AppNotificationId(val value: Long) {
    companion object {
        /** 原子计数器，用于生成唯一ID / Atomic counter for generating unique IDs */
        private val counter = AtomicLong(1)

        /**
         * 生成一个新的通知ID
         * Generate a new notification ID
         *
         * @return 新的通知ID / A new notification ID
         */
        fun generate(): AppNotificationId = AppNotificationId(counter.getAndIncrement())

        /**
         * 初始化计数器，用于在应用重启后恢复计数器状态
         * Initialize the counter to a specific value, used to restore counter state after app restart
         *
         * @param startValue 计数器的起始值，通常为已有通知最大ID + 1
         *                   The starting value for the counter, typically max existing notification ID + 1
         */
        fun initCounter(startValue: Long) {
            counter.set(startValue.coerceAtLeast(1))
        }
    }
}

/**
 * AppNotificationId 的序列化器
 * Serializer for AppNotificationId
 *
 * 由于 value class 需要自定义序列化器来正确处理。
 * Value classes require custom serializers for proper handling.
 */
@kotlinx.serialization.Serializer(forClass = AppNotificationId::class)
object AppNotificationIdSerializer : kotlinx.serialization.KSerializer<AppNotificationId> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveKind.LONG.let {
        kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("AppNotificationId", it)
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: AppNotificationId) {
        encoder.encodeLong(value.value)
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): AppNotificationId {
        return AppNotificationId(decoder.decodeLong())
    }
}

// ==================== 通知优先级 / Notification Priority ====================

/**
 * 通知优先级枚举
 * Notification priority enum
 *
 * 定义通知的紧急程度，从低到高依次为 LOW、NORMAL、HIGH、URGENT。
 * Defines the urgency level of notifications, from low to high: LOW, NORMAL, HIGH, URGENT.
 */
@Serializable
enum class NotificationPriority {
    /** 低优先级 - 一般信息类通知 / Low priority - informational notifications */
    LOW,

    /** 普通优先级 - 默认级别 / Normal priority - default level */
    NORMAL,

    /** 高优先级 - 需要用户关注 / High priority - requires user attention */
    HIGH,

    /** 紧急优先级 - 需要立即处理 / Urgent priority - requires immediate action */
    URGENT
}

// ==================== 通知分类 / Notification Category ====================

/**
 * 通知分类枚举
 * Notification category enum
 *
 * 将通知按功能模块分类，便于筛选和分组管理。
 * Categorizes notifications by functional module for filtering and group management.
 */
@Serializable
enum class NotificationCategory {
    /** 克隆操作相关 / Clone operation related */
    CLONE,

    /** 备份操作相关 / Backup operation related */
    BACKUP,

    /** 冻结操作相关 / Freeze operation related */
    FREEZE,

    /** 性能监控相关 / Performance monitoring related */
    PERFORMANCE,

    /** 安全相关 / Security related */
    SECURITY,

    /** 系统通知 / System notifications */
    SYSTEM,

    /** 更新通知 / Update notifications */
    UPDATE,

    /** 插件相关 / Plugin related */
    PLUGIN,

    /** 自定义通知 / Custom notifications */
    CUSTOM
}

// ==================== 通知状态 / Notification Status ====================

/**
 * 通知状态枚举
 * Notification status enum
 *
 * 跟踪通知的生命周期：待发送 -> 已送达 -> 已读/已忽略/已过期。
 * Tracks the notification lifecycle: PENDING -> DELIVERED -> READ/DISMISSED/EXPIRED.
 */
@Serializable
enum class NotificationStatus {
    /** 待发送 - 通知已创建但尚未送达 / Pending - notification created but not yet delivered */
    PENDING,

    /** 已送达 - 通知已展示给用户 / Delivered - notification shown to user */
    DELIVERED,

    /** 已读 - 用户已查看该通知 / Read - user has viewed the notification */
    READ,

    /** 已忽略 - 用户手动关闭了该通知 / Dismissed - user manually closed the notification */
    DISMISSED,

    /** 已过期 - 通知已超过有效期 / Expired - notification has exceeded its validity period */
    EXPIRED
}

// ==================== 通知实体 / Notification Entity ====================

/**
 * 应用通知数据类
 * Application notification data class
 *
 * 表示一条完整的应用内通知，包含标题、内容、分类、优先级、
 * 状态、时间戳以及可选的操作链接和分组信息。
 * Represents a complete in-app notification with title, content, category, priority,
 * status, timestamps, and optional action links and group information.
 *
 * @property id 通知唯一标识 / Unique notification identifier
 * @property title 通知标题 / Notification title
 * @property content 通知正文内容 / Notification body content
 * @property category 通知分类 / Notification category
 * @property priority 通知优先级 / Notification priority
 * @property status 通知状态 / Notification status
 * @property createdAt 创建时间戳（毫秒） / Creation timestamp in milliseconds
 * @property expireAt 过期时间戳（毫秒），null表示永不过期 / Expiration timestamp, null means never expires
 * @property actionUrl 操作链接URL / Action URL for the notification
 * @property actionLabel 操作按钮文本 / Action button label text
 * @property iconRes 图标资源标识 / Icon resource identifier
 * @property groupId 分组ID，用于将相关通知聚合 / Group ID for aggregating related notifications
 * @property metadata 附加元数据键值对 / Additional metadata key-value pairs
 */
@Serializable
data class AppNotification(
    val id: AppNotificationId = AppNotificationId.generate(),
    val title: String,
    val content: String,
    val category: NotificationCategory = NotificationCategory.SYSTEM,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val status: NotificationStatus = NotificationStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val expireAt: Long? = null,
    val actionUrl: String? = null,
    val actionLabel: String? = null,
    val iconRes: String? = null,
    val groupId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

// ==================== 通知分组 / Notification Group ====================

/**
 * 通知分组数据类
 * Notification group data class
 *
 * 将相关通知聚合为一个分组，显示分组名称、图标、
 * 通知数量和最新一条通知的预览。
 * Aggregates related notifications into a group, displaying group name, icon,
 * notification count, and a preview of the latest notification.
 *
 * @property id 分组唯一标识 / Unique group identifier
 * @property name 分组显示名称 / Group display name
 * @property icon 分组图标资源标识 / Group icon resource identifier
 * @property count 分组内通知总数 / Total notification count in the group
 * @property latestNotification 分组内最新的一条通知 / Latest notification in the group
 * @property unreadCount 分组内未读通知数量 / Unread notification count in the group
 */
@Serializable
data class NotificationGroup(
    val id: String,
    val name: String,
    val icon: String? = null,
    val count: Int = 0,
    val latestNotification: AppNotification? = null,
    val unreadCount: Int = 0
)

// ==================== 通知筛选条件 / Notification Filter ====================

/**
 * 通知筛选条件数据类
 * Notification filter criteria data class
 *
 * 定义通知列表的筛选条件，支持按分类、优先级、状态、
 * 时间范围、分组和关键词进行过滤。
 * Defines filter criteria for notification lists, supporting filtering by category,
 * priority, status, time range, group, and keyword.
 *
 * @property categories 允许的通知分类集合，空集合表示不限制 / Allowed notification categories, empty means no restriction
 * @property priorities 允许的优先级集合，空集合表示不限制 / Allowed priorities, empty means no restriction
 * @property statuses 允许的状态集合，空集合表示不限制 / Allowed statuses, empty means no restriction
 * @property searchQuery 搜索关键词，用于模糊匹配标题和内容 / Search keyword for fuzzy matching title and content
 * @property fromDate 起始时间戳（毫秒），null表示不限制 / Start timestamp, null means no restriction
 * @property toDate 结束时间戳（毫秒），null表示不限制 / End timestamp, null means no restriction
 * @property groupId 分组ID筛选，null表示不限制 / Group ID filter, null means no restriction
 */
@Serializable
data class NotificationFilter(
    val categories: Set<NotificationCategory> = emptySet(),
    val priorities: Set<NotificationPriority> = emptySet(),
    val statuses: Set<NotificationStatus> = emptySet(),
    val searchQuery: String = "",
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val groupId: String? = null
)

// ==================== 通知偏好设置 / Notification Preferences ====================

/**
 * 通知偏好设置数据类
 * Notification preferences data class
 *
 * 管理用户的通知偏好，包括启用的分类、免打扰时段、
 * 每日上限和通知方式（声音、振动、LED）。
 * Manages user notification preferences, including enabled categories, quiet hours,
 * daily limits, and notification methods (sound, vibration, LED).
 *
 * @property enabledCategories 用户启用的通知分类 / User-enabled notification categories
 * @property quietHoursStart 免打扰开始时间（HH:mm格式），null表示不启用 / Quiet hours start time (HH:mm), null means disabled
 * @property quietHoursEnd 免打扰结束时间（HH:mm格式），null表示不启用 / Quiet hours end time (HH:mm), null means disabled
 * @property maxNotificationsPerDay 每日最大通知数量 / Maximum notifications per day
 * @property autoDismissAfter 自动忽略时间（毫秒），null表示不自动忽略 / Auto-dismiss time in ms, null means never
 * @property soundEnabled 是否启用通知声音 / Whether notification sound is enabled
 * @property vibrationEnabled 是否启用通知振动 / Whether notification vibration is enabled
 * @property ledEnabled 是否启用LED通知灯 / Whether LED notification light is enabled
 */
@Serializable
data class NotificationPreferences(
    val enabledCategories: Set<NotificationCategory> = NotificationCategory.entries.toSet(),
    val quietHoursStart: String? = null,
    val quietHoursEnd: String? = null,
    val maxNotificationsPerDay: Int = 100,
    val autoDismissAfter: Long? = null,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val ledEnabled: Boolean = false
)

// ==================== 通知统计摘要 / Notification Summary ====================

/**
 * 通知统计摘要数据类
 * Notification summary data class
 *
 * 提供通知中心的整体统计信息，包括总数、未读数、
 * 紧急数和按分类的分布情况。
 * Provides overall statistics for the notification center, including total count,
 * unread count, urgent count, and distribution by category.
 *
 * @property totalCount 通知总数 / Total notification count
 * @property unreadCount 未读通知数量 / Unread notification count
 * @property urgentCount 紧急通知数量 / Urgent notification count
 * @property byCategory 按分类统计的通知数量 / Notification count by category
 * @property latestNotification 最新的一条通知 / The latest notification
 */
@Serializable
data class NotificationSummary(
    val totalCount: Int = 0,
    val unreadCount: Int = 0,
    val urgentCount: Int = 0,
    val byCategory: Map<NotificationCategory, Int> = emptyMap(),
    val latestNotification: AppNotification? = null
)
