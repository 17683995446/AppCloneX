/*
 * AppCloneX - 通知中心 ViewModel
 * AppCloneX - Notification Center ViewModel
 *
 * Copyright (c) 2024 AppCloneX Team. All rights reserved.
 *
 * 管理通知中心界面的状态，包括通知列表、未读数、筛选状态和用户操作。
 * 继承 BaseViewModel 获得统一异常处理（R9-VM-008 修复）。
 */
package com.appclonex.presentation.screens.notifications

import com.appclonex.domain.model.AppNotification
import com.appclonex.domain.model.AppNotificationId
import com.appclonex.domain.model.NotificationCategory
import com.appclonex.domain.model.NotificationFilter
import com.appclonex.domain.model.NotificationPriority
import com.appclonex.domain.model.NotificationSummary
import com.appclonex.domain.model.NotificationStatus
import com.appclonex.domain.usecase.notification.GetNotificationsUseCase
import com.appclonex.domain.usecase.notification.ManageNotificationsUseCase
import com.appclonex.domain.usecase.notification.SendNotificationUseCase
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 通知中心界面 UI 状态
 */
data class NotificationCenterUiState(
    override val isLoading: Boolean = false,
    override val message: String? = null,
    override val error: String? = null,
    val notifications: List<AppNotification> = emptyList(),
    val summary: NotificationSummary = NotificationSummary(),
    val selectedCategory: NotificationCategory? = null,
    val selectedPriority: NotificationPriority? = null,
    val searchQuery: String = "",
    val successMessage: String? = null,
    val selectedNotification: AppNotification? = null
) : BaseUiState

/**
 * 通知中心 ViewModel
 *
 * 负责管理通知列表的加载、筛选、发送、标记已读、删除和清除操作。
 */
@HiltViewModel
class NotificationCenterViewModel @Inject constructor(
    private val sendNotificationUseCase: SendNotificationUseCase,
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val manageNotificationsUseCase: ManageNotificationsUseCase
) : BaseViewModel<NotificationCenterUiState>(NotificationCenterUiState()) {

    // 跟踪通知列表收集的 Job，切换筛选时取消前一次（R9-VM-009 修复）
    private var notificationListJob: Job? = null

    init {
        refreshNotifications()
        observeSummary()
    }

    // ==================== 观察数据 ====================

    /**
     * 观察通知摘要变化
     */
    private fun observeSummary() {
        launchWithErrorHandler {
            getNotificationsUseCase.observeSummary().collect { summary ->
                updateState { it.copy(summary = summary) }
            }
        }
    }

    // ==================== 筛选操作 ====================

    fun setCategoryFilter(category: NotificationCategory?) {
        updateState { it.copy(selectedCategory = category) }
        refreshNotifications()
    }

    fun setPriorityFilter(priority: NotificationPriority?) {
        updateState { it.copy(selectedPriority = priority) }
        refreshNotifications()
    }

    fun setSearchQuery(query: String) {
        updateState { it.copy(searchQuery = query) }
        refreshNotifications()
    }

    private fun buildCurrentFilter(): NotificationFilter {
        val state = currentState
        return NotificationFilter(
            categories = state.selectedCategory?.let { setOf(it) } ?: emptySet(),
            priorities = state.selectedPriority?.let { setOf(it) } ?: emptySet(),
            searchQuery = state.searchQuery
        )
    }

    /**
     * 刷新通知列表（取消前一次收集，防止竞态）
     */
    private fun refreshNotifications() {
        // 取消前一次 Flow 收集（R9-VM-009 修复）
        notificationListJob?.cancel()
        notificationListJob = launchWithErrorHandler {
            updateState { it.copy(isLoading = true) }
            getNotificationsUseCase(
                filter = buildCurrentFilter(),
                limit = 100
            ).collect { notifications ->
                updateState { it.copy(notifications = notifications, isLoading = false) }
            }
        }
    }

    // ==================== 通知操作 ====================

    fun sendTestNotification() {
        launchWithErrorHandler {
            sendNotificationUseCase(
                title = "测试通知",
                content = "这是一条测试通知消息，用于验证通知中心功能。",
                category = NotificationCategory.SYSTEM,
                priority = NotificationPriority.NORMAL
            ).onSuccess {
                updateState { it.copy(successMessage = "测试通知已发送") }
            }.onFailure { e ->
                updateState { it.copy(error = e.message ?: "发送失败") }
            }
        }
    }

    fun markAsRead(notificationId: AppNotificationId) {
        launchWithErrorHandler {
            manageNotificationsUseCase.markRead(notificationId)
                .onSuccess {
                    updateState { it.copy(successMessage = "已标记为已读") }
                }
                .onFailure { e ->
                    updateState { it.copy(error = e.message ?: "操作失败") }
                }
        }
    }

    fun markAllAsRead() {
        launchWithErrorHandler {
            updateState { it.copy(isLoading = true) }
            manageNotificationsUseCase.markAllRead()
                .onSuccess {
                    updateState { it.copy(successMessage = "已全部标记为已读", isLoading = false) }
                }
                .onFailure { e ->
                    updateState { it.copy(error = e.message ?: "操作失败", isLoading = false) }
                }
        }
    }

    fun deleteNotification(notificationId: AppNotificationId) {
        launchWithErrorHandler {
            manageNotificationsUseCase.delete(notificationId)
                .onSuccess {
                    updateState { it.copy(successMessage = "通知已删除") }
                }
                .onFailure { e ->
                    updateState { it.copy(error = e.message ?: "删除失败") }
                }
        }
    }

    fun dismissNotification(notificationId: AppNotificationId) {
        launchWithErrorHandler {
            manageNotificationsUseCase.dismiss(notificationId)
                .onSuccess {
                    updateState { it.copy(successMessage = "通知已忽略") }
                }
                .onFailure { e ->
                    updateState { it.copy(error = e.message ?: "操作失败") }
                }
        }
    }

    fun clearAllNotifications() {
        launchWithErrorHandler {
            updateState { it.copy(isLoading = true) }
            manageNotificationsUseCase.clearAll()
                .onSuccess {
                    updateState { it.copy(successMessage = "所有通知已清除", isLoading = false) }
                }
                .onFailure { e ->
                    updateState { it.copy(error = e.message ?: "清除失败", isLoading = false) }
                }
        }
    }

    // ==================== 通知详情 ====================

    fun selectNotification(notification: AppNotification) {
        updateState { it.copy(selectedNotification = notification) }
        if (notification.status != NotificationStatus.READ) {
            markAsRead(notification.id)
        }
    }

    fun dismissDetailDialog() {
        updateState { it.copy(selectedNotification = null) }
    }

    // ==================== 消息管理 ====================

    fun clearSuccessMessage() {
        updateState { it.copy(successMessage = null) }
    }
}
