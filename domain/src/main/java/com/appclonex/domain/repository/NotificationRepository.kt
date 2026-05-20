/*
 * AppCloneX - 通知仓库接口
 * AppCloneX - Notification Repository Interface
 *
 * Copyright (c) 2024 AppCloneX Team. All rights reserved.
 *
 * 定义通知系统的数据访问接口，遵循 Clean Architecture 的领域层规范。
 * Defines the data access interface for the notification system, following
 * Clean Architecture domain layer conventions.
 */
package com.appclonex.domain.repository

import com.appclonex.domain.model.AppNotification
import com.appclonex.domain.model.AppNotificationId
import com.appclonex.domain.model.NotificationCategory
import com.appclonex.domain.model.NotificationFilter
import com.appclonex.domain.model.NotificationGroup
import com.appclonex.domain.model.NotificationPreferences
import com.appclonex.domain.model.NotificationSummary
import kotlinx.coroutines.flow.Flow

/**
 * 通知仓库接口
 * Notification repository interface
 *
 * 提供通知的发送、查询、状态管理、分组聚合和偏好设置等完整功能。
 * Provides complete functionality for notification sending, querying,
 * status management, group aggregation, and preference management.
 */
interface NotificationRepository {

    // ==================== 发送通知 / Send Notifications ====================

    /**
     * 发送单条通知
     * Send a single notification
     *
     * 将通知保存到持久化存储，并通知观察者更新。
     * 会自动检查免打扰模式和每日通知上限。
     * Saves the notification to persistent storage and notifies observers.
     * Automatically checks quiet hours mode and daily notification limit.
     *
     * @param notification 要发送的通知 / The notification to send
     * @return 发送成功后返回完整的通知对象 / The complete notification object on success
     */
    suspend fun sendNotification(notification: AppNotification): Result<AppNotification>

    /**
     * 批量发送通知
     * Send notifications in batch
     *
     * @param notifications 要发送的通知列表 / List of notifications to send
     * @return 发送成功后返回完整的通知对象列表 / List of complete notification objects on success
     */
    suspend fun sendBatchNotifications(notifications: List<AppNotification>): Result<List<AppNotification>>

    // ==================== 状态管理 / Status Management ====================

    /**
     * 将指定通知标记为已读
     * Mark a specific notification as read
     *
     * @param notificationId 通知ID / Notification ID
     * @return 操作结果 / Operation result
     */
    suspend fun markAsRead(notificationId: AppNotificationId): Result<Unit>

    /**
     * 将所有通知标记为已读
     * Mark all notifications as read
     *
     * @return 操作结果 / Operation result
     */
    suspend fun markAllAsRead(): Result<Unit>

    /**
     * 忽略指定通知
     * Dismiss a specific notification
     *
     * @param notificationId 通知ID / Notification ID
     * @return 操作结果 / Operation result
     */
    suspend fun dismissNotification(notificationId: AppNotificationId): Result<Unit>

    /**
     * 忽略指定分类的所有通知
     * Dismiss all notifications of a specific category
     *
     * @param category 通知分类，null表示忽略所有 / Notification category, null means dismiss all
     * @return 操作结果 / Operation result
     */
    suspend fun dismissAll(category: NotificationCategory?): Result<Unit>

    // ==================== 删除操作 / Delete Operations ====================

    /**
     * 删除指定通知
     * Delete a specific notification
     *
     * @param notificationId 通知ID / Notification ID
     * @return 操作结果 / Operation result
     */
    suspend fun deleteNotification(notificationId: AppNotificationId): Result<Unit>

    /**
     * 删除所有已过期的通知
     * Delete all expired notifications
     *
     * 自动检查通知的 expireAt 字段，移除已过期的通知。
     * Automatically checks the expireAt field and removes expired notifications.
     *
     * @return 被删除的通知数量 / Number of deleted notifications
     */
    suspend fun deleteExpiredNotifications(): Result<Int>

    // ==================== 查询操作 / Query Operations ====================

    /**
     * 获取指定ID的通知
     * Get a notification by its ID
     *
     * @param notificationId 通知ID / Notification ID
     * @return 通知对象，不存在则返回null / Notification object, or null if not found
     */
    suspend fun getNotification(notificationId: AppNotificationId): Result<AppNotification?>

    /**
     * 根据筛选条件获取通知列表
     * Get notifications based on filter criteria
     *
     * 支持分页查询，自动过滤已过期通知。
     * Supports paginated queries, automatically filters expired notifications.
     *
     * @param filter 筛选条件 / Filter criteria
     * @param limit 返回数量上限 / Maximum number of results
     * @param offset 偏移量（分页用） / Offset for pagination
     * @return 符合条件的通知列表 / List of matching notifications
     */
    suspend fun getNotifications(
        filter: NotificationFilter,
        limit: Int,
        offset: Int
    ): Result<List<AppNotification>>

    /**
     * 获取未读通知数量
     * Get the count of unread notifications
     *
     * @return 未读通知数量 / Unread notification count
     */
    suspend fun getUnreadCount(): Result<Int>

    /**
     * 获取通知统计摘要
     * Get notification summary statistics
     *
     * @return 通知摘要信息 / Notification summary
     */
    suspend fun getSummary(): Result<NotificationSummary>

    /**
     * 获取通知分组列表
     * Get notification groups
     *
     * 按 groupId 聚合通知，返回分组信息列表。
     * Aggregates notifications by groupId and returns a list of group information.
     *
     * @return 通知分组列表 / List of notification groups
     */
    suspend fun getGroups(): Result<List<NotificationGroup>>

    // ==================== 清理操作 / Cleanup Operations ====================

    /**
     * 清除所有通知
     * Clear all notifications
     *
     * @return 操作结果 / Operation result
     */
    suspend fun clearAllNotifications(): Result<Unit>

    // ==================== 偏好设置 / Preferences ====================

    /**
     * 更新通知偏好设置
     * Update notification preferences
     *
     * @param preferences 新的通知偏好设置 / New notification preferences
     * @return 操作结果 / Operation result
     */
    suspend fun updatePreferences(preferences: NotificationPreferences): Result<Unit>

    /**
     * 获取当前通知偏好设置
     * Get current notification preferences
     *
     * @return 当前的通知偏好设置 / Current notification preferences
     */
    suspend fun getPreferences(): Result<NotificationPreferences>

    // ==================== 实时观察 / Real-time Observation ====================

    /**
     * 观察通知列表变化
     * Observe notification list changes
     *
     * 返回一个 Flow，每当通知列表发生变化时自动发射最新列表。
     * Returns a Flow that automatically emits the latest list when notifications change.
     *
     * @return 通知列表的 Flow / Flow of notification list
     */
    fun observeNotifications(): Flow<List<AppNotification>>

    /**
     * 观察未读通知数量变化
     * Observe unread notification count changes
     *
     * @return 未读数量的 Flow / Flow of unread count
     */
    fun observeUnreadCount(): Flow<Int>

    /**
     * 观察通知摘要变化
     * Observe notification summary changes
     *
     * @return 通知摘要的 Flow / Flow of notification summary
     */
    fun observeSummary(): Flow<NotificationSummary>
}
