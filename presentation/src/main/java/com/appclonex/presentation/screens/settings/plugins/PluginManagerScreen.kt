/*
 * Copyright (c) 2024 AppCloneX Contributors
 *
 * 插件管理界面
 *
 * 提供插件系统的可视化管理界面，包括：
 * - 已安装插件列表（卡片式展示）
 * - 插件详情弹窗（完整信息、权限、配置）
 * - 安装插件对话框
 * - 搜索栏（模糊搜索）
 * - 空状态提示
 * - 权限管理对话框
 *
 * 使用 Material 3 设计规范，支持深色模式。
 */
package com.appclonex.presentation.screens.settings.plugins

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appclonex.domain.model.PluginId
import com.appclonex.domain.model.PluginManifest
import com.appclonex.domain.model.PluginMetadata
import com.appclonex.domain.model.PluginPermission
import com.appclonex.domain.model.PluginState

// ============================================================================
// 主界面
// ============================================================================

/**
 * 插件管理界面入口
 *
 * 展示已安装插件列表，提供搜索、安装、管理等功能。
 *
 * @param onNavigateBack 返回上一页的回调
 * @param viewModel 插件管理ViewModel，通过Hilt注入
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginManagerScreen(
    onNavigateBack: () -> Unit,
    viewModel: PluginManagerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 显示提示消息
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    // 显示错误消息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("插件管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showInstallDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "安装插件")
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
            // 搜索栏
            PluginSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) }
            )

            // 插件列表或空状态
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredPlugins.isEmpty()) {
                PluginEmptyState(
                    isSearching = uiState.searchQuery.isNotBlank()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                ) {
                    items(
                        items = uiState.filteredPlugins,
                        key = { it.id.value }
                    ) { plugin ->
                        val state = uiState.pluginStates[plugin.id.value]
                        PluginCard(
                            plugin = plugin,
                            state = state,
                            onClick = { viewModel.selectPlugin(plugin) }
                        )
                    }
                }
            }
        }
    }

    // 安装插件对话框
    if (uiState.showInstallDialog) {
        InstallPluginDialog(
            onDismiss = { viewModel.hideInstallDialog() },
            onInstall = { manifest -> viewModel.installPlugin(manifest) }
        )
    }

    // 插件详情弹窗
    val selectedPlugin = uiState.selectedPlugin
    if (uiState.showDetailDialog && selectedPlugin != null) {
        PluginDetailDialog(
            plugin = selectedPlugin,
            state = uiState.pluginStates[selectedPlugin.id.value],
            permissions = uiState.pluginPermissions[selectedPlugin.id.value] ?: emptyList(),
            onDismiss = { viewModel.hideDetailDialog() },
            onEnable = { viewModel.enablePlugin(selectedPlugin.id) },
            onDisable = { viewModel.disablePlugin(selectedPlugin.id.value) },
            onUninstall = { viewModel.uninstallPlugin(selectedPlugin.id) },
            onUpdate = { viewModel.updatePlugin(selectedPlugin.id) },
            onManagePermissions = { viewModel.showPermissionDialog() }
        )
    }

    // 权限管理对话框
    if (uiState.showPermissionDialog && selectedPlugin != null) {
        PermissionManageDialog(
            plugin = selectedPlugin,
            grantedPermissions = uiState.pluginPermissions[selectedPlugin.id.value] ?: emptyList(),
            onGrant = { permission ->
                viewModel.grantPermission(selectedPlugin.id, permission)
            },
            onRevoke = { permission ->
                viewModel.revokePermission(selectedPlugin.id, permission)
            },
            onDismiss = { viewModel.hidePermissionDialog() }
        )
    }
}

// ============================================================================
// 搜索栏
// ============================================================================

/**
 * 插件搜索栏组件
 *
 * @param query 当前搜索关键词
 * @param onQueryChange 搜索关键词变更回调
 */
@Composable
private fun PluginSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("搜索插件...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "清除搜索")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

// ============================================================================
// 空状态
// ============================================================================

/**
 * 插件列表空状态组件
 *
 * @param isSearching 是否处于搜索状态（搜索无结果 vs 无已安装插件）
 */
@Composable
private fun PluginEmptyState(isSearching: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Extension,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearching) "未找到匹配的插件" else "暂无已安装插件",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isSearching) "尝试更换搜索关键词" else "点击右上角 + 按钮安装插件",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// ============================================================================
// 插件卡片
// ============================================================================

