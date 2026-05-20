package com.appclonex.presentation.screens.clone

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appclonex.domain.model.*

// ─────────────────────────────────────────────────────────────────────────────
// 主入口
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchCloneWizardScreen(
    viewModel: BatchCloneWizardViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.currentStep) {
                            WizardStep.SELECT_PROFILE -> "选择配置文件"
                            WizardStep.SELECT_APPS -> "选择应用"
                            WizardStep.CONFIGURE_OPTIONS -> "克隆选项"
                            WizardStep.REVIEW_CONFIRM -> "确认克隆"
                            WizardStep.PROGRESS -> "克隆进度"
                            WizardStep.SUMMARY -> "克隆完成"
                            else -> "批量克隆"
                        }
                    )
                },
                navigationIcon = {
                    if (uiState.currentStep != WizardStep.PROGRESS) {
                        IconButton(onClick = {
                            if (uiState.currentStep == WizardStep.SELECT_PROFILE) {
                                onNavigateBack()
                            } else {
                                viewModel.previousStep()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 步骤指示器（进度步骤和摘要步骤不显示）
            if (uiState.currentStep != WizardStep.PROGRESS &&
                uiState.currentStep != WizardStep.SUMMARY
            ) {
                StepIndicator(
                    currentStep = uiState.currentStep,
                    totalSteps = WizardStep.TOTAL_STEPS - 2, // 不含进度和摘要
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            // 步骤内容
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (uiState.currentStep) {
                    WizardStep.SELECT_PROFILE -> {
                        StepSelectProfile(
                            profiles = uiState.profiles,
                            selectedProfile = uiState.selectedProfile,
                            onProfileSelected = viewModel::selectProfile
                        )
                    }
                    WizardStep.SELECT_APPS -> {
                        StepSelectApps(
                            apps = filteredApps,
                            selectedPackages = uiState.selectedPackages,
                            searchQuery = uiState.searchQuery,
                            onToggleApp = viewModel::toggleApp,
                            onSelectAll = viewModel::selectAllApps,
                            onDeselectAll = viewModel::deselectAllApps,
                            onSearchQueryChanged = viewModel::updateSearchQuery
                        )
                    }
                    WizardStep.CONFIGURE_OPTIONS -> {
                        StepConfigureOptions(
                            options = uiState.options,
                            onSkipExistingChanged = viewModel::setSkipExisting,
                            onAutoFreezeChanged = viewModel::setAutoFreeze,
                            onCopySettingsChanged = viewModel::setCopySettings,
                            onParallelCloneChanged = viewModel::setParallelClone,
                            onMaxParallelChanged = viewModel::setMaxParallel,
                            onShowProgressChanged = viewModel::setShowProgress,
                            onNotifyOnCompleteChanged = viewModel::setNotifyOnComplete
                        )
                    }
                    WizardStep.REVIEW_CONFIRM -> {
                        StepReviewConfirm(
                            selectedProfile = uiState.selectedProfile,
                            selectedApps = uiState.allApps.filter { it.packageName in uiState.selectedPackages },
                            options = uiState.options
                        )
                    }
                    WizardStep.PROGRESS -> {
                        StepProgress(progress = uiState.progress)
                    }
                    WizardStep.SUMMARY -> {
                        StepSummary(
                            summary = uiState.summary,
                            onRetryFailed = viewModel::retryFailed,
                            onResetWizard = viewModel::resetWizard,
                            onNavigateBack = onNavigateBack
                        )
                    }
                }
            }

            // 底部导航按钮
            BottomNavigation(
                currentStep = uiState.currentStep,
                isLoading = uiState.isLoading,
                canProceed = viewModel.canProceedToReview(),
                onNext = viewModel::nextStep,
                onBack = viewModel::previousStep,
                onStartClone = viewModel::startClone,
                onCancelClone = viewModel::cancelClone
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 步骤指示器
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val stepLabels = listOf("配置文件", "选择应用", "选项", "确认")

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        stepLabels.forEachIndexed { index, label ->
            val isActive = index <= currentStep
            val isCurrent = index == currentStep

            StepDot(
                label = label,
                stepNumber = index + 1,
                isActive = isActive,
                isCurrent = isCurrent,
                modifier = Modifier.weight(1f)
            )

            if (index < stepLabels.lastIndex) {
                StepLine(
                    isActive = index < currentStep,
                    modifier = Modifier.weight(0.5f)
                )
            }
        }
    }
}

@Composable
private fun StepDot(
    label: String,
    stepNumber: Int,
    isActive: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            isCurrent -> MaterialTheme.colorScheme.primary
            isActive -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "stepColor"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            isCurrent || isActive -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "stepContentColor"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCurrent) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                } else if (isActive) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = stepNumber.toString(),
                        color = contentColor,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isCurrent) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StepLine(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "lineColor"
    )
    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(2.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(1.dp))
            .background(color)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 步骤 1：选择配置文件
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepSelectProfile(
    profiles: List<ProfileItem>,
    selectedProfile: ProfileItem?,
    onProfileSelected: (ProfileItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "请选择要将应用克隆到的目标配置文件",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(profiles, key = { it.id }) { profile ->
            ProfileCard(
                profile = profile,
                isSelected = selectedProfile?.id == profile.id,
                onClick = { onProfileSelected(profile) }
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ProfileItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        label = "profileBorder"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "已包含 ${profile.appCount} 个应用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 步骤 2：选择应用
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepSelectApps(
    apps: List<AppItem>,
    selectedPackages: Set<String>,
    searchQuery: String,
    onToggleApp: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onSearchQueryChanged: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 搜索栏
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            label = { Text("搜索应用") },
            placeholder = { Text("输入应用名称或包名") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        )

        // 全选 / 取消全选
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已选择 ${selectedPackages.size} / ${apps.size} 个应用",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                TextButton(onClick = onSelectAll) {
                    Text("全选")
                }
                TextButton(onClick = onDeselectAll) {
                    Text("取消全选")
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // 应用列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppCheckboxItem(
                    app = app,
                    isChecked = app.packageName in selectedPackages,
                    onCheckedChange = { onToggleApp(app.packageName) }
                )
            }
        }
    }
}

@Composable
private fun AppCheckboxItem(
    app: AppItem,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 应用图标占位
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (app.isAlreadyCloned) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "已克隆",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 步骤 3：配置选项
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepConfigureOptions(
    options: BatchCloneOptions,
    onSkipExistingChanged: (Boolean) -> Unit,
    onAutoFreezeChanged: (Boolean) -> Unit,
    onCopySettingsChanged: (Boolean) -> Unit,
    onParallelCloneChanged: (Boolean) -> Unit,
    onMaxParallelChanged: (Int) -> Unit,
    onShowProgressChanged: (Boolean) -> Unit,
    onNotifyOnCompleteChanged: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 克隆行为
        item {
            Text(
                text = "克隆行为",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            OptionSwitchItem(
                title = "跳过已克隆的应用",
                description = "如果目标配置文件中已存在该应用，则自动跳过",
                checked = options.skipExisting,
                onCheckedChange = onSkipExistingChanged
            )
        }

        item {
            OptionSwitchItem(
                title = "复制原应用设置",
                description = "将原应用的数据和设置复制到克隆版本",
                checked = options.copySettings,
                onCheckedChange = onCopySettingsChanged
            )
        }

        item {
            OptionSwitchItem(
                title = "克隆后自动冻结",
                description = "克隆完成后自动冻结应用，不占用系统资源",
                checked = options.autoFreeze,
                onCheckedChange = onAutoFreezeChanged
            )
        }

        // 并行设置
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "并行设置",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            OptionSwitchItem(
                title = "并行克隆",
                description = "同时克隆多个应用以加快速度",
                checked = options.parallelClone,
                onCheckedChange = onParallelCloneChanged
            )
        }

        item {
            if (options.parallelClone) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "最大并行数",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${options.maxParallel}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = options.maxParallel.toFloat(),
                            onValueChange = { onMaxParallelChanged(it.toInt()) },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "1",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "5",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 通知设置
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "通知设置",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            OptionSwitchItem(
                title = "显示进度通知",
                description = "在通知栏显示克隆进度",
                checked = options.showProgress,
                onCheckedChange = onShowProgressChanged
            )
        }

        item {
            OptionSwitchItem(
                title = "完成后通知",
                description = "全部克隆完成后发送通知",
                checked = options.notifyOnComplete,
                onCheckedChange = onNotifyOnCompleteChanged
            )
        }

        // 底部留白
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OptionSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 步骤 4：确认
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepReviewConfirm(
    selectedProfile: ProfileItem?,
    selectedApps: List<AppItem>,
    options: BatchCloneOptions
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 目标配置文件
        item {
            ReviewSectionCard(title = "目标配置文件") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedProfile?.name ?: "未选择",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 克隆应用列表
        item {
            ReviewSectionCard(title = "克隆应用 (${selectedApps.size})") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedApps.forEach { app ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Android,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = app.versionName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 克隆选项
        item {
            ReviewSectionCard(title = "克隆选项") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReviewOptionRow(
                        label = "跳过已克隆",
                        value = if (options.skipExisting) "是" else "否"
                    )
                    ReviewOptionRow(
                        label = "自动冻结",
                        value = if (options.autoFreeze) "是" else "否"
                    )
                    ReviewOptionRow(
                        label = "复制设置",
                        value = if (options.copySettings) "是" else "否"
                    )
                    ReviewOptionRow(
                        label = "并行克隆",
                        value = if (options.parallelClone) "是 (${options.maxParallel}个)" else "否"
                    )
                    ReviewOptionRow(
                        label = "完成后通知",
                        value = if (options.notifyOnComplete) "是" else "否"
                    )
                }
            }
        }

        // 提示
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "请确认以上信息无误后点击开始克隆",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReviewSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ReviewOptionRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 步骤 5：进度
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepProgress(progress: BatchCloneProgress) {
    val fraction = if (progress.totalApps > 0) {
        (progress.completedApps + progress.failedApps).toFloat() / progress.totalApps
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 状态图标
        when (progress.currentStatus) {
            CloneStatus.PREPARING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "准备中...",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            CloneStatus.CLONING, CloneStatus.INSTALLING, CloneStatus.CONFIGURING -> {
                // 大进度环
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.size(120.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "${(fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                // 当前应用
                if (progress.currentApp.isNotEmpty()) {
                    Text(
                        text = "正在克隆: $progress.currentApp",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 状态文本
                Text(
                    text = statusToText(progress.currentStatus),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CloneStatus.COMPLETED -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "克隆完成!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            CloneStatus.CANCELLED -> {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "克隆已取消",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            CloneStatus.FAILED -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "克隆失败",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            CloneStatus.IDLE -> {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "等待开始...",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 统计信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "总计",
                value = progress.totalApps.toString(),
                color = MaterialTheme.colorScheme.onSurface
            )
            StatItem(
                label = "成功",
                value = progress.completedApps.toString(),
                color = MaterialTheme.colorScheme.primary
            )
            StatItem(
                label = "失败",
                value = progress.failedApps.toString(),
                color = MaterialTheme.colorScheme.error
            )
            StatItem(
                label = "剩余",
                value = (progress.totalApps - progress.completedApps - progress.failedApps)
                    .coerceAtLeast(0).toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 线性进度条
        if (progress.currentStatus == CloneStatus.CLONING ||
            progress.currentStatus == CloneStatus.INSTALLING ||
            progress.currentStatus == CloneStatus.CONFIGURING
        ) {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${progress.completedApps + progress.failedApps} / ${progress.totalApps}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 已完成的应用列表
        if (progress.results.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "已完成的应用",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(progress.results, key = { it.packageName }) { result ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (result.success) Icons.Default.CheckCircle
                            else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (result.success) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = result.appName,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!result.success && result.errorMessage != null) {
                            Text(
                                text = result.errorMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
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

private fun statusToText(status: CloneStatus): String = when (status) {
    CloneStatus.IDLE -> "空闲"
    CloneStatus.PREPARING -> "正在准备..."
    CloneStatus.CLONING -> "正在克隆应用数据..."
    CloneStatus.INSTALLING -> "正在安装..."
    CloneStatus.CONFIGURING -> "正在配置..."
    CloneStatus.COMPLETED -> "已完成"
    CloneStatus.FAILED -> "失败"
    CloneStatus.CANCELLED -> "已取消"
}

// ─────────────────────────────────────────────────────────────────────────────
// 步骤 6：摘要
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepSummary(
    summary: BatchCloneSummary?,
    onRetryFailed: () -> Unit,
    onResetWizard: () -> Unit,
    onNavigateBack: () -> Unit
) {
    if (summary == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val hasFailures = summary.failedCount > 0
    val allSuccess = summary.failedCount == 0 && summary.successCount > 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 结果图标
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Icon(
                imageVector = if (allSuccess) Icons.Default.CheckCircle
                else if (hasFailures) Icons.Default.Warning
                else Icons.Default.Info,
                contentDescription = null,
                tint = when {
                    allSuccess -> MaterialTheme.colorScheme.primary
                    hasFailures -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    allSuccess -> "全部克隆成功!"
                    hasFailures -> "部分克隆失败"
                    else -> "克隆完成"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "耗时 ${formatDuration(summary.totalDurationMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 统计卡片
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryStatColumn(
                        label = "总计",
                        value = summary.totalApps.toString(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SummaryStatColumn(
                        label = "成功",
                        value = summary.successCount.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    SummaryStatColumn(
                        label = "失败",
                        value = summary.failedCount.toString(),
                        color = MaterialTheme.colorScheme.error
                    )
                    SummaryStatColumn(
                        label = "跳过",
                        value = summary.skippedCount.toString(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 失败应用列表
        if (hasFailures) {
            item {
                Text(
                    text = "失败的应用",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            items(
                summary.results.filter { !it.success },
                key = { it.packageName }
            ) { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = result.appName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (result.errorMessage != null) {
                                Text(
                                    text = result.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // 重试按钮
            item {
                Button(
                    onClick = onRetryFailed,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重试失败的应用")
                }
            }
        }

        // 成功应用列表
        if (summary.successCount > 0) {
            item {
                Text(
                    text = "成功克隆的应用",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            items(
                summary.results.filter { it.success },
                key = { it.packageName }
            ) { result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = result.appName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = formatDuration(result.durationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 操作按钮
        item {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onResetWizard,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("重新开始")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("完成")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryStatColumn(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 底部导航按钮
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BottomNavigation(
    currentStep: Int,
    isLoading: Boolean,
    canProceed: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onStartClone: () -> Unit,
    onCancelClone: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        when (currentStep) {
            WizardStep.PROGRESS -> {
                // 进度步骤只显示取消按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onCancelClone,
                        modifier = Modifier.fillMaxWidth(0.6f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("取消克隆")
                    }
                }
            }
            WizardStep.SUMMARY -> {
                // 摘要步骤不显示底部按钮（按钮在内容区域）
                Box(modifier = Modifier.padding(12.dp))
            }
            WizardStep.REVIEW_CONFIRM -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        Text("返回")
                    }
                    Button(
                        onClick = onStartClone,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = canProceed && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("开始克隆")
                    }
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 第一步不显示返回按钮
                    if (currentStep > WizardStep.SELECT_PROFILE) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("上一步")
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = when (currentStep) {
                            WizardStep.SELECT_PROFILE -> canProceed
                            WizardStep.SELECT_APPS -> canProceed
                            else -> true
                        }
                    ) {
                        Text("下一步")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 工具函数
// ─────────────────────────────────────────────────────────────────────────────
private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    return when {
        seconds < 60 -> "${seconds}秒"
        else -> {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            "${minutes}分${remainingSeconds}秒"
        }
    }
}
