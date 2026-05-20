/*
 * AppCloneX - 获取通知列表用例
 * AppCloneX - Get Notifications Use Case
 *
 * Copyright (c) 2024 AppCloneX Team. All rights reserved.
 *
 * 封装通知查询逻辑，支持实时观察通知列表变化。
 * Encapsulates notification query logic with real-time observation of list changes.
 */
package com.appclonex.domain.usecase.notification

import com.appclonex.domain.model.AppNotification
import com.appclonex.domain.model.NotificationFilter
import com.appclonex.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 获取通知列表用例
 * Get notifications use case
 *
 * 根据筛选条件获取通知列表，返回 Flow 以支持实时更新。
 * Retrieves notification list based on filter criteria, returns Flow for real-time updates.
 *
 * @property repository 通知仓库接口 / Notification repository interface
 */
class GetNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository
) {

    /**
     * 获取通知列表（实时观察）
     * Get notification list (real-time observation)
     *
     * 通过 Flow 持续观察通知变化，自动应用筛选条件。
     * Continuously observes notification changes via Flow, automatically applying filter criteria.
     *
     * @param filter 筛选条件 / Filter criteria
     * @param limit 返回数量上限 / Maximum number of results
     * @return 符合条件的通知列表 Flow / Flow of matching notification list
     */
    operator fun invoke(
        filter: NotificationFilter,
        limit: Int = 50
    ): Flow<List<AppNotification>> {
        return repository.observeNotifications().map { notifications ->
            applyFilter(notifications, filter).take(limit)
        }
    }

    /**
     * 获取通知摘要（实时观察）
     * Get notification summary (real-time observation)
     *
     * @return 通知摘要的 Flow / Flow of notification summary
     */
    fun observeSummary(): Flow<com.appclonex.domain.model.NotificationSummary> {
        return repository.observeSummary()
    }

    /**
     * 获取未读通知数量（实时观察）
     * Get unread notification count (real-time observation)
     *
     * @return 未读数量的 Flow / Flow of unread count
     */
    fun observeUnreadCount(): Flow<Int> {
        return repository.observeUnreadCount()
    }

    /**
     * 应用筛选条件到通知列表
     * Apply filter criteria to notification list
     *
     * 按分类、优先级、状态、时间范围、分组和搜索关键词进行过滤。
     * Filters by category, priority, status, time range, group, and search keyword.
     *
     * @param notifications 原始通知列表 / Original notification list
     * @param filter 筛选条件 / Filter criteria
     * @return 符合条件的通知列表 / Filtered notification list
     */
    private fun applyFilter(
        notifications: List<AppNotification>,
        filter: NotificationFilter
    ): List<AppNotification> {
        return notifications.filter { notification ->
            // 按分类筛选
            (filter.categories.isEmpty() || notification.category in filter.categories) &&
                // 按优先级筛选
                (filter.priorities.isEmpty() || notification.priority in filter.priorities) &&
                // 按状态筛选
                (filter.statuses.isEmpty() || notification.status in filter.statuses) &&
                // 按时间范围筛选
                (filter.fromDate == null || notification.createdAt >= filter.fromDate) &&
                (filter.toDate == null || notification.createdAt <= filter.toDate) &&
                // 按分组筛选
                (filter.groupId == null || notification.groupId == filter.groupId) &&
                // 按搜索关键词筛选（模糊匹配标题和内容）
                (filter.searchQuery.isBlank() ||
                    notification.title.contains(filter.searchQuery, ignoreCase = true) ||
                    notification.content.contains(filter.searchQuery, ignoreCase = true))
        }.sortedByDescending { it.createdAt }
    }
}
