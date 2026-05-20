/*
 * AppCloneX - 通知中心界面
 * AppCloneX - Notification Center Screen
 *
 * Copyright (c) 2024 AppCloneX Team. All rights reserved.
 *
 * 通知中心主界面，提供通知列表展示、分类筛选、优先级筛选、
 * 搜索、标记已读、删除和通知详情查看等功能。
 * Main notification center screen providing notification list display, category/priority
 * filtering, search, mark as read, delete, and notification detail viewing.
 */
package com.appclonex.presentation.screens.notifications

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appclonex.domain.model.AppNotification
import com.appclonex.domain.model.NotificationCategory
import com.appclonex.domain.model.NotificationPriority
import com.appclonex.domain.model.NotificationStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==================== 分类显示名称映射 / Category Display Name Mapping ====================

/**
 * 获取通知分类的中文显示名称
 * Get the Chinese display name for a notification category
 */
private fun NotificationCategory.displayName(): String = when (this) {
    NotificationCategory.CLONE -> "克隆"
    NotificationCategory.BACKUP -> "备份"
    NotificationCategory.FREEZE -> "冻结"
    NotificationCategory.PERFORMANCE -> "性能"
    NotificationCategory.SECURITY -> "安全"
    NotificationCategory.SYSTEM -> "系统"
    NotificationCategory.UPDATE -> "更新"
    NotificationCategory.PLUGIN -> "插件"
    NotificationCategory.CUSTOM -> "自定义"
}

/**
 * 获取通知分类对应的颜色
 * Get the color associated with a notification category
 */
private fun NotificationCategory.color(): Color = when (this) {
    NotificationCategory.CLONE -> Color(0xFF4CAF50)
    NotificationCategory.BACKUP -> Color(0xFF2196F3)
    NotificationCategory.FREEZE -> Color(0xFF9C27B0)
    NotificationCategory.PERFORMANCE -> Color(0xFFFF9800)
    NotificationCategory.SECURITY -> Color(0xFFF44336)
    NotificationCategory.SYSTEM -> Color(0xFF607D8B)
    NotificationCategory.UPDATE -> Color(0xFF00BCD4)
    NotificationCategory.PLUGIN -> Color(0xFFE91E63)
    NotificationCategory.CUSTOM -> Color(0xFF795548)
}

/**
 * 获取通知优先级对应的颜色
 * Get the color associated with a notification priority
 */
private fun NotificationPriority.color(): Color = when (this) {
    NotificationPriority.LOW -> Color(0xFF9E9E9E)
    NotificationPriority.NORMAL -> Color(0xFF2196F3)
    NotificationPriority.HIGH -> Color(0xFFFF9800)
    NotificationPriority.URGENT -> Color(0xFFF44336)
}

/**
 * 获取通知优先级的显示名称
 * Get the display name for a notification priority
 */
private fun NotificationPriority.displayName(): String = when (this) {
    NotificationPriority.LOW -> "低"
    NotificationPriority.NORMAL -> "普通"
    NotificationPriority.HIGH -> "重要"
    NotificationPriority.URGENT -> "紧急"
}

/**
 * 格式化时间戳为可读的时间字符串
 * Format timestamp to a readable time string
 */
private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000}分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000}小时前"
        diff < 604_800_000 -> "${diff / 86_400_000}天前"
        else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}

// ==================== 主界面 / Main Screen ====================

