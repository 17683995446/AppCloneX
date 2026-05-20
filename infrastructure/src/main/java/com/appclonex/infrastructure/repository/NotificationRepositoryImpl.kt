/*
 * AppCloneX - 通知仓库实现
 * AppCloneX - Notification Repository Implementation
 *
 * Copyright (c) 2024 AppCloneX Team. All rights reserved.
 *
 * 通知仓库的具体实现，使用 JSON 文件进行持久化存储。
 * Concrete implementation of the notification repository using JSON file persistence.
 */
package com.appclonex.infrastructure.repository

import android.content.Context
import android.util.Log
import com.appclonex.domain.model.AppNotification
import com.appclonex.domain.model.AppNotificationId
import com.appclonex.domain.model.NotificationCategory
import com.appclonex.domain.model.NotificationFilter
import com.appclonex.domain.model.NotificationGroup
import com.appclonex.domain.model.NotificationPreferences
import com.appclonex.domain.model.NotificationStatus
import com.appclonex.domain.model.NotificationSummary
import com.appclonex.domain.repository.NotificationRepository
import com.appclonex.infrastructure.utility.AtomicFileWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知仓库实现类
 * Notification repository implementation
 *
 * 使用 JSON 文件（notifications.json、notification_prefs.json）进行通知数据的持久化存储。
 * Uses JSON files (notifications.json, notification_prefs.json) for persistent notification storage.
 *
 * 功能特性 / Features:
 * - 线程安全：使用 Mutex 保证并发操作安全 / Thread-safe with Mutex for concurrent operations
 * - 实时更新：通过 StateFlow 暴露通知列表变化 / Real-time updates via StateFlow
 * - 自动过期：查询时自动清理过期通知 / Auto-expiration: cleans expired notifications on query
 * - 免打扰模式：检查静默时段，在免打扰期间不发送通知 / Quiet hours: suppresses notifications during quiet periods
 * - 每日上限：限制每日通知发送数量 / Daily limit: restricts daily notification count
 * - 分组聚合：按 groupId 聚合通知 / Group aggregation by groupId
 * - 模糊搜索：按标题和内容进行关键词搜索 / Fuzzy search by title and content
 *
 * @property context 应用上下文 / Application context
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationRepository {

    companion object {
        private const val TAG = "NotificationRepo"

        /** 通知数据文件名 / Notification data file name */
        private const val NOTIFICATIONS_FILE = "notifications.json"

        /** 偏好设置文件名 / Preferences file name */
        private const val PREFS_FILE = "notification_prefs.json"

        /** 默认最大存储通知数量 / Default maximum stored notification count */
        private const val MAX_STORED_NOTIFICATIONS = 500
    }

    /** JSON 序列化配置 / JSON serialization configuration */
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** 互斥锁，保证线程安全 / Mutex for thread safety */
    private val mutex = Mutex()

    /** 内存中的通知列表缓存 / In-memory notification list cache */
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())

    /** 内存中的偏好设置缓存 / In-memory preferences cache */
    private var cachedPreferences: NotificationPreferences? = null

    /** 当日已发送通知计数 / Daily sent notification count */
    private var todaySentCount: Int = 0

    /** 上次检查日期（用于重置每日计数） / Last check date for resetting daily count */
    private var lastCheckDate: String = ""

    init {
        // 初始化时加载数据 / Load data on initialization
        loadNotificationsFromFile()
        loadPreferences()
        resetDailyCountIfNeeded()

        // 初始化通知ID计数器，防止重启后ID冲突
        // Initialize notification ID counter to prevent ID conflicts after restart
        val maxId = _notifications.value.maxOfOrNull { it.id.value } ?: 0L
        if (maxId > 0) {
            AppNotificationId.initCounter(maxId + 1)
        }
    }

    // ==================== 发送通知 / Send Notifications ====================

    /**
     * 发送单条通知
     * Send a single notification
     *
     * 流程：分配ID -> 设置时间戳 -> 检查免打扰 -> 检查每日上限 -> 保存 -> 通知观察者
     * Flow: assign ID -> set timestamp -> check quiet hours -> check daily limit -> save -> notify observers
     */
    override suspend fun sendNotification(notification: AppNotification): Result<AppNotification> =
        mutex.withLock {
            sendNotificationInternal(notification)
        }

    /**
     * 内部发送通知实现（无锁版本）
     * 供 sendNotification 和 sendBatchNotifications 内部调用
     * 调用方必须确保已持有 mutex 锁
     */
    private suspend fun sendNotificationInternal(notification: AppNotification): Result<AppNotification> {
        try {
            // 检查免打扰模式 / Check quiet hours mode
            if (isInQuietHours()) {
                Log.d(TAG, "通知被免打扰模式拦截: ${notification.title}")
                return Result.failure(
                    IllegalStateException("当前处于免打扰时段，通知已被静默处理")
                )
            }

            // 检查每日通知上限 / Check daily notification limit
            val preferences = getPreferencesInternal()
            if (todaySentCount >= preferences.maxNotificationsPerDay) {
                Log.d(TAG, "已达到每日通知上限: ${preferences.maxNotificationsPerDay}")
                return Result.failure(
                    IllegalStateException("已达到每日通知上限 (${preferences.maxNotificationsPerDay})")
                )
            }

            // 检查分类是否启用 / Check if category is enabled
            if (notification.category !in preferences.enabledCategories) {
                Log.d(TAG, "通知分类已禁用: ${notification.category}")
                return Result.failure(
                    IllegalStateException("通知分类 ${notification.category} 已被禁用")
                )
            }

            // 创建带ID和时间戳的通知 / Create notification with ID and timestamp
            val now = System.currentTimeMillis()
            val newNotification = notification.copy(
                id = AppNotificationId.generate(),
                createdAt = now,
                status = NotificationStatus.DELIVERED
            )

            // 更新内存缓存和持久化 / Update in-memory cache and persist
            val currentList = _notifications.value.toMutableList()
            currentList.add(0, newNotification)

            // 限制最大存储数量 / Limit maximum stored count
            val trimmedList = if (currentList.size > MAX_STORED_NOTIFICATIONS) {
                currentList.take(MAX_STORED_NOTIFICATIONS)
            } else {
                currentList
            }

            _notifications.value = trimmedList
            saveNotificationsToFile(trimmedList)

            // 更新每日计数 / Update daily count
            todaySentCount++

            Log.d(TAG, "通知发送成功: ${newNotification.title} (ID: ${newNotification.id.value})")
            Result.success(newNotification)
        } catch (e: Exception) {
            Log.e(TAG, "发送通知失败", e)
            Result.failure(e)
        }
    }

    /**
     * 批量发送通知
     * Send notifications in batch
     *
     * 注意：此方法内部调用 sendNotificationInternal 而非 sendNotification，
     * 避免死锁（sendNotification 内部也使用 mutex.withLock）
     */
    override suspend fun sendBatchNotifications(
        notifications: List<AppNotification>
    ): Result<List<AppNotification>> = mutex.withLock {
        try {
            val results = mutableListOf<AppNotification>()
            val errors = mutableListOf<String>()

            for (notification in notifications) {
                sendNotificationInternal(notification)
                    .onSuccess { results.add(it) }
                    .onFailure { errors.add(it.message ?: "未知错误") }
            }

            if (results.isEmpty()) {
                Result.failure(IllegalStateException("所有通知发送失败: ${errors.joinToString()}"))
            } else {
                Log.d(TAG, "批量发送完成: 成功 ${results.size}, 失败 ${errors.size}")
                Result.success(results)
            }
        } catch (e: Exception) {
            Log.e(TAG, "批量发送通知失败", e)
            Result.failure(e)
        }
    }

    // ==================== 状态管理 / Status Management ====================

    /**
     * 标记指定通知为已读
     * Mark a specific notification as read
     */
    override suspend fun markAsRead(notificationId: AppNotificationId): Result<Unit> =
        mutex.withLock {
            try {
                updateNotificationStatus(notificationId, NotificationStatus.READ)
                Log.d(TAG, "通知已标记为已读: ${notificationId.value}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "标记已读失败", e)
                Result.failure(e)
            }
        }

    /**
     * 标记所有通知为已读
     * Mark all notifications as read
     */
    override suspend fun markAllAsRead(): Result<Unit> = mutex.withLock {
        try {
            val updatedList = _notifications.value.map { notification ->
                if (notification.status == NotificationStatus.DELIVERED ||
                    notification.status == NotificationStatus.PENDING
                ) {
                    notification.copy(status = NotificationStatus.READ)
                } else {
                    notification
                }
            }
            _notifications.value = updatedList
            saveNotificationsToFile(updatedList)
            Log.d(TAG, "所有通知已标记为已读")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "全部标记已读失败", e)
            Result.failure(e)
        }
    }

    /**
     * 忽略指定通知
     * Dismiss a specific notification
     */
    override suspend fun dismissNotification(notificationId: AppNotificationId): Result<Unit> =
        mutex.withLock {
            try {
                updateNotificationStatus(notificationId, NotificationStatus.DISMISSED)
                Log.d(TAG, "通知已忽略: ${notificationId.value}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "忽略通知失败", e)
                Result.failure(e)
            }
        }

    /**
     * 忽略指定分类的所有通知
     * Dismiss all notifications of a specific category
     */
    override suspend fun dismissAll(category: NotificationCategory?): Result<Unit> =
        mutex.withLock {
            try {
                val updatedList = _notifications.value.map { notification ->
                    if (category == null || notification.category == category) {
                        if (notification.status != NotificationStatus.DISMISSED &&
                            notification.status != NotificationStatus.READ
                        ) {
                            notification.copy(status = NotificationStatus.DISMISSED)
                        } else {
                            notification
                        }
                    } else {
                        notification
                    }
                }
                _notifications.value = updatedList
                saveNotificationsToFile(updatedList)
                Log.d(TAG, "已忽略${category?.name ?: "所有"}通知")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "批量忽略通知失败", e)
                Result.failure(e)
            }
        }

    // ==================== 删除操作 / Delete Operations ====================

    /**
     * 删除指定通知
     * Delete a specific notification
     */
    override suspend fun deleteNotification(notificationId: AppNotificationId): Result<Unit> =
        mutex.withLock {
            try {
                val updatedList = _notifications.value.filter { it.id != notificationId }
                _notifications.value = updatedList
                saveNotificationsToFile(updatedList)
                Log.d(TAG, "通知已删除: ${notificationId.value}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "删除通知失败", e)
                Result.failure(e)
            }
        }

    /**
     * 删除所有已过期的通知
     * Delete all expired notifications
     */
    override suspend fun deleteExpiredNotifications(): Result<Int> = mutex.withLock {
        try {
            val now = System.currentTimeMillis()
            val before = _notifications.value.size
            val updatedList = _notifications.value.filter { notification ->
                val expireAt = notification.expireAt
                expireAt == null || expireAt > now
            }
            val deletedCount = before - updatedList.size

            if (deletedCount > 0) {
                _notifications.value = updatedList
                saveNotificationsToFile(updatedList)
                Log.d(TAG, "已删除 $deletedCount 条过期通知")
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            Log.e(TAG, "删除过期通知失败", e)
            Result.failure(e)
        }
    }

    // ==================== 查询操作 / Query Operations ====================

    /**
     * 获取指定ID的通知
     * Get a notification by its ID
     */
    override suspend fun getNotification(notificationId: AppNotificationId): Result<AppNotification?> {
        // 先清理过期通知 / Clean expired notifications first
        cleanExpiredIfNeeded()
        val notification = _notifications.value.find { it.id == notificationId }
        return Result.success(notification)
    }

    /**
     * 根据筛选条件获取通知列表
     * Get notifications based on filter criteria
     */
    override suspend fun getNotifications(
        filter: NotificationFilter,
        limit: Int,
        offset: Int
    ): Result<List<AppNotification>> {
        // 先清理过期通知 / Clean expired notifications first
        cleanExpiredIfNeeded()

        val filtered = applyFilter(_notifications.value, filter)
        val result = filtered.drop(offset).take(limit)
        return Result.success(result)
    }

    /**
     * 获取未读通知数量
     * Get the count of unread notifications
     */
    override suspend fun getUnreadCount(): Result<Int> {
        cleanExpiredIfNeeded()
        val count = _notifications.value.count {
            it.status == NotificationStatus.DELIVERED || it.status == NotificationStatus.PENDING
        }
        return Result.success(count)
    }

    /**
     * 获取通知统计摘要
     * Get notification summary statistics
     */
    override suspend fun getSummary(): Result<NotificationSummary> {
        cleanExpiredIfNeeded()
        val notifications = _notifications.value

        val summary = NotificationSummary(
            totalCount = notifications.size,
            unreadCount = notifications.count {
                it.status == NotificationStatus.DELIVERED ||
                    it.status == NotificationStatus.PENDING
            },
            urgentCount = notifications.count {
                it.priority == com.appclonex.domain.model.NotificationPriority.URGENT &&
                    (it.status == NotificationStatus.DELIVERED ||
                        it.status == NotificationStatus.PENDING)
            },
            byCategory = notifications
                .groupBy { it.category }
                .mapValues { it.value.size },
            latestNotification = notifications.maxByOrNull { it.createdAt }
        )
        return Result.success(summary)
    }

    /**
     * 获取通知分组列表
     * Get notification groups
     */
    override suspend fun getGroups(): Result<List<NotificationGroup>> {
        cleanExpiredIfNeeded()
        val notifications = _notifications.value
        val grouped = notifications.mapNotNull { notification ->
            notification.groupId?.let { groupId -> groupId to notification }
        }.groupBy({ it.first }, { it.second })

        val groups = grouped.map { (groupId, groupNotifications) ->
            NotificationGroup(
                id = groupId,
                name = groupNotifications.firstOrNull()?.metadata?.get("groupName") ?: groupId,
                icon = groupNotifications.firstOrNull()?.iconRes,
                count = groupNotifications.size,
                latestNotification = groupNotifications.maxByOrNull { it.createdAt },
                unreadCount = groupNotifications.count {
                    it.status == NotificationStatus.DELIVERED ||
                        it.status == NotificationStatus.PENDING
                }
            )
        }.sortedByDescending { it.latestNotification?.createdAt ?: 0 }

        return Result.success(groups)
    }

    // ==================== 清理操作 / Cleanup Operations ====================

    /**
     * 清除所有通知
     * Clear all notifications
     */
    override suspend fun clearAllNotifications(): Result<Unit> = mutex.withLock {
        try {
            _notifications.value = emptyList()
            saveNotificationsToFile(emptyList())
            Log.d(TAG, "所有通知已清除")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "清除所有通知失败", e)
            Result.failure(e)
        }
    }

    // ==================== 偏好设置 / Preferences ====================

    /**
     * 更新通知偏好设置
     * Update notification preferences
     */
    override suspend fun updatePreferences(preferences: NotificationPreferences): Result<Unit> =
        mutex.withLock {
            try {
                cachedPreferences = preferences
                savePreferencesToFile(preferences)
                Log.d(TAG, "通知偏好设置已更新")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "更新偏好设置失败", e)
                Result.failure(e)
            }
        }

    /**
     * 获取当前通知偏好设置
     * Get current notification preferences
     */
    override suspend fun getPreferences(): Result<NotificationPreferences> {
        return Result.success(getPreferencesInternal())
    }

    // ==================== 实时观察 / Real-time Observation ====================

    /**
     * 观察通知列表变化
     * Observe notification list changes
     */
    override fun observeNotifications(): Flow<List<AppNotification>> =
        _notifications.asStateFlow()

    /**
     * 观察未读通知数量变化
     * Observe unread notification count changes
     */
    override fun observeUnreadCount(): Flow<Int> =
        _notifications.asStateFlow().map { notifications ->
            notifications.count {
                it.status == NotificationStatus.DELIVERED ||
                    it.status == NotificationStatus.PENDING
            }
        }

    /**
     * 观察通知摘要变化
     * Observe notification summary changes
     */
    override fun observeSummary(): Flow<NotificationSummary> =
        _notifications.asStateFlow().map { notifications ->
            NotificationSummary(
                totalCount = notifications.size,
                unreadCount = notifications.count {
                    it.status == NotificationStatus.DELIVERED ||
                        it.status == NotificationStatus.PENDING
                },
                urgentCount = notifications.count {
                    it.priority == com.appclonex.domain.model.NotificationPriority.URGENT &&
                        (it.status == NotificationStatus.DELIVERED ||
                            it.status == NotificationStatus.PENDING)
                },
                byCategory = notifications
                    .groupBy { it.category }
                    .mapValues { it.value.size },
                latestNotification = notifications.maxByOrNull { it.createdAt }
            )
        }

    // ==================== 私有辅助方法 / Private Helper Methods ====================

    /**
     * 更新指定通知的状态
     * Update the status of a specific notification
     *
     * @param notificationId 通知ID / Notification ID
     * @param newStatus 新状态 / New status
     */
    private suspend fun updateNotificationStatus(
        notificationId: AppNotificationId,
        newStatus: NotificationStatus
    ) {
        val updatedList = _notifications.value.map { notification ->
            if (notification.id == notificationId) {
                notification.copy(status = newStatus)
            } else {
                notification
            }
        }
        _notifications.value = updatedList
        saveNotificationsToFile(updatedList)
    }

    /**
     * 应用筛选条件到通知列表
     * Apply filter criteria to notification list
     *
     * @param notifications 原始通知列表 / Original notification list
     * @param filter 筛选条件 / Filter criteria
     * @return 符合条件的通知列表（按时间倒序） / Filtered list sorted by time descending
     */
    private fun applyFilter(
        notifications: List<AppNotification>,
        filter: NotificationFilter
    ): List<AppNotification> {
        return notifications.filter { notification ->
            // 按分类筛选 / Filter by category
            (filter.categories.isEmpty() || notification.category in filter.categories) &&
                // 按优先级筛选 / Filter by priority
                (filter.priorities.isEmpty() || notification.priority in filter.priorities) &&
                // 按状态筛选 / Filter by status
                (filter.statuses.isEmpty() || notification.status in filter.statuses) &&
                // 按时间范围筛选 / Filter by time range
                (filter.fromDate == null || notification.createdAt >= filter.fromDate) &&
                (filter.toDate == null || notification.createdAt <= filter.toDate) &&
                // 按分组筛选 / Filter by group
                (filter.groupId == null || notification.groupId == filter.groupId) &&
                // 按搜索关键词筛选（模糊匹配标题和内容） / Filter by search keyword
                (filter.searchQuery.isBlank() ||
                    notification.title.contains(filter.searchQuery, ignoreCase = true) ||
                    notification.content.contains(filter.searchQuery, ignoreCase = true))
        }.sortedByDescending { it.createdAt }
    }

    /**
     * 检查当前是否处于免打扰时段
     * Check if currently in quiet hours
     *
     * @return true 表示处于免打扰时段 / true if in quiet hours
     */
    private fun isInQuietHours(): Boolean {
        val prefs = cachedPreferences ?: return false
        val start = prefs.quietHoursStart ?: return false
        val end = prefs.quietHoursEnd ?: return false

        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        return if (start <= end) {
            currentTime in start..end
        } else {
            // 跨午夜的情况（如 22:00 - 08:00） / Cross-midnight case (e.g., 22:00 - 08:00)
            currentTime >= start || currentTime <= end
        }
    }

    /**
     * 重置每日通知计数（如果日期已变更）
     * Reset daily notification count if the date has changed
     */
    private fun resetDailyCountIfNeeded() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        if (today != lastCheckDate) {
            todaySentCount = 0
            lastCheckDate = today
        }
    }

    /**
     * 清理过期通知（如果需要）
     * Clean expired notifications if needed
     */
    private suspend fun cleanExpiredIfNeeded() {
        val now = System.currentTimeMillis()
        val hasExpired = _notifications.value.any { notification ->
            val expireAt = notification.expireAt
            expireAt != null && expireAt <= now
        }
        if (hasExpired) {
            deleteExpiredNotifications()
        }
    }

    /**
     * 获取内部偏好设置（不使用 suspend）
     * Get internal preferences (non-suspend)
     */
    private fun getPreferencesInternal(): NotificationPreferences {
        return cachedPreferences ?: NotificationPreferences()
    }

    // ==================== 文件持久化 / File Persistence ====================

    /**
     * 从文件加载通知数据
     * Load notification data from file
     */
    private fun loadNotificationsFromFile() {
        try {
            val file = File(context.filesDir, NOTIFICATIONS_FILE)
            if (file.exists()) {
                val content = file.readText()
                val notifications = json.decodeFromString<List<AppNotification>>(content)
                _notifications.value = notifications
                Log.d(TAG, "已从文件加载 ${notifications.size} 条通知")
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载通知文件失败", e)
            _notifications.value = emptyList()
        }
    }

    /**
     * 保存通知数据到文件
     * Save notification data to file
     */
    private fun saveNotificationsToFile(notifications: List<AppNotification>) {
        try {
            val file = File(context.filesDir, NOTIFICATIONS_FILE)
            val content = json.encodeToString(notifications)
            AtomicFileWriter.write(file, content)
        } catch (e: Exception) {
            Log.e(TAG, "保存通知文件失败", e)
        }
    }

    /**
     * 从文件加载偏好设置
     * Load preferences from file
     */
    private fun loadPreferences() {
        try {
            val file = File(context.filesDir, PREFS_FILE)
            if (file.exists()) {
                val content = file.readText()
                cachedPreferences = json.decodeFromString<NotificationPreferences>(content)
                Log.d(TAG, "已加载通知偏好设置")
            } else {
                cachedPreferences = NotificationPreferences()
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载偏好设置文件失败", e)
            cachedPreferences = NotificationPreferences()
        }
    }

    /**
     * 保存偏好设置到文件
     * Save preferences to file
     */
    private fun savePreferencesToFile(preferences: NotificationPreferences) {
        try {
            val file = File(context.filesDir, PREFS_FILE)
            val content = json.encodeToString(preferences)
            AtomicFileWriter.write(file, content)
        } catch (e: Exception) {
            Log.e(TAG, "保存偏好设置文件失败", e)
        }
    }
}