/**
 * 插件卡片组件
 *
 * 以卡片形式展示插件的基本信息，包括名称、版本、状态和作者。
 *
 * @param plugin 插件元数据
 * @param state 插件当前状态
 * @param onClick 点击回调
 */
@Composable
private fun PluginCard(
    plugin: PluginMetadata,
    state: PluginState?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 插件图标
            Icon(
                imageVector = Icons.Default.Extension,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 插件信息
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // 状态标签
                    PluginStateBadge(state = state)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "v${plugin.version}  ·  ${plugin.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = plugin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ============================================================================
// 状态标签
// ============================================================================

/**
 * 插件状态标签组件
 *
 * @param state 插件状态
 */
@Composable
private fun PluginStateBadge(state: PluginState?) {
    val (text, containerColor) = when (state) {
        PluginState.ENABLED -> "已启用" to MaterialTheme.colorScheme.primaryContainer
        PluginState.DISABLED -> "已禁用" to MaterialTheme.colorScheme.surfaceVariant
        PluginState.INSTALLED -> "已安装" to MaterialTheme.colorScheme.tertiaryContainer
        PluginState.ERROR -> "错误" to MaterialTheme.colorScheme.errorContainer
        PluginState.UPDATING -> "更新中" to MaterialTheme.colorScheme.secondaryContainer
        null -> "未知" to MaterialTheme.colorScheme.surfaceVariant
    }

    Badge(
        containerColor = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// ============================================================================
// 安装插件对话框
// ============================================================================

/**
 * 安装插件对话框
 *
 * 提供输入插件 manifest URL 或选择本地文件的安装方式。
 *
 * @param onDismiss 关闭对话框回调
 * @param onInstall 执行安装回调
 */
@Composable
private fun InstallPluginDialog(
    onDismiss: () -> Unit,
    onInstall: (PluginManifest) -> Unit
) {
    var pluginName by remember { mutableStateOf("") }
    var pluginVersion by remember { mutableStateOf("1.0.0") }
    var pluginAuthor by remember { mutableStateOf("") }
    var pluginDescription by remember { mutableStateOf("") }
    var manifestUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("安装插件") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "输入插件 Manifest URL 或手动填写信息",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = manifestUrl,
                    onValueChange = { manifestUrl = it },
                    label = { Text("Manifest URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "— 或手动填写 —",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                OutlinedTextField(
                    value = pluginName,
                    onValueChange = { pluginName = it },
                    label = { Text("插件名称 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pluginVersion,
                    onValueChange = { pluginVersion = it },
                    label = { Text("版本号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pluginAuthor,
                    onValueChange = { pluginAuthor = it },
                    label = { Text("作者 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pluginDescription,
                    onValueChange = { pluginDescription = it },
                    label = { Text("描述 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 构建插件清单
                    val metadata = com.appclonex.domain.model.PluginMetadata(
                        id = PluginId.generate(),
                        name = pluginName,
                        version = pluginVersion,
                        description = pluginDescription,
                        author = pluginAuthor,
                        minAppVersion = "1.0.0"
                    )
                    onInstall(PluginManifest(metadata = metadata))
                },
                enabled = pluginName.isNotBlank() && pluginAuthor.isNotBlank()
            ) {
                Text("安装")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// ============================================================================
// 插件详情弹窗
// ============================================================================

/**
 * 插件详情弹窗
 *
 * 展示插件的完整信息，包括元数据、权限列表和操作按钮。
 *
 * @param plugin 插件元数据
 * @param state 插件状态
 * @param permissions 已授权权限列表
 * @param onDismiss 关闭弹窗回调
 * @param onEnable 启用插件回调
 * @param onDisable 禁用插件回调
 * @param onUninstall 卸载插件回调
 * @param onUpdate 更新插件回调
 * @param onManagePermissions 管理权限回调
 */
@Composable
private fun PluginDetailDialog(
    plugin: PluginMetadata,
    state: PluginState?,
    permissions: List<PluginPermission>,
    onDismiss: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onUninstall: () -> Unit,
    onUpdate: () -> Unit,
    onManagePermissions: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(plugin.name)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 基本信息
                DetailRow(label = "版本", value = "v${plugin.version}")
                DetailRow(label = "作者", value = plugin.author)
                DetailRow(label = "最低应用版本", value = plugin.minAppVersion)
                DetailRow(label = "安装日期", value = formatDate(plugin.installDate))
                DetailRow(label = "最后更新", value = formatDate(plugin.lastUpdateDate))

                Spacer(modifier = Modifier.height(4.dp))

                // 描述
                Text(
                    text = plugin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 当前状态
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "状态：",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    PluginStateBadge(state = state)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 权限信息
                Text(
                    text = "所需权限：",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (plugin.permissions.isEmpty()) {
                    Text(
                        text = "无特殊权限要求",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        plugin.permissions.forEach { permission ->
                            val isGranted = permission in permissions
                            FilterChip(
                                selected = isGranted,
                                onClick = { /* 在权限管理对话框中操作 */ },
                                label = { Text(getPermissionDisplayName(permission), style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = if (isGranted) {
                                    { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 操作按钮组
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 启用/禁用按钮
                    if (state == PluginState.ENABLED) {
                        FilledTonalButton(
                            onClick = onDisable,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ToggleOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("禁用")
                        }
                    } else {
                        Button(
                            onClick = onEnable,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ToggleOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("启用")
                        }
                    }

                    // 更新按钮
                    OutlinedButton(
                        onClick = onUpdate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("更新")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 权限管理按钮
                    OutlinedButton(
                        onClick = onManagePermissions,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("权限")
                    }

                    // 卸载按钮
                    OutlinedButton(
                        onClick = onUninstall,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("卸载")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

// ============================================================================
// 权限管理对话框
// ============================================================================

/**
 * 权限管理对话框
 *
 * 展示插件所需的所有权限，允许用户授予或撤销每项权限。
 *
 * @param plugin 插件元数据
 * @param grantedPermissions 已授权权限列表
 * @param onGrant 授予权限回调
 * @param onRevoke 撤销权限回调
 * @param onDismiss 关闭对话框回调
 */
@Composable
private fun PermissionManageDialog(
    plugin: PluginMetadata,
    grantedPermissions: List<PluginPermission>,
    onGrant: (PluginPermission) -> Unit,
    onRevoke: (PluginPermission) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("权限管理 - ${plugin.name}") },
        text = {
            if (plugin.permissions.isEmpty()) {
                Text(
                    text = "该插件无需特殊权限",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    plugin.permissions.forEach { permission ->
                        val isGranted = permission in grantedPermissions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = getPermissionDisplayName(permission),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = getPermissionDescription(permission),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isGranted,
                                onCheckedChange = { checked ->
                                    if (checked) onGrant(permission) else onRevoke(permission)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}

// ============================================================================
// 辅助组件和工具函数
// ============================================================================

/**
 * 详情行组件
 *
 * @param label 标签文本
 * @param value 值文本
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 格式化时间戳为可读日期
 *
 * @param timestamp 时间戳（毫秒）
 * @return 格式化后的日期字符串
 */
private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

/**
 * 获取权限的中文显示名称
 *
 * @param permission 插件权限枚举
 * @return 权限的中文显示名称
 */
private fun getPermissionDisplayName(permission: PluginPermission): String {
    return when (permission) {
        PluginPermission.CLONE_ACCESS -> "克隆访问"
        PluginPermission.FREEZE_ACCESS -> "冻结访问"
        PluginPermission.BACKUP_ACCESS -> "备份访问"
        PluginPermission.NETWORK_ACCESS -> "网络访问"
        PluginPermission.STORAGE_ACCESS -> "存储访问"
        PluginPermission.NOTIFICATION_ACCESS -> "通知访问"
        PluginPermission.SYSTEM_ACCESS -> "系统访问"
    }
}

/**
 * 获取权限的中文描述
 *
 * @param permission 插件权限枚举
 * @return 权限的中文描述文本
 */
private fun getPermissionDescription(permission: PluginPermission): String {
    return when (permission) {
        PluginPermission.CLONE_ACCESS -> "允许插件操作克隆应用"
        PluginPermission.FREEZE_ACCESS -> "允许插件使用应用冻结功能"
        PluginPermission.BACKUP_ACCESS -> "允许插件读取和创建备份"
        PluginPermission.NETWORK_ACCESS -> "允许插件进行网络通信"
        PluginPermission.STORAGE_ACCESS -> "允许插件读写设备存储"
        PluginPermission.NOTIFICATION_ACCESS -> "允许插件发送系统通知"
        PluginPermission.SYSTEM_ACCESS -> "允许插件执行系统级操作"
    }
}
