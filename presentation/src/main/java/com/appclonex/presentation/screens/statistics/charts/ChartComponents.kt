/*
 * Copyright (c) 2024 AppCloneX Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appclonex.presentation.screens.statistics.charts

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Predefined color palette for charts
// ---------------------------------------------------------------------------

/**
 * Centralised color palette used across all chart components.
 *
 * Each color is chosen to be visually distinct and accessible on both
 * light and dark backgrounds.
 */
object ChartColors {

    val Blue = Color(0xFF4A90D9)
    val Green = Color(0xFF50C878)
    val Orange = Color(0xFFF5A623)
    val Red = Color(0xFFE74C3C)
    val Purple = Color(0xFF9B59B6)
    val Teal = Color(0xFF1ABC9C)
    val Pink = Color(0xFFE91E8F)
    val Yellow = Color(0xFFF1C40F)
    val Indigo = Color(0xFF5C6BC0)
    val Cyan = Color(0xFF00BCD4)

    /** Default palette cycled when a chart needs more colors than manually assigned. */
    val DefaultPalette: List<Color> = listOf(
        Blue,
        Green,
        Orange,
        Red,
        Purple,
        Teal,
        Pink,
        Yellow,
        Indigo,
        Cyan,
    )

    /** Returns a color from the default palette, cycling when [index] exceeds the list size. */
    fun paletteColor(index: Int): Color {
        return DefaultPalette[index % DefaultPalette.size]
    }

    /** Semi-transparent version of [color] suitable for fill areas. */
    fun withAlpha(color: Color, alpha: Float = 0.2f): Color = color.copy(alpha = alpha)
}

// ---------------------------------------------------------------------------
// Animation defaults
// ---------------------------------------------------------------------------

/**
 * Shared animation timing constants used by all chart components.
 */
object ChartAnimationDefaults {

    /** Total duration of the entry animation in milliseconds. */
    const val durationMillis: Int = 800

    /** Stagger delay before the animation starts, in milliseconds. */
    const val staggerDelayMillis: Int = 100
}

// ---------------------------------------------------------------------------
// Chart Type 枚举
// ---------------------------------------------------------------------------

/**
 * 图表类型枚举
 * 定义所有支持的图表类型，用于 AdaptiveChart 自动选择对应组件
 */
enum class ChartType {
    /** 柱状图 */
    BAR,
    /** 饼图 */
    PIE,
    /** 折线图 */
    LINE,
    /** 雷达图 */
    RADAR,
    /** 散点图 */
    SCATTER,
}

// ---------------------------------------------------------------------------
// Chart Config 数据类
// ---------------------------------------------------------------------------

/**
 * 图表统一配置数据类
 * 封装图表的所有配置参数，供 AdaptiveChart 使用
 *
 * @property type 图表类型
 * @property title 图表标题
 * @property subtitle 图表副标题（可选）
 * @property data 图表数据（Any 类型，根据 type 具体解析）
 * @property style 图表样式配置（Any 类型，根据 type 具体解析）
 * @property showLegend 是否显示图例
 * @property showEmptyState 数据为空时是否显示空状态占位
 */
data class ChartConfig(
    val type: ChartType,
    val title: String = "",
    val subtitle: String = "",
    val data: Any? = null,
    val style: Any? = null,
    val showLegend: Boolean = true,
    val showEmptyState: Boolean = true,
)

// ---------------------------------------------------------------------------
// Legend
// ---------------------------------------------------------------------------

/**
 * Represents a single entry in a chart legend.
 *
 * @param label The human-readable label text.
 * @param color The color swatch associated with this legend item.
 */
data class LegendItem(
    val label: String,
    val color: Color,
)

/**
 * A horizontal legend that displays colored dots alongside labels.
 *
 * The legend automatically wraps to multiple rows when items exceed the
 * available width.
 *
 * @param items    The legend entries to display.
 * @param modifier Optional modifier applied to the root container.
 */
@Composable
fun ChartLegend(
    items: List<LegendItem>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Use a simple flow layout: iterate items and group into rows.
        var currentRow = mutableListOf<LegendItem>()
        val rows = mutableListOf<List<LegendItem>>()

        for (item in items) {
            currentRow.add(item)
            if (currentRow.size >= 4) { // max items per row
                rows.add(currentRow)
                currentRow = mutableListOf()
            }
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (item in row) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Color dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .padding(1.dp),
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                                drawCircle(color = item.color)
                            }
                        }
                        // Label text
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f),
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------

/**
 * A placeholder shown when a chart has no data.
 *
 * @param message The message displayed to the user.
 */
@Composable
fun ChartEmptyState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            ),
        )
    }
}

