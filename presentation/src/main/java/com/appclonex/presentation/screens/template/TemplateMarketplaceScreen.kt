/*
 * Copyright (c) 2024 AppCloneX
 *
 * 模板市场界面
 * 提供模板浏览、搜索、筛选、分享、导入、评分等功能的完整 UI
 */

package com.appclonex.presentation.screens.template

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.appclonex.domain.model.CloneTemplate
import com.appclonex.domain.model.SortField
import com.appclonex.domain.model.TemplateCategory

/**
 * 模板市场界面
 * 提供模板浏览、搜索、分类筛选、排序、分享、导入、评分等完整功能
 *
 * @param onNavigateBack 返回上一页的回调
 * @param viewModel 模板市场 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateMarketplaceScreen(
    onNavigateBack: () -> Unit,
    viewModel: TemplateMarketplaceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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

    // ==================== 对话框和底部弹窗 ====================

    // 模板详情底部弹窗
    val selectedTemplate = uiState.selectedTemplate
    if (uiState.showDetailSheet && selectedTemplate != null) {
        TemplateDetailBottomSheet(
            template = selectedTemplate,
            onDismiss = { viewModel.dismissDetailSheet() },
            onShare = { viewModel.showShareDialog(selectedTemplate) },
            onRate = { viewModel.showRatingDialog(selectedTemplate) },
            onImport = { /* 从详情页导入暂不实现 */ }
        )
    }

    // 分享对话框
    if (uiState.showShareDialog) {
        ShareDialog(
            shareInfo = uiState.shareInfo,
            shareProgress = uiState.shareProgress,
            onShare = { viewModel.shareTemplate() },
            onDismiss = { viewModel.dismissShareDialog() }
        )
    }

    // 导入对话框
    if (uiState.showImportDialog) {
        ImportDialog(
            shareCode = uiState.importShareCode,
            importProgress = uiState.importProgress,
            onShareCodeChange = { viewModel.updateImportShareCode(it) },
            onImport = { viewModel.importTemplate() },
            onDismiss = { viewModel.dismissImportDialog() }
        )
    }

    // 评分对话框
    if (uiState.showRatingDialog && selectedTemplate != null) {
        RatingDialog(
            templateName = selectedTemplate.name,
            onDismiss = { viewModel.dismissRatingDialog() },
            onSubmit = { rating, comment -> viewModel.submitRating(rating, comment) }
        )
    }

    // ==================== 主界面 ====================

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "模板市场") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 导入按钮
                    IconButton(onClick = { viewModel.showImportDialog() }) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "导入模板"
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
        ) {
            // 搜索栏
            SearchBarSection(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) }
            )

            // 分类筛选 Chips
            CategoryFilterSection(
                selectedCategories = uiState.selectedCategories,
                onCategoryToggle = { viewModel.toggleCategory(it) }
            )

            // 排序选项
            SortOptionsSection(
                currentSortField = uiState.sortField,
                onSortChange = { sortField ->
                    viewModel.updateSort(sortField)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // 模板列表
            TemplateListSection(
                templates = uiState.templates,
                isLoading = uiState.isLoading,
                isRefreshing = uiState.isRefreshing,
                onTemplateClick = { viewModel.selectTemplate(it) },
                onRefresh = { viewModel.loadTemplates(isRefresh = true) }
            )
        }
    }
}

// ==================== 搜索栏组件 ====================

/**
 * 搜索栏组件
 * 提供关键词搜索输入框
 */
@Composable
private fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(text = "搜索模板...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "清除搜索",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large
    )
}

// ==================== 分类筛选组件 ====================

/**
 * 分类筛选 Chips 组件
 * 显示所有模板分类，支持多选切换
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryFilterSection(
    selectedCategories: List<TemplateCategory>,
    onCategoryToggle: (TemplateCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TemplateCategory.entries.forEach { category ->
            FilterChip(
                selected = category in selectedCategories,
                onClick = { onCategoryToggle(category) },
                label = { Text(text = getCategoryDisplayName(category)) },
                leadingIcon = {
                    Text(text = getCategoryIcon(category))
                }
            )
        }
    }
}

// ==================== 排序选项组件 ====================

/**
 * 排序选项组件
 * 提供按热门、最新、评分、下载量排序的切换按钮
 */
