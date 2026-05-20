/*
 * Copyright (c) 2024 AppCloneX. All rights reserved.
 *
 * 性能回归检测界面
 * 提供性能回归自动检测、历史趋势分析、阈值告警管理的可视化界面
 * 使用 Material 3 设计，支持深色模式
 */

package com.appclonex.presentation.screens.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appclonex.domain.model.RegressionSeverity
import com.appclonex.domain.model.RegressionType
import com.appclonex.domain.usecase.monitoring.DetectionProgress
import com.appclonex.domain.usecase.monitoring.ExportFormat
import com.appclonex.presentation.screens.statistics.charts.LineChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// 严重程度颜色工具函数
// ============================================================================

/**
 * 根据回归严重程度返回对应的颜色
 *
 * @param severity 严重程度
 * @return 对应的颜色值
 */
@Composable
private fun severityColor(severity: RegressionSeverity): Color = when (severity) {
    RegressionSeverity.NONE -> Color(0xFF4CAF50)       // 绿色 - 正常
    RegressionSeverity.MINOR -> Color(0xFFFFC107)       // 黄色 - 轻微
    RegressionSeverity.MODERATE -> Color(0xFFFF9800)    // 橙色 - 中等
    RegressionSeverity.SEVERE -> Color(0xFFF44336)      // 红色 - 严重
    RegressionSeverity.CRITICAL -> Color(0xFFB71C1C)    // 深红色 - 关键
}

/**
 * 根据回归严重程度返回对应的中文标签
 *
 * @param severity 严重程度
 * @return 中文标签
 */
private fun severityLabel(severity: RegressionSeverity): String = when (severity) {
    RegressionSeverity.NONE -> "正常"
    RegressionSeverity.MINOR -> "轻微"
    RegressionSeverity.MODERATE -> "中等"
    RegressionSeverity.SEVERE -> "严重"
    RegressionSeverity.CRITICAL -> "关键"
}

// ============================================================================
// 主界面 Composable
// ============================================================================

/**
 * 性能回归检测界面
 *
 * 功能区域：
 * 1. 顶部概览卡片 - 总体评分、变化趋势、上次检测时间
 * 2. 检测进度 - 运行检测时显示进度条
 * 3. 回归项目列表 - 各项指标的回归检测结果
 * 4. 历史趋势图 - 使用 LineChart 显示评分趋势
 * 5. 告警列表 - 未读告警优先，支持标记已读
 * 6. 操作按钮 - 运行检测、导出报告、管理阈值
 *
 * @param onNavigateBack 返回上一页的回调
 * @param viewModel 性能回归检测 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceRegressionScreen(
    onNavigateBack: () -> Unit,
    viewModel: PerformanceRegressionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 显示错误消息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // 显示提示消息
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    // 导出内容对话框
    if (uiState.exportedContent != null) {
        ExportContentDialog(
            content = uiState.exportedContent!!,
            onDismiss = { viewModel.clearExportedContent() }
        )
    }

    // 阈值设置对话框
    if (uiState.showThresholdDialog) {
        ThresholdSettingsDialog(
            currentThresholds = uiState.thresholds,
            onUpdateThreshold = { type, warning, critical ->
                viewModel.updateThreshold(type, warning, critical)
            },
            onDismiss = { viewModel.toggleThresholdDialog(false) }
        )
    }

    // 导出格式选择对话框
    if (uiState.showExportDialog) {
        ExportFormatDialog(
            onExport = { format -> viewModel.exportReport(format) },
            onDismiss = { viewModel.toggleExportDialog(false) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("性能回归检测") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 导出按钮
                    IconButton(
                        onClick = { viewModel.toggleExportDialog(true) },
                        enabled = uiState.latestReport != null && !uiState.isDetecting
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "导出报告"
                        )
                    }
                    // 阈值设置按钮
                    IconButton(
                        onClick = { viewModel.toggleThresholdDialog(true) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "阈值设置"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ==================== 概览卡片 ====================
            item {
                OverviewCard(
                    report = uiState.latestReport,
                    isDetecting = uiState.isDetecting
                )
            }

            // ==================== 检测进度 ====================
            if (uiState.isDetecting && uiState.detectionProgress != null) {
                item {
                    DetectionProgressCard(progress = uiState.detectionProgress!!)
                }
            }

            // ==================== 操作按钮 ====================
            if (!uiState.isDetecting) {
                item {
                    ActionButtons(
                        onRunDetection = { viewModel.runDetection() },
                        onExportReport = { viewModel.toggleExportDialog(true) },
                        onManageThresholds = { viewModel.toggleThresholdDialog(true) },
                        hasReport = uiState.latestReport != null
                    )
                }
            }

            // ==================== 回归项目列表 ====================
            uiState.latestReport?.let { report ->
                if (report.regressions.isNotEmpty()) {
                    item {
                        Text(
                            text = "回归检测结果",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(
                        items = report.regressions,
                        key = { it.type.name }
                    ) { item ->
                        RegressionItemCard(item = item)
                    }
                }
            }

            // ==================== 历史趋势图 ====================
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "历史趋势",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 趋势类型选择器
            item {
                TrendTypeSelector(
                    selectedType = uiState.selectedTrendType,
                    onTypeSelected = { viewModel.selectTrendType(it) }
                )
            }

            // 趋势图表
            item {
                val trend = uiState.trendData[uiState.selectedTrendType]
                TrendChartCard(
                    trend = trend,
                    type = uiState.selectedTrendType
                )
            }

            // ==================== 告警列表 ====================
            val unreadAlerts = uiState.alerts.filter { !it.isRead }
            val readAlerts = uiState.alerts.filter { it.isRead }

            if (uiState.alerts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "告警 (${unreadAlerts.size} 未读)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (unreadAlerts.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "未读告警",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                items(
                    items = uiState.alerts,
                    key = { it.id }
                ) { alert ->
                    AlertCard(
                        alert = alert,
                        onMarkRead = { viewModel.markAlertRead(alert.id) },
                        onDismiss = { viewModel.dismissAlert(alert.id) }
                    )
                }
            }

            // ==================== 空状态 ====================
            if (uiState.latestReport == null && !uiState.isDetecting) {
                item {
                    EmptyStateCard()
                }
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ============================================================================
// 子组件 - 概览卡片
// ============================================================================

/**
 * 概览卡片
 * 显示总体评分、评分变化趋势和上次检测时间
 *
 * @param report 最新的回归报告
 * @param isDetecting 是否正在检测
 */
