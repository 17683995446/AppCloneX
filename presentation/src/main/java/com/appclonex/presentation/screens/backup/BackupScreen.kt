package com.appclonex.presentation.screens.backup

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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appclonex.domain.model.BackupMetadata

/**
 * 备份与恢复管理界面
 * Backup and restore management screen
 *
 * 提供创建备份、查看备份列表、恢复备份和删除备份的功能。
 * Provides functionality to create backups, view backup list, restore and delete backups.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showRestoreDialog by remember { mutableStateOf<BackupMetadata?>(null) }
    var showDeleteDialog by remember { mutableStateOf<BackupMetadata?>(null) }

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

    // 恢复确认对话框
    if (showRestoreDialog != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = null },
            title = { Text(text = "恢复备份") },
            text = {
                Text(
                    text = "确定要从备份 \"${showRestoreDialog?.fileName}\" 恢复吗？\n\n" +
                        "这将覆盖当前的配置文件和设置，此操作不可撤销。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDialog?.let { backup ->
                            viewModel.restoreBackup(backup.fileName)
                        }
                        showRestoreDialog = null
                    }
                ) {
                    Text(text = "恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = null }) {
                    Text(text = "取消")
                }
            }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(text = "删除备份") },
            text = {
                Text(
                    text = "确定要删除备份 \"${showDeleteDialog?.fileName}\" 吗？\n\n此操作不可撤销。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog?.let { backup ->
                            viewModel.deleteBackup(backup.id)
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
                title = { Text(text = "备份与恢复") },
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 创建备份卡片
            CreateBackupCard(
                isLoading = uiState.isCreating,
                onCreateBackup = { viewModel.createBackup() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 备份列表标题
            Text(
                text = "备份列表",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 备份列表内容
            if (uiState.isLoading && uiState.backups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.backups.isEmpty()) {
                EmptyBackupState(
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.backups,
                        key = { it.id }
                    ) { backup ->
                        BackupItemCard(
                            backup = backup,
                            onRestore = { showRestoreDialog = backup },
                            onDelete = { showDeleteDialog = backup }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 创建备份卡片组件
 * Card component for creating a new backup
 */
@Composable
private fun CreateBackupCard(
    isLoading: Boolean,
    onCreateBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "创建备份",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "将当前所有配置文件、克隆应用、设置和自动化规则创建为一份完整备份。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCreateBackup,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "正在创建备份...")
                } else {
                    Text(text = "立即创建备份")
                }
            }
        }
    }
}

/**
 * 备份列表项卡片组件
 * Backup list item card component
 */
@Composable
private fun BackupItemCard(
    backup: BackupMetadata,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 文件名和日期
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = backup.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (backup.isAutoBackup) {
                    Text(
                        text = "自动",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 日期
            Text(
                text = formatBackupDate(backup.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 备份信息行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BackupInfoChip(
                    label = "大小",
                    value = formatFileSize(backup.fileSize)
                )
                BackupInfoChip(
                    label = "配置",
                    value = "${backup.profileCount} 个"
                )
                BackupInfoChip(
                    label = "应用",
                    value = "${backup.appCount} 个"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "恢复")
                }

                OutlinedButton(
                    onClick = onDelete,
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "删除")
                }
            }
        }
    }
}

/**
 * 备份信息标签组件
 * Backup info chip component
 */
@Composable
private fun BackupInfoChip(
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
 * 空备份状态组件
 * Empty backup state component
 */
@Composable
private fun EmptyBackupState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AddCircleOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无备份",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "点击上方按钮创建您的第一份备份",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 格式化备份日期时间戳
 * Format backup date timestamp to readable string
 */
private fun formatBackupDate(timestamp: Long): String {
    if (timestamp == 0L) return "未知时间"
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

/**
 * 格式化文件大小
 * Format file size in bytes to human-readable string
 */
private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val index = digitGroups.coerceAtMost(units.size - 1)
    return String.format(
        java.util.Locale.getDefault(),
        "%.1f %s",
        bytes / Math.pow(1024.0, index.toDouble()),
        units[index]
    )
}