/**
 * 通知中心界面
 * Notification center screen
 *
 * 提供完整的通知管理功能，包括：
 * - 顶部统计栏（未读数、紧急数）
 * - 分类筛选 Chips
 * - 优先级筛选
 * - 通知列表（卡片式，支持滑动操作）
 * - 通知详情弹窗
 * - 空状态提示
 *
 * @param onNavigateBack 返回上一页的回调 / Callback to navigate back
 * @param viewModel 通知中心 ViewModel / Notification center ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotificationCenterScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationCenterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showClearAllDialog by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }

    // 显示成功消息 / Show success message
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }

    // 显示错误消息 / Show error message
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // 清除全部确认对话框 / Clear all confirmation dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("清除全部通知") },
            text = { Text("确定要清除所有通知吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllNotifications()
                    showClearAllDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 通知详情弹窗 / Notification detail dialog
    uiState.selectedNotification?.let { notification ->
        NotificationDetailDialog(
            notification = notification,
            onDismiss = { viewModel.dismissDetailDialog() },
            onMarkRead = { viewModel.markAsRead(notification.id) },
            onDelete = {
                viewModel.deleteNotification(notification.id)
                viewModel.dismissDetailDialog()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    BadgedBox(
                        badge = {
                            val unreadCount = uiState.summary.unreadCount
                            if (unreadCount > 0) {
                                Badge {
                                    Text(
                                        text = if (unreadCount > 99) "99+" else "$unreadCount",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    ) {
                        Text("通知中心")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 全部已读按钮 / Mark all as read button
                    if (uiState.summary.unreadCount > 0) {
                        IconButton(onClick = { viewModel.markAllAsRead() }) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "全部已读"
                            )
                        }
                    }
                    // 清除全部按钮 / Clear all button
                    if (uiState.notifications.isNotEmpty()) {
                        IconButton(onClick = { showClearAllDialog = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "清除全部"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ==================== 统计栏 / Statistics Bar ====================
            StatisticsBar(summary = uiState.summary)

            // ==================== 搜索栏 / Search Bar ====================
            if (searchExpanded) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text("搜索通知...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "清除搜索")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )
            }

            // ==================== 分类筛选 Chips / Category Filter Chips ====================
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 搜索按钮 / Search button
                FilterChip(
                    selected = searchExpanded,
                    onClick = { searchExpanded = !searchExpanded },
                    label = { Text("搜索") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                // 全部分类 / All categories
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text("全部") }
                )

                // 各分类筛选 / Category filters
                NotificationCategory.entries.forEach { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = {
                            viewModel.setCategoryFilter(
                                if (uiState.selectedCategory == category) null else category
                            )
                        },
                        label = { Text(category.displayName()) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(category.color())
                            )
                        }
                    )
                }
            }

            // ==================== 优先级筛选 / Priority Filter ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedPriority == null,
                    onClick = { viewModel.setPriorityFilter(null) },
                    label = { Text("全部优先级") }
                )
                NotificationPriority.entries.forEach { priority ->
                    FilterChip(
                        selected = uiState.selectedPriority == priority,
                        onClick = {
                            viewModel.setPriorityFilter(
                                if (uiState.selectedPriority == priority) null else priority
                            )
                        },
                        label = { Text(priority.displayName()) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(priority.color())
                            )
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ==================== 通知列表 / Notification List ====================
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.notifications.isEmpty()) {
                // 空状态 / Empty state
                EmptyNotificationState(
                    hasFilters = uiState.selectedCategory != null ||
                        uiState.selectedPriority != null ||
                        uiState.searchQuery.isNotBlank(),
                    onClearFilters = {
                        viewModel.setCategoryFilter(null)
                        viewModel.setPriorityFilter(null)
                        viewModel.setSearchQuery("")
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = uiState.notifications,
                        key = { it.id.value }
                    ) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = { viewModel.selectNotification(notification) },
                            onMarkRead = { viewModel.markAsRead(notification.id) },
                            onDelete = { viewModel.deleteNotification(notification.id) }
                        )
                    }
                }
            }
        }
    }
}

// ==================== 统计栏 / Statistics Bar ====================

/**
 * 通知统计栏组件
 * Notification statistics bar component
 *
 * 显示未读通知数和紧急通知数。
 * Displays unread count and urgent count.
 *
 * @param summary 通知统计摘要 / Notification summary
 */