@Composable
private fun OverviewCard(
    report: RegressionReport?,
    isDetecting: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "总体评分",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 评分显示
                Text(
                    text = if (report != null) {
                        String.format("%.1f", report.overallScore)
                    } else {
                        "--"
                    },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                // 变化趋势箭头
                if (report != null) {
                    TrendIndicator(scoreChange = report.scoreChange)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 上次检测时间
            if (report != null) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                Text(
                    text = "上次检测: ${dateFormat.format(Date(report.timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            } else {
                Text(
                    text = "尚未运行检测",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ============================================================================
// 子组件 - 趋势指示器
// ============================================================================

/**
 * 评分变化趋势指示器
 * 显示箭头和变化数值
 *
 * @param scoreChange 评分变化值
 */
@Composable
private fun TrendIndicator(scoreChange: Float) {
    val (icon, color, label) = when {
        scoreChange > 0.5f -> Triple(
            Icons.Default.TrendingUp,
            Color(0xFF4CAF50),
            "+${String.format("%.1f", scoreChange)}"
        )
        scoreChange < -0.5f -> Triple(
            Icons.Default.TrendingDown,
            Color(0xFFF44336),
            String.format("%.1f", scoreChange)
        )
        else -> Triple(
            Icons.Default.TrendingFlat,
            Color(0xFFFFC107),
            "稳定"
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

// ============================================================================
// 子组件 - 检测进度卡片
// ============================================================================

/**
 * 检测进度卡片
 * 显示当前检测阶段和进度条
 *
 * @param progress 当前检测进度
 */
@Composable
private fun DetectionProgressCard(progress: DetectionProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = when (progress) {
                        is DetectionProgress.Initializing -> "正在初始化检测环境..."
                        is DetectionProgress.RunningBenchmark ->
                            "正在测试: ${progress.type.name} (${(progress.progress * 100).toInt()}%)"
                        is DetectionProgress.AnalyzingResults -> "正在分析测试结果..."
                        is DetectionProgress.GeneratingReport -> "正在生成报告..."
                        else -> "处理中..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // 进度条
            if (progress is DetectionProgress.RunningBenchmark) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ============================================================================
// 子组件 - 操作按钮
// ============================================================================

/**
 * 操作按钮区域
 *
 * @param onRunDetection 运行检测回调
 * @param onExportReport 导出报告回调
 * @param onManageThresholds 管理阈值回调
 * @param hasReport 是否已有报告
 */
@Composable
private fun ActionButtons(
    onRunDetection: () -> Unit,
    onExportReport: () -> Unit,
    onManageThresholds: () -> Unit,
    hasReport: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onRunDetection,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("运行检测")
        }

        OutlinedButton(
            onClick = onExportReport,
            enabled = hasReport,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("导出报告")
        }

        OutlinedButton(
            onClick = onManageThresholds,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("阈值")
        }
    }
}

// ============================================================================
// 子组件 - 回归项卡片
// ============================================================================

/**
 * 回归项卡片
 * 显示单个性能指标的回归检测结果
 *
 * @param item 回归项数据
 */
@Composable
private fun RegressionItemCard(item: com.appclonex.domain.model.RegressionItem) {
    val color = severityColor(item.severity)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.severity == RegressionSeverity.NONE) {
                MaterialTheme.colorScheme.surface
            } else {
                color.copy(alpha = 0.08f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 严重程度颜色指示器
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 指标信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.metricName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 变化百分比和严重程度标签
            Column(horizontalAlignment = Alignment.End) {
                val changeText = if (item.changePercent >= 0) {
                    "+${String.format("%.1f", item.changePercent)}%"
                } else {
                    "${String.format("%.1f", item.changePercent)}%"
                }
                Text(
                    text = changeText,
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                // 严重程度标签
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = severityLabel(item.severity),
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ============================================================================
// 子组件 - 趋势类型选择器
// ============================================================================

/**
 * 趋势类型选择器
 * 使用 FilterChip 让用户选择要查看的回归类型趋势
 *
 * @param selectedType 当前选中的类型
 * @param onTypeSelected 类型选择回调
 */
@Composable
private fun TrendTypeSelector(
    selectedType: RegressionType,
    onTypeSelected: (RegressionType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RegressionType.entries.forEach { type ->
            FilterChip(
                selected = type == selectedType,
                onClick = { onTypeSelected(type) },
                label = {
                    Text(
                        text = type.name,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

// ============================================================================
// 子组件 - 趋势图表卡片
// ============================================================================

/**
 * 趋势图表卡片
 * 使用 LineChart 组件显示评分趋势
 *
 * @param trend 趋势数据
 * @param type 回归类型
 */
@Composable
private fun TrendChartCard(
    trend: com.appclonex.domain.model.RegressionTrend?,
    type: RegressionType
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 趋势方向指示
            if (trend != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${type.name} 趋势",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val directionColor = when (trend.direction) {
                            com.appclonex.domain.model.TrendDirection.IMPROVING -> Color(0xFF4CAF50)
                            com.appclonex.domain.model.TrendDirection.STABLE -> Color(0xFFFFC107)
                            com.appclonex.domain.model.TrendDirection.DEGRADING -> Color(0xFFF44336)
                            com.appclonex.domain.model.TrendDirection.UNKNOWN -> Color.Gray
                        }
                        val directionLabel = when (trend.direction) {
                            com.appclonex.domain.model.TrendDirection.IMPROVING -> "改善中"
                            com.appclonex.domain.model.TrendDirection.STABLE -> "稳定"
                            com.appclonex.domain.model.TrendDirection.DEGRADING -> "下降中"
                            com.appclonex.domain.model.TrendDirection.UNKNOWN -> "数据不足"
                        }
                        Icon(
                            imageVector = when (trend.direction) {
                                com.appclonex.domain.model.TrendDirection.IMPROVING -> Icons.Default.TrendingUp
                                com.appclonex.domain.model.TrendDirection.STABLE -> Icons.Default.TrendingFlat
                                com.appclonex.domain.model.TrendDirection.DEGRADING -> Icons.Default.TrendingDown
                                com.appclonex.domain.model.TrendDirection.UNKNOWN -> Icons.Default.TrendingFlat
                            },
                            contentDescription = directionLabel,
                            tint = directionColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = directionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = directionColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // LineChart
                if (trend.dataPoints.isNotEmpty()) {
                    val chartData = trend.dataPoints.map { it.value }
                    val chartLabels = trend.dataPoints.mapIndexed { index, point ->
                        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                        if (trend.dataPoints.size <= 10 || index % 3 == 0) {
                            dateFormat.format(Date(point.timestamp))
                        } else {
                            ""
                        }
                    }
                    LineChart(
                        data = chartData,
                        labels = chartLabels,
                        lineColor = MaterialTheme.colorScheme.primary,
                        fillColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无趋势数据",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "加载趋势数据中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ============================================================================
// 子组件 - 告警卡片
// ============================================================================

/**
 * 告警卡片
 * 显示单条性能告警信息，支持标记已读和忽略操作
 *
 * @param alert 告警数据
 * @param onMarkRead 标记已读回调
 * @param onDismiss 忽略告警回调
 */
@Composable
private fun AlertCard(
    alert: com.appclonex.domain.model.PerformanceAlert,
    onMarkRead: () -> Unit,
    onDismiss: () -> Unit
) {
    val severityColor = severityColor(alert.severity)
    val backgroundColor = if (alert.isRead) {
        MaterialTheme.colorScheme.surface
    } else {
        severityColor.copy(alpha = 0.08f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 告警图标
            Icon(
                imageVector = if (alert.severity == RegressionSeverity.CRITICAL) {
                    Icons.Default.Error
                } else {
                    Icons.Default.Warning
                },
                contentDescription = null,
                tint = severityColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 告警内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (!alert.isRead) FontWeight.Bold else FontWeight.Normal
                )
                val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                Text(
                    text = dateFormat.format(Date(alert.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 操作按钮
            Column(horizontalAlignment = Alignment.End) {
                if (!alert.isRead) {
                    TextButton(
                        onClick = onMarkRead,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "已读",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "忽略",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// 子组件 - 空状态
// ============================================================================

/**
 * 空状态卡片
 * 首次使用时显示引导信息
 */
@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "欢迎使用性能回归检测",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击\"运行检测\"按钮开始第一次性能基准测试。\n" +
                    "系统将自动检测性能回归并生成详细报告。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================================
// 对话框 - 导出格式选择
// ============================================================================

/**
 * 导出格式选择对话框
 *
 * @param onExport 选择格式后的回调
 * @param onDismiss 取消回调
 */
@Composable
private fun ExportFormatDialog(
    onExport: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择导出格式") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportFormat.entries.forEach { format ->
                    val description = when (format) {
                        ExportFormat.JSON -> "JSON - 适合程序化处理"
                        ExportFormat.CSV -> "CSV - 适合电子表格分析"
                        ExportFormat.MARKDOWN -> "Markdown - 适合文档展示"
                    }
                    Button(
                        onClick = { onExport(format) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(description)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// ============================================================================
// 对话框 - 导出内容展示
// ============================================================================

/**
 * 导出内容展示对话框
 *
 * @param content 导出的文本内容
 * @param onDismiss 关闭回调
 */
@Composable
private fun ExportContentDialog(
    content: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出结果") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                androidx.compose.foundation.horizontalScroll(
                    rememberScrollState()
                ) {
                    androidx.compose.foundation.verticalScroll(
                        rememberScrollState()
                    ) {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        )
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
// 对话框 - 阈值设置
// ============================================================================

/**
 * 阈值设置对话框
 * 为每种回归类型设置警告和关键阈值
 *
 * @param currentThresholds 当前阈值配置
 * @param onUpdateThreshold 更新阈值回调
 * @param onDismiss 关闭回调
 */
@Composable
private fun ThresholdSettingsDialog(
    currentThresholds: Map<RegressionType, com.appclonex.domain.model.RegressionThreshold>,
    onUpdateThreshold: (RegressionType, Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    // 为每种类型维护本地状态
    val localThresholds = remember(currentThresholds) {
        currentThresholds.toMutableMap()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("阈值设置") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(RegressionType.entries) { type ->
                    val threshold = localThresholds[type]
                    ThresholdTypeEditor(
                        type = type,
                        warningThreshold = threshold?.warningThreshold ?: 10f,
                        criticalThreshold = threshold?.criticalThreshold ?: 50f,
                        onWarningChange = { value ->
                            localThresholds[type] = (localThresholds[type] ?: com.appclonex.domain.model.RegressionThreshold(type = type))
                                .copy(warningThreshold = value)
                        },
                        onCriticalChange = { value ->
                            localThresholds[type] = (localThresholds[type] ?: com.appclonex.domain.model.RegressionThreshold(type = type))
                                .copy(criticalThreshold = value)
                        },
                        onSave = {
                            val t = localThresholds[type]
                            if (t != null) {
                                onUpdateThreshold(type, t.warningThreshold, t.criticalThreshold)
                            }
                        }
                    )
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

/**
 * 单个回归类型的阈值编辑器
 *
 * @param type 回归类型
 * @param warningThreshold 当前警告阈值
 * @param criticalThreshold 当前关键阈值
 * @param onWarningChange 警告阈值变更回调
 * @param onCriticalChange 关键阈值变更回调
 * @param onSave 保存回调
 */
@Composable
private fun ThresholdTypeEditor(
    type: RegressionType,
    warningThreshold: Float,
    criticalThreshold: Float,
    onWarningChange: (Float) -> Unit,
    onCriticalChange: (Float) -> Unit,
    onSave: () -> Unit
) {
    var warningText by remember(warningThreshold) { mutableStateOf(warningThreshold.toString()) }
    var criticalText by remember(criticalThreshold) { mutableStateOf(criticalThreshold.toString()) }

    Column {
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = type.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = warningText,
                onValueChange = {
                    warningText = it
                    it.toFloatOrNull()?.let(onWarningChange)
                },
                label = { Text("警告阈值%") },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = criticalText,
                onValueChange = {
                    criticalText = it
                    it.toFloatOrNull()?.let(onCriticalChange)
                },
                label = { Text("关键阈值%") },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存", style = MaterialTheme.typography.labelMedium)
        }
    }
}

// 需要导入的滚动状态
@Composable
private fun rememberScrollState() = androidx.compose.foundation.rememberScrollState()
