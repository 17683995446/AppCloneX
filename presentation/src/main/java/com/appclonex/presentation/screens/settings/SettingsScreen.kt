package com.appclonex.presentation.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.appclonex.R
import com.appclonex.domain.model.ThemeMode
import com.appclonex.presentation.screen.settings.ApplicationSettingsScreenViewModel

/**
 * 应用设置屏幕
 *
 * 管理应用偏好、数据备份/恢复、关于信息。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ApplicationSettingsScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 文件选择器，用于导入配置
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.performRestore(it.toString())
        }
    }

    // 显示消息 Snackbar
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    // 显示错误 Snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ===== 应用偏好 =====
                SectionTitle(text = stringResource(R.string.settings_preferences))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_exclude_system)) },
                            supportingContent = { Text(stringResource(R.string.settings_exclude_system_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = uiState.excludeSystemApps,
                                    onCheckedChange = { viewModel.setExcludeSystemApps(it) }
                                )
                            }
                        )

                        HorizontalDivider()

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_notifications)) },
                            supportingContent = { Text(stringResource(R.string.settings_notifications_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = uiState.showNotifications,
                                    onCheckedChange = { viewModel.setShowNotifications(it) }
                                )
                            }
                        )

                        HorizontalDivider()

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_auto_start)) },
                            supportingContent = { Text(stringResource(R.string.settings_auto_start_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = uiState.autoStartProfile,
                                    onCheckedChange = { viewModel.setAutoStartProfile(it) }
                                )
                            }
                        )

                        HorizontalDivider()

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_dark_theme)) },
                            supportingContent = { Text(stringResource(R.string.settings_dark_theme_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = uiState.darkTheme,
                                    onCheckedChange = { viewModel.setDarkTheme(it) }
                                )
                            }
                        )
                    }
                }

                // ===== 数据管理 =====
                SectionTitle(text = stringResource(R.string.settings_data_management))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.performBackup() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isBackupLoading
                        ) {
                            Text(stringResource(R.string.settings_export))
                        }

                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isRestoreLoading
                        ) {
                            Text(stringResource(R.string.settings_import))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(R.string.settings_last_backup, viewModel.getFormattedLastBackupTime()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ===== 关于 =====
                SectionTitle(text = stringResource(R.string.settings_about))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_version)) },
                            supportingContent = { Text(stringResource(R.string.settings_version_value)) }
                        )

                        HorizontalDivider()

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_profile_count, uiState.totalProfiles)) },
                            supportingContent = { Text("${uiState.totalProfiles}") }
                        )

                        HorizontalDivider()

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_cloned_count, uiState.totalClonedApps)) },
                            supportingContent = { Text("${uiState.totalClonedApps}") }
                        )
                    }
                }

                // 底部间距
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 备份/恢复加载遮罩
            if (uiState.isBackupLoading || uiState.isRestoreLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/**
 * 分区标题组件
 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}
