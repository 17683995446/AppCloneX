package com.appclonex.presentation.screens.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appclonex.domain.model.StatisticsPeriod
import com.appclonex.domain.model.TopUsedApp
import com.appclonex.domain.model.UsageOverview

/**
 * 使用统计仪表盘界面
 * Usage statistics dashboard screen
 *
 * 提供应用使用概览、最常使用应用排行和使用趋势图表。
 * Provides app usage overview, top used app ranking and usage trend chart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageStatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: UsageStatisticsViewModel = hiltViewModel()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "使用统计") },
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 时间段选择器
                item {
                    PeriodSelector(
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = { viewModel.selectPeriod(it) }
                    )
                }

                // 概览卡片
                item {
                    OverviewCards(overview = uiState.overview)
                }

                // 使用趋势图表
                item {
                    UsageTrendChart(
                        trendData = uiState.overview.usageTrend.dailyLaunches,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 最常使用应用列表标题
                item {
                    Text(
                        text = "最常使用应用",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 最常使用应用列表
                if (uiState.topApps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无使用数据",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = uiState.topApps,
                        key = { _, app -> app.packageName }
                    ) { index, app ->
                        TopUsedAppItem(
                            app = app.copy(rank = index + 1),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * 时间段选择器组件
 * Period selector component using FilterChip
 */
@Composable
private fun PeriodSelector(
    selectedPeriod: StatisticsPeriod,
    onPeriodSelected: (StatisticsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val periods = listOf(
        StatisticsPeriod.TODAY to "今天",
        StatisticsPeriod.THIS_WEEK to "本周",
        StatisticsPeriod.THIS_MONTH to "本月",
        StatisticsPeriod.ALL_TIME to "全部"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periods.forEach { (period, label) ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

/**
 * 概览卡片组件
 * Overview cards showing key statistics
 */
@Composable
private fun OverviewCards(
    overview: UsageOverview,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OverviewStatCard(
                title = "克隆应用",
                value = "${overview.totalClonedApps}",
                icon = Icons.Default.Apps,
                modifier = Modifier.weight(1f)
            )
            OverviewStatCard(
                title = "活跃应用",
                value = "${overview.activeApps}",
                icon = Icons.Default.Devices,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OverviewStatCard(
                title = "今日启动",
                value = "${overview.totalLaunchesToday}",
                icon = Icons.Default.TouchApp,
                modifier = Modifier.weight(1f)
            )
            OverviewStatCard(
                title = "总启动次数",
                value = "${overview.totalLaunchesAllTime}",
                icon = Icons.Default.Bolt,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 单个概览统计卡片
 * Single overview stat card
 */
@Composable
private fun OverviewStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * 使用趋势图表组件
 * Usage trend chart using simple bar chart
 *
 * 使用简单的柱状图展示最近的使用趋势数据。
 * Uses a simple bar chart to display recent usage trend data.
 */
@Composable
private fun UsageTrendChart(
    trendData: List<Int>,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)

    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "使用趋势（最近30天）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (trendData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无趋势数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxValue = (trendData.maxOrNull() ?: 1).coerceAtLeast(1)
                val chartHeight = 120.dp

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    trendData.forEach { value ->
                        val barHeightFraction = if (maxValue > 0) value.toFloat() / maxValue else 0f
                        val barHeightDp = (barHeightFraction * chartHeight.value).dp.coerceAtLeast(2.dp)

                        val barColor = if (value == maxValue) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(top = (chartHeight - barHeightDp))
                                .height(barHeightDp)
                                .clip(barShape)
                                .background(barColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "30天前",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "今天",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 最常使用应用列表项
 * Top used app list item
 */
@Composable
private fun TopUsedAppItem(
    app: TopUsedApp,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 排名
            Box(
                modifier = Modifier
                    .size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${app.rank}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (app.rank) {
                        1 -> MaterialTheme.colorScheme.primary
                        2 -> MaterialTheme.colorScheme.secondary
                        3 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 应用名称
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName.ifEmpty { app.packageName },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatUsageTime(app.usageTimeMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 启动次数
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${app.launchCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "次启动",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 格式化使用时长
 * Format usage time from milliseconds to human-readable string
 */
private fun formatUsageTime(ms: Long): String {
    if (ms <= 0) return "未使用"
    val totalMinutes = ms / (1000 * 60)
    return when {
        totalMinutes < 60 -> "${totalMinutes}分钟"
        totalMinutes < 1440 -> {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (minutes == 0L) "${hours}小时" else "${hours}小时${minutes}分钟"
        }
        else -> {
            val days = totalMinutes / 1440
            val hours = (totalMinutes % 1440) / 60
            if (hours == 0L) "${days}天" else "${days}天${hours}小时"
        }
    }
}
