package com.appclonex.presentation.screens.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.appclonex.R
import kotlinx.coroutines.launch

/**
 * 引导页面屏幕
 *
 * 包含三个引导页面的水平翻页视图，向用户介绍 AppCloneX 的核心功能。
 *
 * @param shizukuConnected Shizuku 服务是否已连接
 * @param onComplete 用户完成引导后的回调
 * @param onSkip 用户跳过引导后的回调
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    shizukuConnected: Boolean = false,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部跳过按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, end = 16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }

        // 水平翻页视图
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                0 -> OnboardingPageOne()
                1 -> OnboardingPageTwo()
                2 -> OnboardingPageThree(
                    shizukuConnected = shizukuConnected,
                    onStartClick = onComplete,
                    onSkipClick = onSkip
                )
            }
        }

        // 页面指示器（圆点）
        PageIndicators(
            pageCount = 3,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 32.dp)
        )
    }
}

/**
 * 页面指示器组件
 */
@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val size by animateFloatAsState(
                targetValue = if (isSelected) 24f else 8f,
                label = "indicator_size"
            )
            val color by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.4f,
                label = "indicator_alpha"
            )

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(size.dp)
                    .clip(CircleShape)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = color)
                    )
            )
        }
    }
}

/**
 * 第一页 - 欢迎页面
 */
@Composable
private fun OnboardingPageOne() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 大图标
        Icon(
            imageVector = Icons.Default.Smartphone,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 标题
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 描述
        Text(
            text = stringResource(R.string.onboarding_welcome_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 插图区域（渐变背景卡片）
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "AppCloneX",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "安全 · 隔离 · 高效",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * 第二页 - 核心功能
 */
@Composable
private fun OnboardingPageTwo() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 大图标
        Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 标题
        Text(
            text = stringResource(R.string.onboarding_features_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 描述
        Text(
            text = stringResource(R.string.onboarding_features_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 功能列表
        FeatureListItem(
            icon = Icons.Default.ContentCopy,
            title = stringResource(R.string.onboarding_feature_clone),
            description = stringResource(R.string.onboarding_feature_clone_desc)
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureListItem(
            icon = Icons.Default.Security,
            title = stringResource(R.string.onboarding_feature_sandbox),
            description = stringResource(R.string.onboarding_feature_sandbox_desc)
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureListItem(
            icon = Icons.Default.AcUnit,
            title = stringResource(R.string.onboarding_feature_freeze),
            description = stringResource(R.string.onboarding_feature_freeze_desc)
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureListItem(
            icon = Icons.Default.BarChart,
            title = stringResource(R.string.onboarding_feature_stats),
            description = stringResource(R.string.onboarding_feature_stats_desc)
        )
    }
}

/**
 * 功能列表项组件
 */
@Composable
private fun FeatureListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 第三页 - 开始使用
 */
@Composable
private fun OnboardingPageThree(
    shizukuConnected: Boolean,
    onStartClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 大图标
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 标题
        Text(
            text = stringResource(R.string.onboarding_ready_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 描述
        Text(
            text = stringResource(R.string.onboarding_ready_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Shizuku 状态指示器
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (shizukuConnected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
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
                    imageVector = if (shizukuConnected) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Security
                    },
                    contentDescription = null,
                    tint = if (shizukuConnected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = stringResource(R.string.shizuku_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (shizukuConnected) {
                            stringResource(R.string.onboarding_shizuku_connected)
                        } else {
                            stringResource(R.string.onboarding_shizuku_disconnected)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (shizukuConnected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 开始使用按钮
        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_start),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 跳过按钮
        TextButton(onClick = onSkipClick) {
            Text(
                text = stringResource(R.string.onboarding_skip),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