@Composable
private fun StatisticsBar(summary: com.appclonex.domain.model.NotificationSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 未读通知统计 / Unread notification statistics
        StatCard(
            label = "未读通知",
            count = summary.unreadCount,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        // 紧急通知统计 / Urgent notification statistics
        StatCard(
            label = "紧急通知",
            count = summary.urgentCount,
            color = Color(0xFFF44336),
            modifier = Modifier.weight(1f)
        )

        // 通知总数 / Total notification count
        StatCard(
            label = "通知总数",
            count = summary.totalCount,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 统计卡片组件
 * Stat card component
 *
 * @param label 统计标签 / Stat label
 * @param count 统计数值 / Stat count
 * @param color 统计颜色 / Stat color
 * @param modifier 修饰符 / Modifier
 */
@Composable
private fun StatCard(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== 通知卡片 / Notification Card ====================

/**
 * 通知卡片组件
 * Notification card component
 *
 * 以卡片形式展示单条通知，包含分类图标、标题、内容、时间、
 * 优先级颜色标签和已读/未读状态指示。
 * Displays a single notification as a card with category icon, title, content,
 * time, priority color tag, and read/unread indicator.
 *
 * @param notification 通知数据 / Notification data
 * @param onClick 点击回调 / Click callback
 * @param onMarkRead 标记已读回调 / Mark as read callback
 * @param onDelete 删除回调 / Delete callback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToEnd -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToStart -> {
                    onMarkRead()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    val isUnread = notification.status == NotificationStatus.DELIVERED ||
        notification.status == NotificationStatus.PENDING

    val backgroundColor by animateColorAsState(
        targetValue = if (isUnread) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "notification_bg"
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToStart -> Color(0xFF4CAF50)
                    SwipeToDismissBoxValue.EndToEnd -> Color(0xFFF44336)
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToStart -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToEnd -> Alignment.CenterEnd
                    SwipeToDismissBoxValue.Settled -> Alignment.Center
                }
            ) {
                Icon(
                    imageVector = when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.StartToStart -> Icons.Default.CheckCircle
                        SwipeToDismissBoxValue.EndToEnd -> Icons.Default.Delete
                        SwipeToDismissBoxValue.Settled -> Icons.Default.Notifications
                    },
                    contentDescription = null,
                    tint = Color.White
                )
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 分类颜色指示条 / Category color indicator
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(notification.category.color())
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 通知内容 / Notification content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 标题行：分类标签 + 标题 + 优先级标签 / Title row: category tag + title + priority tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 未读圆点 / Unread indicator dot
                        if (isUnread) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }

                        // 分类标签 / Category tag
                        Text(
                            text = notification.category.displayName(),
                            style = MaterialTheme.typography.labelSmall,
                            color = notification.category.color(),
                            fontWeight = FontWeight.Bold
                        )

                        // 标题 / Title
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 内容 / Content
                    Text(
                        text = notification.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 底部行：时间 + 优先级标签 / Bottom row: time + priority tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatTime(notification.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        // 优先级标签 / Priority tag
                        if (notification.priority == NotificationPriority.HIGH ||
                            notification.priority == NotificationPriority.URGENT
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(notification.priority.color().copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = notification.priority.displayName(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = notification.priority.color(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== 空状态 / Empty State ====================

/**
 * 空通知状态组件
 * Empty notification state component
 *
 * 当没有通知时显示提示信息，如果有筛选条件则提示清除筛选。
 * Shows a prompt when there are no notifications, with an option to clear filters if active.
 *
 * @param hasFilters 是否有激活的筛选条件 / Whether any filters are active
 * @param onClearFilters 清除筛选回调 / Clear filters callback
 */
@Composable
private fun EmptyNotificationState(
    hasFilters: Boolean,
    onClearFilters: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )

            Text(
                text = if (hasFilters) "没有匹配的通知" else "暂无通知",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (hasFilters) "尝试调整筛选条件" else "所有通知都已处理完毕",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            if (hasFilters) {
                TextButton(onClick = onClearFilters) {
                    Text("清除筛选")
                }
            }
        }
    }
}

// ==================== 通知详情弹窗 / Notification Detail Dialog ====================

/**
 * 通知详情弹窗组件
 * Notification detail dialog component
 *
 * 显示通知的完整信息，包括标题、内容、分类、优先级、
 * 时间、状态和操作按钮。
 * Displays complete notification information including title, content, category,
 * priority, time, status, and action buttons.
 *
 * @param notification 通知数据 / Notification data
 * @param onDismiss 关闭弹窗回调 / Dismiss dialog callback
 * @param onMarkRead 标记已读回调 / Mark as read callback
 * @param onDelete 删除回调 / Delete callback
 */
@Composable
private fun NotificationDetailDialog(
    notification: AppNotification,
    onDismiss: () -> Unit,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(notification.category.color())
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(notification.title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 分类和优先级信息 / Category and priority info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "分类: ${notification.category.displayName()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = notification.category.color()
                    )
                    Text(
                        text = "优先级: ${notification.priority.displayName()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = notification.priority.color()
                    )
                }

                HorizontalDivider()

                // 通知内容 / Notification content
                Text(
                    text = notification.content,
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider()

                // 详细信息 / Detailed information
                Text(
                    text = "状态: ${
                        when (notification.status) {
                            NotificationStatus.PENDING -> "待发送"
                            NotificationStatus.DELIVERED -> "已送达"
                            NotificationStatus.READ -> "已读"
                            NotificationStatus.DISMISSED -> "已忽略"
                            NotificationStatus.EXPIRED -> "已过期"
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "时间: ${
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date(notification.createdAt))
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                // 操作链接 / Action link
                notification.actionLabel?.let { label ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "操作: $label",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // 元数据 / Metadata
                if (notification.metadata.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "附加信息:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    notification.metadata.forEach { (key, value) ->
                        Text(
                            text = "  $key: $value",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (notification.status != NotificationStatus.READ) {
                    TextButton(onClick = {
                        onMarkRead()
                        onDismiss()
                    }) {
                        Text("标记已读")
                    }
                }
                TextButton(onClick = {
                    onDelete()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