@Composable
private fun SortOptionsSection(
    currentSortField: SortField,
    onSortChange: (SortField) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SortChip(
            label = "最新",
            isSelected = currentSortField == SortField.UPDATED,
            onClick = { onSortChange(SortField.UPDATED) }
        )
        SortChip(
            label = "热门",
            isSelected = currentSortField == SortField.DOWNLOADS,
            onClick = { onSortChange(SortField.DOWNLOADS) }
        )
        SortChip(
            label = "评分",
            isSelected = currentSortField == SortField.RATING,
            onClick = { onSortChange(SortField.RATING) }
        )
        SortChip(
            label = "名称",
            isSelected = currentSortField == SortField.NAME,
            onClick = { onSortChange(SortField.NAME) }
        )
    }
}

/**
 * 排序选项 Chip
 */
@Composable
private fun SortChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    InputChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        },
        modifier = modifier
    )
}

// ==================== 模板列表组件 ====================

/**
 * 模板列表区域
 * 包含加载状态、空状态和模板卡片列表
 */
@Composable
private fun TemplateListSection(
    templates: List<CloneTemplate>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onTemplateClick: (CloneTemplate) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading && templates.isEmpty()) {
        // 首次加载中
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "正在加载模板...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (templates.isEmpty()) {
        // 空状态
        EmptyMarketplaceState(modifier = modifier.fillMaxSize())
    } else {
        // 模板列表
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            )
        ) {
            // 刷新指示器
            if (isRefreshing) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "正在刷新...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(
                items = templates,
                key = { it.id }
            ) { template ->
                TemplateCard(
                    template = template,
                    onClick = { onTemplateClick(template) }
                )
            }
        }
    }
}

// ==================== 模板卡片组件 ====================

/**
 * 模板卡片组件
 * 显示模板的名称、描述、分类、评分、下载量等信息
 */
