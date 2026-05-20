/*
 * AppCloneX - 发送通知用例
 * AppCloneX - Send Notification Use Case
 *
 * Copyright (c) 2024 AppCloneX Team. All rights reserved.
 *
 * 封装发送通知的业务逻辑，提供简化的调用接口。
 * Encapsulates the business logic for sending notifications with a simplified API.
 */
package com.appclonex.domain.usecase.notification

import com.appclonex.domain.model.AppNotification
import com.appclonex.domain.model.NotificationCategory
import com.appclonex.domain.model.NotificationPriority
import com.appclonex.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * 发送通知用例
 * Send notification use case
 *
 * 提供简化的通知发送接口，只需传入标题、内容、分类和优先级即可发送通知。
 * Provides a simplified notification sending interface - just pass title, content,
 * category, and priority to send a notification.
 *
 * @property repository 通知仓库接口 / Notification repository interface
 */
class SendNotificationUseCase @Inject constructor(
    private val repository: NotificationRepository
) {

    /**
     * 发送一条新通知
     * Send a new notification
     *
     * 自动生成通知ID、设置创建时间戳和默认状态。
     * Automatically generates notification ID, sets creation timestamp and default status.
     *
     * @param title 通知标题 / Notification title
     * @param content 通知正文内容 / Notification body content
     * @param category 通知分类 / Notification category
     * @param priority 通知优先级 / Notification priority
     * @return 发送成功后返回完整的通知对象 / Complete notification object on success
     */
    suspend operator fun invoke(
        title: String,
        content: String,
        category: NotificationCategory,
        priority: NotificationPriority
    ): Result<AppNotification> {
        val notification = AppNotification(
            title = title,
            content = content,
            category = category,
            priority = priority
        )
        return repository.sendNotification(notification)
    }

    /**
     * 发送一条带分组的新通知
     * Send a new notification with group
     *
     * @param title 通知标题 / Notification title
     * @param content 通知正文内容 / Notification body content
     * @param category 通知分类 / Notification category
     * @param priority 通知优先级 / Notification priority
     * @param groupId 分组ID / Group ID
     * @param actionUrl 操作链接URL / Action URL
     * @param actionLabel 操作按钮文本 / Action button label
     * @return 发送成功后返回完整的通知对象 / Complete notification object on success
     */
    suspend operator fun invoke(
        title: String,
        content: String,
        category: NotificationCategory,
        priority: NotificationPriority,
        groupId: String?,
        actionUrl: String? = null,
        actionLabel: String? = null
    ): Result<AppNotification> {
        val notification = AppNotification(
            title = title,
            content = content,
            category = category,
            priority = priority,
            groupId = groupId,
            actionUrl = actionUrl,
            actionLabel = actionLabel
        )
        return repository.sendNotification(notification)
    }
}
