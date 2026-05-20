package com.appclonex.presentation.screens.appdetail

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.appclonex.R
import com.appclonex.infrastructure.caching.ApplicationIconModel
import com.appclonex.presentation.screen.appdetail.ApplicationDetailScreenViewModel
import com.appclonex.presentation.util.PermissionNameMapper

/**
 * 应用详情屏幕
 *
 * 展示克隆应用的详细信息，包括应用信息、操作按钮、存储详情和权限列表。
 * 所有业务操作由 ViewModel 处理。
 *
 * @param packageName 应用包名
 * @param userId 用户 ID（多用户场景）
 * @param onNavigateBack 返回上一页的回调
 * @param onDeleteSuccess 删除成功后的回调（如返回上一页）
 * @param viewModel 应用详情 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    userId: Int = 0,
    onNavigateBack: () -> Unit,
    onDeleteSuccess: () -> Unit = {},
    viewModel: ApplicationDetailScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 加载应用详情
    LaunchedEffect(packageName, userId) {
        viewModel.loadAppDetail(packageName, userId)
    }

    // 显示消息
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    // 显示错误
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // 删除成功后返回（仅当消息为删除成功时触发）
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onDeleteSuccess()
        }
    }

    val app = uiState.app
    val appName = app?.getDisplayName() ?: extractAppNameFromPackage(packageName)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = appName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 应用信息卡片
                AppInfoCard(
                    packageName = packageName,
                    userId = userId,
                    appName = appName,
                    versionName = app?.versionName,
                    createdAt = app?.createdAt
                )

                // 操作按钮行
                ActionButtonsRow(
                    isFrozen = uiState.isFrozen,
                    isProcessing = uiState.isProcessing,
                    onLaunch = { viewModel.launchApp(packageName, userId) },
                    onFreezeToggle = { viewModel.toggleFreeze(packageName, userId) },
                    onClearData = { viewModel.clearAppData(packageName, userId) },
                    onUninstall = { viewModel.deleteApp(packageName, userId) }
                )

                // 存储信息卡片
                StorageInfoCard(
                    cacheSize = uiState.cacheSize,
                    dataSize = uiState.dataSize,
                    totalSize = uiState.totalSize
                )

                // 权限列表
                PermissionsSection(permissions = uiState.permissions)

                // 底部操作按钮
                BottomActionButtons(
                    onClearCache = { viewModel.clearCache(packageName, userId) },
                    onClearData = { viewModel.clearAppData(packageName, userId) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 从包名提取应用名称（占位逻辑）
 */
private fun extractAppNameFromPackage(packageName: String): String {
    return packageName.substringAfterLast(".")
}

/**
 * 应用信息卡片
 */
@Composable
private fun AppInfoCard(
    packageName: String,
    userId: Int,
    appName: String,
    versionName: String?,
    createdAt: Long?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 应用图标 - 使用 Coil AsyncImage
                AsyncImage(
                    model = ApplicationIconModel(packageName),
                    contentDescription = appName,
                    modifier = Modifier.size(64.dp),
                    placeholder = rememberAsyncImagePainter(
                        model = null,
                        placeholder = painterResource(android.R.drawable.sym_def_app_icon)
                    ),
                    error = rememberAsyncImagePainter(
                        model = null,
                        placeholder = painterResource(android.R.drawable.sym_def_app_icon)
                    )
                )

                Spacer(modifier = Modifier.width(16.dp))

                // 应用名称和包名
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // 详细信息行
            InfoRow(
                icon = Icons.Default.CalendarToday,
                label = stringResource(R.string.detail_install_date),
                value = createdAt?.let { formatDate(it) } ?: "--"
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(
                icon = Icons.Default.Info,
                label = stringResource(R.string.detail_version),
                value = versionName ?: "--"
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(
                icon = Icons.Default.Memory,
                label = stringResource(R.string.detail_user_id),
                value = userId.toString()
            )
        }
    }
}

/**
 * 格式化时间戳为日期字符串
 */
private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

/**
 * 信息行组件
 */
@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 操作按钮行
 */
@Composable
private fun ActionButtonsRow(
    isFrozen: Boolean,
    isProcessing: Boolean,
    onLaunch: () -> Unit,
    onFreezeToggle: () -> Unit,
    onClearData: () -> Unit,
    onUninstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_preferences),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 启动按钮
                FilledTonalButton(
                    onClick = onLaunch,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing && !isFrozen
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.detail_launch), style = MaterialTheme.typography.labelMedium)
                }

                // 冻结/解冻按钮
                OutlinedButton(
                    onClick = onFreezeToggle,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing
                ) {
                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFrozen) stringResource(R.string.detail_unfreeze) else stringResource(R.string.detail_freeze),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 清除数据按钮
                OutlinedButton(
                    onClick = onClearData,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.detail_clear_data), style = MaterialTheme.typography.labelMedium)
                }

                // 卸载按钮（红色）
                OutlinedButton(
                    onClick = onUninstall,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.detail_uninstall), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * 存储信息卡片
 */
@Composable
private fun StorageInfoCard(
    cacheSize: String,
    dataSize: String,
    totalSize: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.detail_storage_info),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            StorageInfoRow(label = stringResource(R.string.detail_cache_size), value = cacheSize)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            StorageInfoRow(label = stringResource(R.string.detail_data_size), value = dataSize)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            StorageInfoRow(
                label = stringResource(R.string.detail_total_size),
                value = totalSize,
                isTotal = true
            )
        }
    }
}

/**
 * 存储信息行组件
 */
@Composable
private fun StorageInfoRow(
    label: String,
    value: String,
    isTotal: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isTotal) {
                MaterialTheme.typography.bodyLarge
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isTotal) FontWeight.SemiBold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = if (isTotal) {
                MaterialTheme.typography.bodyLarge
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Medium,
            color = if (isTotal) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/**
 * 权限列表区域
 */
@Composable
private fun PermissionsSection(permissions: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.detail_permissions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (permissions.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_no_apps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                permissions.forEachIndexed { index, permission ->
                    PermissionItem(permission = permission)
                    if (index < permissions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 权限项组件
 */
@Composable
private fun PermissionItem(
    permission: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.VerifiedUser,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = PermissionNameMapper.getDisplayName(permission),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = permission,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 底部操作按钮
 */
@Composable
private fun BottomActionButtons(
    onClearCache: () -> Unit,
    onClearData: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 清除缓存按钮
            OutlinedButton(
                onClick = onClearCache,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.detail_clear_cache), style = MaterialTheme.typography.labelLarge)
            }

            // 清除数据按钮
            OutlinedButton(
                onClick = onClearData,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.detail_clear_data_action), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