@Composable
private fun TemplateCard(
    template: CloneTemplate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 模板图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = template.icon,
                    fontSize = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 模板信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 名称和分类标签
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CategoryBadge(category = template.category)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 描述
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 统计信息行
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 评分
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", 4.5f), // 默认评分展示
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 下载量
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${template.usageCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 应用数量
                    Text(
                        text = "${template.appPackages.size} 个应用",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==================== 分类标签组件 ====================

/**
 * 分类标签组件
 * 以小标签形式显示模板分类
 */
@Composable
private fun CategoryBadge(
    category: TemplateCategory,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = getCategoryColors(category)
    Box(
        modifier = modifier
            .background(
                color = bgColor,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = getCategoryDisplayName(category),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

// ==================== 模板详情底部弹窗 ====================

/**
 * 模板详情底部弹窗
 * 展示模板完整信息、评分列表、分享和评分按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateDetailBottomSheet(
    template: CloneTemplate,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onRate: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // 模板头部信息
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = template.icon,
                        fontSize = androidx.compose.ui.unit.TextUnit(28f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    CategoryBadge(category = template.category)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // 描述
            Text(
                text = "描述",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (template.description.isNotBlank()) template.description else "暂无描述",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 包含的应用
            Text(
                text = "包含应用 (${template.appPackages.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (template.appPackages.isNotEmpty()) {
                template.appPackages.forEach { packageName ->
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            } else {
                Text(
                    text = "暂无应用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 统计信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                StatItem(label = "使用次数", value = "${template.usageCount}")
                StatItem(label = "评分", value = String.format("%.1f", 4.5f))
                StatItem(label = "创建时间", value = formatTimestamp(template.createdAt))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "分享")
                }

                OutlinedButton(
                    onClick = onRate,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "评分")
                }
            }
        }
    }
}

// ==================== 分享对话框 ====================

/**
 * 分享对话框
 * 显示分享进度和分享码，支持复制分享码
 */
@Composable
private fun ShareDialog(
    shareInfo: com.appclonex.domain.model.TemplateShareInfo?,
    shareProgress: ShareProgressUiState,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            // 分享进行中不允许关闭
            if (shareProgress == ShareProgressUiState.Idle ||
                shareProgress is ShareProgressUiState.Completed ||
                shareProgress is ShareProgressUiState.Error
            ) {
                onDismiss()
            }
        },
        title = { Text(text = "分享模板") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (shareProgress) {
                    is ShareProgressUiState.Preparing -> {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "正在准备模板数据...", style = MaterialTheme.typography.bodyMedium)
                    }
                    is ShareProgressUiState.GeneratingCode -> {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "正在生成分享码...", style = MaterialTheme.typography.bodyMedium)
                    }
                    is ShareProgressUiState.Uploading -> {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "正在上传分享信息...", style = MaterialTheme.typography.bodyMedium)
                    }
                    is ShareProgressUiState.Completed -> {
                        if (shareInfo != null) {
                            Text(
                                text = "分享成功!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // 分享码显示
                            Text(
                                text = "分享码",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = shareInfo.shareCode,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 分享链接
                            Text(
                                text = "分享链接",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = shareInfo.shareUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 二维码占位
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.medium
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "二维码",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    is ShareProgressUiState.Error -> {
                        Text(
                            text = "分享失败",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = shareProgress.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is ShareProgressUiState.Idle -> {
                        Text(
                            text = "确定要分享此模板吗？",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "其他用户可以通过分享码导入此模板配置。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (shareProgress) {
                is ShareProgressUiState.Idle -> {
                    Button(onClick = onShare) {
                        Text(text = "分享")
                    }
                }
                is ShareProgressUiState.Completed -> {
                    Button(onClick = onDismiss) {
                        Text(text = "完成")
                    }
                }
                is ShareProgressUiState.Error -> {
                    Button(onClick = onDismiss) {
                        Text(text = "关闭")
                    }
                }
                else -> {
                    // 进度中不显示按钮
                }
            }
        },
        dismissButton = {
            if (shareProgress == ShareProgressUiState.Idle ||
                shareProgress is ShareProgressUiState.Error
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = "取消")
                }
            }
        }
    )
}

// ==================== 导入对话框 ====================

/**
 * 导入对话框
 * 输入分享码并预览模板信息后确认导入
 */
@Composable
private fun ImportDialog(
    shareCode: String,
    importProgress: ImportProgressUiState,
    onShareCodeChange: (String) -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (importProgress == ImportProgressUiState.Idle ||
                importProgress is ImportProgressUiState.Completed ||
                importProgress is ImportProgressUiState.Error
            ) {
                onDismiss()
            }
        },
        title = { Text(text = "导入模板") },
        text = {
            Column {
                when (importProgress) {
                    is ImportProgressUiState.Validating -> {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "正在验证分享码...", style = MaterialTheme.typography.bodyMedium)
                    }
                    is ImportProgressUiState.Downloading -> {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "正在下载模板数据...", style = MaterialTheme.typography.bodyMedium)
                    }
                    is ImportProgressUiState.Parsing -> {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "正在解析模板配置...", style = MaterialTheme.typography.bodyMedium)
                    }
                    is ImportProgressUiState.Merging -> {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "正在合并到本地...", style = MaterialTheme.typography.bodyMedium)
                    }
                    is ImportProgressUiState.Completed -> {
                        Text(
                            text = "导入成功!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "模板: ${importProgress.template.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is ImportProgressUiState.Error -> {
                        Text(
                            text = "导入失败",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = importProgress.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is ImportProgressUiState.Idle -> {
                        Text(
                            text = "请输入模板分享码",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = shareCode,
                            onValueChange = onShareCodeChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(text = "输入8位分享码") },
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (importProgress) {
                is ImportProgressUiState.Idle -> {
                    Button(
                        onClick = onImport,
                        enabled = shareCode.trim().isNotBlank()
                    ) {
                        Text(text = "导入")
                    }
                }
                is ImportProgressUiState.Completed -> {
                    Button(onClick = onDismiss) {
                        Text(text = "完成")
                    }
                }
                is ImportProgressUiState.Error -> {
                    Button(onClick = onDismiss) {
                        Text(text = "关闭")
                    }
                }
                else -> {
                    // 进度中不显示按钮
                }
            }
        },
        dismissButton = {
            if (importProgress == ImportProgressUiState.Idle ||
                importProgress is ImportProgressUiState.Error
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = "取消")
                }
            }
        }
    )
}

// ==================== 评分对话框 ====================

/**
 * 评分对话框
 * 提供星级选择和评论输入功能
 */
@Composable
private fun RatingDialog(
    templateName: String,
    onDismiss: () -> Unit,
    onSubmit: (Float, String) -> Unit
) {
    var selectedRating by remember { mutableFloatStateOf(0f) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "为模板评分") },
        text = {
            Column {
                Text(
                    text = templateName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 星级选择
                Text(
                    text = "评分",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= selectedRating) {
                                Icons.Default.Star
                            } else {
                                Icons.Outlined.StarOutline
                            },
                            contentDescription = "$i 星",
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { selectedRating = i.toFloat() },
                            tint = if (i <= selectedRating) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 评论输入
                Text(
                    text = "评论（可选）",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (it.length <= 500) comment = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text(text = "分享你的使用体验...") },
                    maxLines = 4
                )

                if (comment.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${comment.length}/500",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedRating, comment) },
                enabled = selectedRating > 0
            ) {
                Text(text = "提交")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        }
    )
}

// ==================== 空状态组件 ====================

/**
 * 空市场状态组件
 * 当没有模板时显示的提示界面
 */
@Composable
private fun EmptyMarketplaceState(
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
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无模板",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "分享你的模板到市场，或尝试其他搜索条件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// ==================== 统计信息组件 ====================

/**
 * 统计信息项组件
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==================== 辅助函数 ====================

/**
 * 获取分类的显示名称
 */
private fun getCategoryDisplayName(category: TemplateCategory): String {
    return when (category) {
        TemplateCategory.GENERAL -> "通用"
        TemplateCategory.SOCIAL -> "社交"
        TemplateCategory.WORK -> "工作"
        TemplateCategory.GAMING -> "游戏"
        TemplateCategory.PRIVACY -> "隐私"
        TemplateCategory.PRODUCTIVITY -> "生产力"
    }
}

/**
 * 获取分类的图标（emoji）
 */
private fun getCategoryIcon(category: TemplateCategory): String {
    return when (category) {
        TemplateCategory.GENERAL -> "\uD83D\uDCE6"
        TemplateCategory.SOCIAL -> "\uD83D\uDCAC"
        TemplateCategory.WORK -> "\uD83D\uDCBC"
        TemplateCategory.GAMING -> "\uD83C\uDFAE"
        TemplateCategory.PRIVACY -> "\uD83D\uDD12"
        TemplateCategory.PRODUCTIVITY -> "\u2699\uFE0F"
    }
}

/**
 * 获取分类对应的颜色组合（背景色, 文字色）
 * 使用 Material 3 的颜色方案，支持深色模式
 */
@Composable
private fun getCategoryColors(category: TemplateCategory): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
    val colorScheme = MaterialTheme.colorScheme
    return when (category) {
        TemplateCategory.GENERAL -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
        TemplateCategory.SOCIAL -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        TemplateCategory.WORK -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        TemplateCategory.GAMING -> colorScheme.errorContainer to colorScheme.onErrorContainer
        TemplateCategory.PRIVACY -> colorScheme.surfaceVariant to colorScheme.onSurfaceVariant
        TemplateCategory.PRODUCTIVITY -> colorScheme.inversePrimary to colorScheme.inverseOnSurface
    }
}

/**
 * 格式化时间戳为可读日期
 */
private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "未知"
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
