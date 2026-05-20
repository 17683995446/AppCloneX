/*
 * AppCloneX - 通知管理用例
 * AppCloneX - Manage Notifications Use Case
 *
 * Copyright (c) 2024 AppCloneX Team. All rights reserved.
 *
 * 封装通知的状态管理操作，包括标记已读、忽略和清除。
 * Encapsulates notification status management operations including marking as read,
 * dismissing, and clearing.
 */
package com.appclonex.domain.usecase.notification

import com.appclonex.domain.model.AppNotificationId
import com.appclonex.domain.model.NotificationCategory
import com.appclonex.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * 通知管理用例
 * Manage notifications use case
 *
 * 提供通知状态管理的统一入口，包括标记已读、全部已读、
 * 忽略单条、忽略分类和清除全部等操作。
 * Provides a unified entry point for notification status management, including
 * marking as read, marking all as read, dismissing single/category, and clearing all.
 *
 * @property repository 通知仓库接口 / Notification repository interface
 */
class ManageNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository
) {

    /**
     * 将指定通知标记为已读
     * Mark a specific notification as read
     *
     * @param id 通知ID / Notification ID
     * @return 操作结果 / Operation result
     */
    suspend fun markRead(id: AppNotificationId): Result<Unit> =
        repository.markAsRead(id)

    /**
     * 将所有通知标记为已读
     * Mark all notifications as read
     *
     * @return 操作结果 / Operation result
     */
    suspend fun markAllRead(): Result<Unit> =
        repository.markAllAsRead()

    /**
     * 忽略指定通知
     * Dismiss a specific notification
     *
     * @param id 通知ID / Notification ID
     * @return 操作结果 / Operation result
     */
    suspend fun dismiss(id: AppNotificationId): Result<Unit> =
        repository.dismissNotification(id)

    /**
     * 忽略指定分类的所有通知，或忽略全部
     * Dismiss all notifications of a specific category, or dismiss all
     *
     * @param category 通知分类，null表示忽略所有 / Notification category, null means dismiss all
     * @return 操作结果 / Operation result
     */
    suspend fun dismissAll(category: NotificationCategory? = null): Result<Unit> =
        repository.dismissAll(category)

    /**
     * 清除所有通知
     * Clear all notifications
     *
     * 与 dismissAll 不同，此操作会永久删除所有通知数据。
     * Unlike dismissAll, this operation permanently deletes all notification data.
     *
     * @return 操作结果 / Operation result
     */
    suspend fun clearAll(): Result<Unit> =
        repository.clearAllNotifications()

    /**
     * 删除指定通知
     * Delete a specific notification
     *
     * @param id 通知ID / Notification ID
     * @return 操作结果 / Operation result
     */
    suspend fun delete(id: AppNotificationId): Result<Unit> =
        repository.deleteNotification(id)

    /**
     * 删除所有已过期的通知
     * Delete all expired notifications
     *
     * @return 被删除的通知数量 / Number of deleted notifications
     */
    suspend fun deleteExpired(): Result<Int> =
        repository.deleteExpiredNotifications()
}