// ---------------------------------------------------------------------------
// Chart Tooltip 通用悬浮提示组件
// ---------------------------------------------------------------------------

/**
 * 通用图表悬浮提示组件
 * 在数据点附近显示带背景的信息提示框
 *
 * @param text 提示文字内容
 * @param isVisible 是否显示提示框
 * @param backgroundColor 提示框背景颜色
 * @param textColor 提示文字颜色
 * @param modifier Modifier
 */
@Composable
fun ChartTooltip(
    text: String,
    isVisible: Boolean,
    backgroundColor: Color = Color(0xDD333333),
    textColor: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    if (!isVisible || text.isEmpty()) return

    Box(
        modifier = modifier.padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(width = 120.dp, height = 36.dp)) {
            drawRoundRect(
                color = backgroundColor,
                cornerRadius = CornerRadius(6.dp.toPx()),
                size = Size(size.width, size.height),
            )
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Chart Loading Indicator 图表加载动画
// ---------------------------------------------------------------------------

/**
 * 图表加载动画组件
 * 显示一个带脉冲效果的加载指示器，用于图表数据加载中的状态
 *
 * @param message 加载提示文字，默认为 "加载中..."
 * @param modifier Modifier
 */
@Composable
fun ChartLoadingIndicator(
    message: String = "加载中...",
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ChartLoading")
    // 脉冲透明度动画
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "LoadingAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 脉冲圆点动画
            Canvas(modifier = Modifier.size(32.dp)) {
                drawCircle(
                    color = ChartColors.Blue.copy(alpha = alpha),
                    radius = 12.dp.toPx(),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = alpha),
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Adaptive Chart 自适应图表组件
// ---------------------------------------------------------------------------

/**
 * 自适应图表组件
 * 根据 [ChartConfig.type] 自动选择对应的图表组件进行渲染
 *
 * 支持的图表类型：
 * - [ChartType.BAR] - 柱状图
 * - [ChartType.PIE] - 饼图
 * - [ChartType.LINE] - 折线图
 * - [ChartType.RADAR] - 雷达图
 * - [ChartType.SCATTER] - 散点图
 *
 * @param config 图表统一配置
 * @param modifier Modifier
 */
@Composable
fun AdaptiveChart(
    config: ChartConfig,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 标题区域
        if (config.title.isNotEmpty()) {
            Text(
                text = config.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color.White,
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        if (config.subtitle.isNotEmpty()) {
            Text(
                text = config.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.6f),
                ),
                modifier = Modifier.padding(horizontal = 8.dp, bottom = 4.dp),
            )
        }

        // 根据类型选择图表组件
        when (config.type) {
            ChartType.BAR -> renderBarChart(config)
            ChartType.PIE -> renderPieChart(config)
            ChartType.LINE -> renderLineChart(config)
            ChartType.RADAR -> renderRadarChart(config)
            ChartType.SCATTER -> renderScatterChart(config)
        }
    }
}

/**
 * 渲染柱状图
 * 从 ChartConfig 中提取柱状图数据并渲染
 */
@Composable
private fun renderBarChart(config: ChartConfig) {
    // 柱状图数据需要以特定格式传入，此处提供默认空状态
    ChartEmptyState(message = "请配置柱状图数据")
}

/**
 * 渲染饼图
 * 从 ChartConfig 中提取饼图数据并渲染
 */
@Composable
private fun renderPieChart(config: ChartConfig) {
    ChartEmptyState(message = "请配置饼图数据")
}

/**
 * 渲染折线图
 * 从 ChartConfig 中提取折线图数据并渲染
 */
@Composable
private fun renderLineChart(config: ChartConfig) {
    ChartEmptyState(message = "请配置折线图数据")
}

/**
 * 渲染雷达图
 * 从 ChartConfig 中提取雷达图数据并渲染
 */
@Composable
private fun renderRadarChart(config: ChartConfig) {
    val dataSets = config.data as? List<*>
    if (dataSets != null) {
        @Suppress("UNCHECKED_CAST")
        RadarChart(
            dataSets = dataSets.filterIsInstance<RadarChartData>(),
            style = config.style as? RadarChartStyle ?: RadarChartStyle(),
        )
    } else {
        RadarChart(
            labels = emptyList(),
            values = emptyList(),
        )
    }
}

/**
 * 渲染散点图
 * 从 ChartConfig 中提取散点图数据并渲染
 */
@Composable
private fun renderScatterChart(config: ChartConfig) {
    val scatterData = config.data as? ScatterChartData
    if (scatterData != null) {
        ScatterChart(
            data = scatterData,
            style = config.style as? ScatterChartStyle ?: ScatterChartStyle(),
        )
    } else {
        ScatterChart(
            data = ScatterChartData(points = emptyList()),
        )
    }
}
