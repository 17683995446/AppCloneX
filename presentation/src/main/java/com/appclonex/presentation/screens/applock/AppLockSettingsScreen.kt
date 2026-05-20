package com.appclonex.presentation.screens.applock

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appclonex.R
import com.appclonex.domain.model.AppLockConfiguration
import com.appclonex.presentation.screen.applock.AppLockSettingsViewModel

/**
 * 应用锁设置屏幕
 *
 * 允许用户配置应用锁设置，包括 PIN 锁、生物识别锁以及管理已锁定应用列表。
 *
 * @param onNavigateBack 返回上一页的回调
 * @param viewModel 应用锁设置 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppLockSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPinDialog by remember { mutableStateOf(false) }
    var showAppPickerDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var newPackageName by remember { mutableStateOf("") }

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

    // PIN 设置完成后关闭对话框
    LaunchedEffect(uiState.isSettingPin) {
        if (!uiState.isSettingPin && pinInput.isNotEmpty()) {
            showPinDialog = false
            pinInput = ""
        }
    }

    // PIN 设置对话框
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                pinInput = ""
            },
            title = { Text(stringResource(R.string.applock_pin_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            pinInput = it
                        }
                    },
                    label = { Text(stringResource(R.string.applock_pin_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinInput.length == 4) {
                            viewModel.setPin(pinInput)
                        }
                    },
                    enabled = pinInput.length == 4 && !uiState.isSettingPin
                ) {
                    Text(stringResource(R.string.applock_pin_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    pinInput = ""
                }) {
                    Text(stringResource(R.string.applock_pin_cancel))
                }
            }
        )
    }

    // 应用选择对话框
    if (showAppPickerDialog) {
        AlertDialog(
            onDismissRequest = {
                showAppPickerDialog = false
                newPackageName = ""
            },
            title = { Text(stringResource(R.string.applock_app_picker_title)) },
            text = {
                OutlinedTextField(
                    value = newPackageName,
                    onValueChange = { newPackageName = it },
                    label = { Text(stringResource(R.string.applock_package_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPackageName.isNotBlank()) {
                            viewModel.lockApp(newPackageName.trim())
                            showAppPickerDialog = false
                            newPackageName = ""
                        }
                    },
                    enabled = newPackageName.isNotBlank()
                ) {
                    Text(stringResource(R.string.applock_lock_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAppPickerDialog = false
                    newPackageName = ""
                }) {
                    Text(stringResource(R.string.applock_pin_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.applock_title)) },
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
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 当前锁状态卡片
                    item {
                        LockStatusCard(
                            currentLockType = uiState.currentLockType
                        )
                    }

                    // 锁设置操作按钮
                    item {
                        LockActionButtons(
                            currentLockType = uiState.currentLockType,
                            isSettingPin = uiState.isSettingPin,
                            isEnrollingBiometric = uiState.isEnrollingBiometric,
                            onSetPinClick = { showPinDialog = true },
                            onRemovePinClick = { viewModel.removePin() },
                            onSetBiometricClick = { viewModel.enrollBiometric() }
                        )
                    }

                    // 已锁定应用列表
                    item {
                        Text(
                            text = stringResource(R.string.applock_locked_apps),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (uiState.lockedApps.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.applock_no_locked_apps),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = uiState.lockedApps,
                            key = { it }
                        ) { packageName ->
                            LockedAppItem(
                                packageName = packageName,
                                onUnlock = { viewModel.unlockApp(packageName) }
                            )
                        }
                    }

                    // 锁定新应用按钮
                    item {
                        OutlinedButton(
                            onClick = { showAppPickerDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.applock_lock_new))
                        }
                    }

                    // 底部间距
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * 当前锁状态卡片
 */
@Composable
private fun LockStatusCard(
    currentLockType: AppLockConfiguration.LockType
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (currentLockType) {
                AppLockConfiguration.LockType.NONE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                AppLockConfiguration.LockType.PIN -> MaterialTheme.colorScheme.primaryContainer
                AppLockConfiguration.LockType.BIOMETRIC -> MaterialTheme.colorScheme.secondaryContainer
                AppLockConfiguration.LockType.PATTERN -> MaterialTheme.colorScheme.tertiaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (currentLockType) {
                    AppLockConfiguration.LockType.NONE -> Icons.Default.LockOpen
                    AppLockConfiguration.LockType.PIN -> Icons.Default.Pin
                    AppLockConfiguration.LockType.BIOMETRIC -> Icons.Default.Fingerprint
                    AppLockConfiguration.LockType.PATTERN -> Icons.Default.Lock
                },
                contentDescription = null,
                tint = when (currentLockType) {
                    AppLockConfiguration.LockType.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
                    AppLockConfiguration.LockType.PIN -> MaterialTheme.colorScheme.primary
                    AppLockConfiguration.LockType.BIOMETRIC -> MaterialTheme.colorScheme.secondary
                    AppLockConfiguration.LockType.PATTERN -> MaterialTheme.colorScheme.tertiary
                },
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = stringResource(R.string.applock_current_status),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (currentLockType) {
                        AppLockConfiguration.LockType.NONE -> stringResource(R.string.applock_status_none)
                        AppLockConfiguration.LockType.PIN -> stringResource(R.string.applock_status_pin)
                        AppLockConfiguration.LockType.BIOMETRIC -> stringResource(R.string.applock_status_biometric)
                        AppLockConfiguration.LockType.PATTERN -> stringResource(R.string.applock_status_none)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (currentLockType) {
                        AppLockConfiguration.LockType.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
                        AppLockConfiguration.LockType.PIN -> MaterialTheme.colorScheme.primary
                        AppLockConfiguration.LockType.BIOMETRIC -> MaterialTheme.colorScheme.secondary
                        AppLockConfiguration.LockType.PATTERN -> MaterialTheme.colorScheme.tertiary
                    }
                )
            }
        }
    }
}

/**
 * 锁设置操作按钮
 */
@Composable
private fun LockActionButtons(
    currentLockType: AppLockConfiguration.LockType,
    isSettingPin: Boolean,
    isEnrollingBiometric: Boolean,
    onSetPinClick: () -> Unit,
    onRemovePinClick: () -> Unit,
    onSetBiometricClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // PIN 锁按钮
            if (currentLockType == AppLockConfiguration.LockType.PIN) {
                OutlinedButton(
                    onClick = onRemovePinClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.applock_remove_pin))
                }
            } else {
                FilledTonalButton(
                    onClick = onSetPinClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSettingPin
                ) {
                    Icon(
                        imageVector = Icons.Default.Pin,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.applock_set_pin))
                }
            }

            // 生物识别锁按钮
            FilledTonalButton(
                onClick = onSetBiometricClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !isEnrollingBiometric
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.applock_set_biometric))
            }
        }
    }
}

/**
 * 已锁定应用项
 */
@Composable
private fun LockedAppItem(
    packageName: String,
    onUnlock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = packageName.substringAfterLast("."),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onUnlock,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.applock_unlock))
            }
        }
    }
}
