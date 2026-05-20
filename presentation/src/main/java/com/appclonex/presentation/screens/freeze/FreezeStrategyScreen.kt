package com.appclonex.presentation.screens.freeze

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appclonex.domain.model.FreezeStrategy
import com.appclonex.domain.model.FreezeTriggerType

/**
 * 智能冻结策略管理界面
 * Smart freeze strategy management screen
 *
 * 提供冻结策略的列表展示、启用/禁用切换、创建、编辑、删除和手动执行功能。
 * Provides freeze strategy list display, enable/disable toggle, create, edit, delete and manual execution.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreezeStrategyScreen(
    onNavigateBack: () -> Unit,
    viewModel: FreezeStrategyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<FreezeStrategy?>(null) }
    var showDeleteDialog by remember { mutableStateOf<FreezeStrategy?>(null) }

    // 显示成功消息
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }

    // 显示错误消息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // 创建/编辑策略对话框
    val editingStrategy = showEditDialog
    if (showCreateDialog || editingStrategy != null) {
        StrategyEditDialog(
            strategy = editingStrategy,
            onDismiss = {
                showCreateDialog = false
                showEditDialog = null
            },
            onSave = { name, triggerType ->
                if (editingStrategy != null) {
                    viewModel.updateStrategy(
                        editingStrategy.copy(name = name, triggerType = triggerType)
                    )
                } else {
                    viewModel.createStrategy(name, triggerType)
                }
                showCreateDialog = false
                showEditDialog = null
            }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(text = "删除策略") },
            text = {
                Text(
                    text = "确定要删除策略 \"${showDeleteDialog?.name}\" 吗？\n\n此操作不可撤销。"
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showDeleteDialog?.let { strategy ->
                            viewModel.deleteStrategy(strategy.id)
                        }
                        showDeleteDialog = null
                    }
                ) {
                    Text(text = "删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(text = "取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "智能冻结策略") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建策略"
                )
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.strategies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.strategies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无冻结策略",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "点击右下角按钮创建您的第一个冻结策略",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                items(
                    items = uiState.strategies,
                    key = { it.id }
                ) { strategy ->
                    FreezeStrategyItemCard(
                        strategy = strategy,
                        isExecuting = uiState.executingStrategyId == strategy.id,
                        onToggleEnabled = { enabled ->
                            viewModel.toggleStrategy(strategy.id, enabled)
                        },
                        onExecute = { viewModel.executeStrategy(strategy.id) },
                        onEdit = { showEditDialog = strategy },
                        onDelete = { showDeleteDialog = strategy }
                    )
                }
            }
        }
    }
}

/**
 * 冻结策略列表项卡片
 * Freeze strategy list item card
 */
@Composable
private fun FreezeStrategyItemCard(
    strategy: FreezeStrategy,
    isExecuting: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onExecute: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (strategy.isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 策略名称和开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strategy.name.ifEmpty { "未命名策略" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = strategy.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 策略详情信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StrategyInfoChip(
                    label = "触发类型",
                    value = getTriggerTypeDisplayName(strategy.triggerType)
                )
                StrategyInfoChip(
                    label = "目标应用",
                    value = if (strategy.targetApps.isEmpty()) "全部" else "${strategy.targetApps.size} 个"
                )
            }

            // 调度信息
            if (strategy.schedule != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "调度: ${formatSchedule(strategy.schedule.startTimeHour, strategy.schedule.startTimeMinute)} - " +
                        "${formatSchedule(strategy.schedule.endTimeHour, strategy.schedule.endTimeMinute)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "执行中...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    IconButton(onClick = onExecute) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "执行策略",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑策略",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除策略",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 策略信息标签
 * Strategy info chip
 */
@Composable
private fun StrategyInfoChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 策略创建/编辑对话框
 * Strategy create/edit dialog
 */
@Composable
private fun StrategyEditDialog(
    strategy: FreezeStrategy?,
    onDismiss: () -> Unit,
    onSave: (name: String, triggerType: FreezeTriggerType) -> Unit
) {
    var strategyName by remember(strategy) {
        mutableStateOf(strategy?.name ?: "")
    }
    var selectedTriggerType by remember(strategy) {
        mutableStateOf(strategy?.triggerType ?: FreezeTriggerType.MANUAL)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (strategy != null) "编辑策略" else "新建策略")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = strategyName,
                    onValueChange = { strategyName = it },
                    label = { Text(text = "策略名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "触发类型",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                FreezeTriggerType.entries.forEach { triggerType ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedTriggerType == triggerType,
                            onClick = { selectedTriggerType = triggerType }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getTriggerTypeDisplayName(triggerType),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onSave(strategyName, selectedTriggerType) },
                enabled = strategyName.isNotBlank()
            ) {
                Text(text = "保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}

/**
 * 获取触发类型的中文显示名称
 * Get the Chinese display name for a trigger type
 */
private fun getTriggerTypeDisplayName(triggerType: FreezeTriggerType): String {
    return when (triggerType) {
        FreezeTriggerType.MANUAL -> "手动触发"
        FreezeTriggerType.SCHEDULE -> "定时触发"
        FreezeTriggerType.SCREEN_OFF -> "熄屏触发"
        FreezeTriggerType.INACTIVITY -> "不活跃触发"
        FreezeTriggerType.BATTERY_LOW -> "低电量触发"
        FreezeTriggerType.WIFI_DISCONNECT -> "WiFi断开触发"
        FreezeTriggerType.APP_SWITCH -> "切换应用触发"
        FreezeTriggerType.COMPOUND -> "组合条件"
    }
}

/**
 * 格式化调度时间
 * Format schedule time to readable string
 */
private fun formatSchedule(hour: Int, minute: Int): String {
    return String.format("%02d:%02d", hour, minute)
}
