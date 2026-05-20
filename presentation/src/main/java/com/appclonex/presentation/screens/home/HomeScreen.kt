package com.appclonex.presentation.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.appclonex.R
import com.appclonex.presentation.screen.home.ApplicationHomeScreenViewModel
import com.appclonex.presentation.screen.home.HomeUiState

// ============================================================================
// 首页功能入口项 - 支持分组、角标、颜色标记
// ============================================================================

/**
 * 功能入口分类
 */
private enum class FeatureSection(val titleResId: Int) {
    CORE(R.string.home_section_core),
    ADVANCED(R.string.home_section_advanced),
    TOOLS(R.string.home_section_tools)
}

/**
 * 首页功能入口项
 */
private data class FeatureEntry(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val section: FeatureSection = FeatureSection.CORE,
    val badgeCount: Int = 0,
    val isHighlighted: Boolean = false
)

/**
 * 统计摘要项
 */
private data class StatItem(
    val label: String,
    val value: String,
    val icon: ImageVector? = null
)

// ============================================================================
// 首页屏幕 - 功能导航中心
// ============================================================================

/**
 * 首页屏幕
 *
 * 提供所有功能模块的统一入口，分为核心功能、高级功能和工具三个区域。
 * 支持统计摘要显示、告警角标、版本信息展示。
 *
 * @param onNavigateToApps 导航到克隆应用管理
 * @param onNavigateToSettings 导航到设置
 * @param onNavigateToFreeze 导航到应用冻结
 * @param onNavigateToSandbox 导航到沙盒管理
 * @param onNavigateToDashboard 导航到使用统计仪表盘
 * @param onNavigateToProfile 导航到用户配置文件
 * @param onNavigateToAppLock 导航到应用锁设置
 * @param onNavigateToAutomation 导航到自动化规则
 * @param onNavigateToBackup 导航到备份恢复
 * @param onNavigateToBatchClone 导航到批量克隆向导
 * @param onNavigateToFreezeStrategy 导航到智能冻结策略
 * @param onNavigateToUsageStatistics 导航到使用统计
 * @param onNavigateToTemplateMarketplace 导航到模板市场
 * @param onNavigateToPerformanceRegression 导航到性能回归检测
 * @param viewModel 首页 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onNavigateToApps: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFreeze: () -> Unit = {},
    onNavigateToSandbox: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAppLock: () -> Unit = {},
    onNavigateToAutomation: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToBatchClone: () -> Unit = {},
    onNavigateToFreezeStrategy: () -> Unit = {},
    onNavigateToUsageStatistics: () -> Unit = {},
    onNavigateToTemplateMarketplace: () -> Unit = {},
    onNavigateToPerformanceRegression: () -> Unit = {},
    onNavigateToPluginManager: () -> Unit = {},
    onNavigateToNotificationCenter: () -> Unit = {},
    viewModel: ApplicationHomeScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // 构建功能入口列表，按分组组织
    val coreFeatures = listOf(
        FeatureEntry(
            title = stringResource(R.string.home_feature_clone),
            description = stringResource(R.string.home_feature_clone_desc),
            icon = Icons.Default.ContentCopy,
            onClick = onNavigateToApps,
            section = FeatureSection.CORE
        ),
        FeatureEntry(
            title = stringResource(R.string.home_feature_profile),
            description = stringResource(R.string.home_feature_profile_desc),
            icon = Icons.Default.People,
            onClick = onNavigateToProfile,
            section = FeatureSection.CORE
        ),
        FeatureEntry(
            title = stringResource(R.string.home_feature_freeze),
            description = stringResource(R.string.home_feature_freeze_desc),
            icon = Icons.Default.AcUnit,
            onClick = onNavigateToFreeze,
            section = FeatureSection.CORE
        ),
        FeatureEntry(
            title = stringResource(R.string.home_feature_sandbox),
            description = stringResource(R.string.home_feature_sandbox_desc),
            icon = Icons.Default.Security,
            onClick = onNavigateToSandbox,
            section = FeatureSection.CORE
        )
    )

    val advancedFeatures = listOf(
        FeatureEntry(
            title = stringResource(R.string.home_feature_freeze_strategy),
            description = stringResource(R.string.home_feature_freeze_strategy_desc),
            icon = Icons.Default.SmartToy,
            onClick = onNavigateToFreezeStrategy,
            section = FeatureSection.ADVANCED
        ),
        FeatureEntry(
            title = stringResource(R.string.home_feature_batch_clone),
            description = stringResource(R.string.home_feature_batch_clone_desc),
            icon = Icons.Default.ContentCopy,
            onClick = onNavigateToBatchClone,
            section = FeatureSection.ADVANCED
        ),
        FeatureEntry(
            title = stringResource(R.string.home_feature_template_market),
            description = stringResource(R.string.home_feature_template_market_desc),
            icon = Icons.Default.Storefront,
            onClick = onNavigateToTemplateMarketplace,
            section = FeatureSection.ADVANCED
        )
    )

    val toolFeatures = listOf(
        FeatureEntry(
            title = stringResource(R.string.home_feature_dashboard),
            description = stringResource(R.string.home_feature_dashboard_desc),
            icon = Icons.Default.BarChart,
            onClick = onNavigateToDashboard,
            section = FeatureSection.TOOLS
        ),
        FeatureEntry(
            title = stringResource(R.string.home_feature_usage_stats),
            description = stringResource(R.string.home_feature_usage_stats_desc),
            icon = Icons.Default.Dashboard,
            onClick = onNavigateToUsageStatistics,
            section = FeatureSection.TOOLS
        ),
        FeatureEntry(
            title = stringResource(R.string.home_feature_perf_regression),
            description = stringResource(R.string.home_feature_perf_regression_desc),
            icon = Icons.Default.Speed,
            onClick = onNavigateToPerformanceRegression,
            section = FeatureSection.TOOLS,
            badgeCount = 0 // 动态更新
        ),
        FeatureEntry(
            title = stringResource(R.string.home_feature_backup),
            description = stringResource(R.string.home_feature_backup_desc),
            icon = Icons.Default.CloudSync,
            onClick = onNavigateToBackup,
            section = FeatureSection.TOOLS
        ),
        FeatureEntry(
            title = stringResource(R.string.applock_title),
            description = stringResource(R.string.applock_current_status),
            icon = Icons.Default.Lock,
            onClick = onNavigateToAppLock,
            section = FeatureSection.TOOLS
        ),
        FeatureEntry(
            title = stringResource(R.string.automation_title),
            description = stringResource(R.string.automation_desc),
            icon = Icons.Default.SmartToy,
            onClick = onNavigateToAutomation,
            section = FeatureSection.TOOLS
        ),
        FeatureEntry(
            title = stringResource(R.string.home_feature_settings),
            description = stringResource(R.string.home_feature_settings_desc),
            icon = Icons.Default.Settings,
            onClick = onNavigateToSettings,
            section = FeatureSection.TOOLS
        ),
        FeatureEntry(
            title = stringResource(R.string.home_feature_template_market),
            description = "管理应用模板和插件",
            icon = Icons.Default.Storefront,
            onClick = onNavigateToPluginManager,
            section = FeatureSection.TOOLS
        ),
        FeatureEntry(
            title = "通知中心",
            description = "查看和管理所有通知",
            icon = Icons.Default.Notifications,
            onClick = onNavigateToNotificationCenter,
            section = FeatureSection.TOOLS
        )
    )

    // 统计摘要数据
    val statItems = listOf(
        StatItem(
            label = stringResource(R.string.home_stat_profiles),
            value = "${uiState.totalProfiles}",
            icon = Icons.Default.People
        ),
        StatItem(
            label = stringResource(R.string.home_stat_cloned_apps),
            value = "${uiState.totalClonedApps}",
            icon = Icons.Default.ContentCopy
        ),
        StatItem(
            label = stringResource(R.string.home_stat_strategies),
            value = "0",
            icon = Icons.Default.SmartToy
        ),
        StatItem(
            label = stringResource(R.string.home_stat_templates),
            value = "4",
            icon = Icons.Default.Storefront
        )
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AppCloneX",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.home_version_info, "2.4.0", 24),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ============================================================
                // 欢迎区域
                // ============================================================
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.home_welcome),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.home_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ============================================================
                // 统计摘要卡片 - 4项网格
                // ============================================================
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalArrangement = Arrangement.Center,
                            maxItemsInEachRow = 4
                        ) {
                            statItems.forEach { stat ->
                                StatCardItem(
                                    label = stat.label,
                                    value = stat.value,
                                    icon = stat.icon,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ============================================================
                // 核心功能区
                // ============================================================
                item {
                    SectionHeader(title = stringResource(R.string.home_section_core))
                }

                items(coreFeatures) { entry ->
                    FeatureCard(entry = entry)
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // ============================================================
                // 高级功能区
                // ============================================================
                item {
                    SectionHeader(title = stringResource(R.string.home_section_advanced))
                }

                items(advancedFeatures) { entry ->
                    FeatureCard(entry = entry)
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // ============================================================
                // 工具区
                // ============================================================
                item {
                    SectionHeader(title = stringResource(R.string.home_section_tools))
                }

                items(toolFeatures) { entry ->
                    FeatureCard(entry = entry)
                }

                // ============================================================
                // 告警提示区
                // ============================================================
                item {
                    AlertSection()
                }

                // 底部间距
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ============================================================================
// 私有组件
// ============================================================================

/**
 * 分区标题
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

/**
 * 功能入口卡片
 */
@Composable
private fun FeatureCard(entry: FeatureEntry) {
    val containerColor by animateColorAsState(
        targetValue = if (entry.isHighlighted) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "feature_card_color"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = entry.onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标（带角标）
            BadgedBox(
                badge = {
                    if (entry.badgeCount > 0) {
                        Badge {
                            Text(
                                text = if (entry.badgeCount > 99) "99+" else "${entry.badgeCount}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = entry.title,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 标题和描述
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 统计摘要卡片项
 */
@Composable
private fun StatCardItem(
    label: String,
    value: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

/**
 * 告警提示区域
 */
@Composable
private fun AlertSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.home_no_alerts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
