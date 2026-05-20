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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// ============================================================================
// 数据类定义 / Data Class Definitions
// ============================================================================

/**
 * 雷达图数据模型
 * 支持多组数据叠加显示，每组数据使用不同颜色区分
 *
 * @property labels 维度标签列表，每个标签对应雷达图的一个轴
 * @property values 各维度的数值列表，长度必须与 labels 一致
 * @property maxValue 自定义最大值，用于归一化数据；0 表示自动取 values 中的最大值
 * @property title 图表标题
 * @property color 当前数据组的颜色，null 时使用默认调色板
 */
data class RadarChartData(
    val labels: List<String>,
    val values: List<Float>,
    val maxValue: Float = 0f,
    val title: String = "",
    val color: Color? = null,
)

/**
 * 雷达图样式配置
 * 控制雷达图的视觉表现，包括颜色、线宽、标签等
 *
 * @property strokeColor 数据区域边框颜色
 * @property fillColor 数据区域填充颜色（半透明）
 * @property strokeWidth 边框线宽（dp）
 * @property labelColor 标签文字颜色
 * @property gridColor 网格线颜色
 * @property pointColor 数据点颜色
 * @property pointRadius 数据点半径（dp）
 * @property labelFontSize 标签字体大小（sp）
 * @property showValues 是否在数据点旁显示数值
 * @property showGrid 是否绘制网格线（同心多边形）
 * @property animate 是否启用入场动画
 */
data class RadarChartStyle(
    val strokeColor: Color = ChartColors.Blue,
    val fillColor: Color = ChartColors.Blue,
    val strokeWidth: Float = 2f,
    val labelColor: Color = Color.White.copy(alpha = 0.7f),
    val gridColor: Color = Color.White.copy(alpha = 0.15f),
    val pointColor: Color = Color.White,
    val pointRadius: Float = 4f,
    val labelFontSize: Float = 10f,
    val showValues: Boolean = true,
    val showGrid: Boolean = true,
    val animate: Boolean = true,
)

// ============================================================================
// 雷达图组件 / Radar Chart Composable
// ============================================================================

/**
 * 纯 Compose Canvas 实现的雷达图组件
 *
 * 特性：
 * - 支持多组数据叠加（不同颜色区分）
 * - 支持自定义最大值
 * - 支持动画绘制
 * - 支持标签显示和数值显示
 * - 网格线绘制（同心多边形）
 * - 数据点绘制（圆形标记）
 * - 填充区域半透明
 * - 支持深色模式
 *
 * @param dataSets 多组雷达图数据列表
 * @param style 雷达图样式配置
 * @param modifier Modifier 应用于图表容器
 */
@Composable
fun RadarChart(
    dataSets: List<RadarChartData>,
    style: RadarChartStyle = RadarChartStyle(),
    modifier: Modifier = Modifier,
) {
    // 数据校验：至少需要一组数据且维度 >= 3
    if (dataSets.isEmpty() || dataSets.firstOrNull()?.labels?.isEmpty() == true) {
        ChartEmptyState(message = "暂无雷达图数据")
        return
    }

    // 入场动画
    var animationProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(dataSets) {
        animationProgress = 1f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(
            durationMillis = if (style.animate) ChartAnimationDefaults.durationMillis else 0,
            delayMillis = ChartAnimationDefaults.staggerDelayMillis,
        ),
        label = "RadarChartAnimation",
    )

    val textMeasurer = rememberTextMeasurer()
    val labels = dataSets.first().labels
    val axisCount = labels.size

    // 计算全局最大值（考虑所有数据组）
    val effectiveMaxValue = if (dataSets.any { it.maxValue > 0f }) {
        dataSets.filter { it.maxValue > 0f }.maxOf { it.maxValue }
    } else {
        dataSets.maxOf { dataSet -> dataSet.values.maxOrNull() ?: 0f }
    }.coerceAtLeast(1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        // 雷达图半径，留出标签空间
        val maxRadius = (size.width.coerceAtMost(size.height) / 2f - 40.dp.toPx())
            .coerceAtLeast(1f)
        val angleStep = (2f * Math.PI / axisCount).toFloat()

        // 将角度和半径转换为画布坐标
        fun polarToCartesian(angle: Float, radius: Float): Offset {
            // 从顶部（-90度）开始顺时针绘制
            val adjustedAngle = angle - (Math.PI / 2f).toFloat()
            return Offset(
                x = centerX + radius * cos(adjustedAngle),
                y = centerY + radius * sin(adjustedAngle),
            )
        }

        // ---- 绘制网格线（同心多边形） ----
        if (style.showGrid) {
            val gridLevels = 5
            for (level in 1..gridLevels) {
                val gridRadius = (maxRadius * level / gridLevels) * animatedProgress
                val gridPath = Path()
                for (i in 0 until axisCount) {
                    val angle = i * angleStep
                    val point = polarToCartesian(angle, gridRadius)
                    if (i == 0) gridPath.moveTo(point.x, point.y)
                    else gridPath.lineTo(point.x, point.y)
                }
                gridPath.close()
                drawPath(
                    path = gridPath,
                    color = style.gridColor,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }

        // ---- 绘制轴线 ----
        for (i in 0 until axisCount) {
            val angle = i * angleStep
            val endPoint = polarToCartesian(angle, maxRadius * animatedProgress)
            drawLine(
                color = style.gridColor,
                start = Offset(centerX, centerY),
                end = endPoint,
                strokeWidth = 1.dp.toPx(),
            )
        }

        // ---- 绘制数据区域（支持多组叠加） ----
        dataSets.forEachIndexed { dataSetIndex, dataSet ->
            val dataColor = dataSet.color
                ?: ChartColors.paletteColor(dataSetIndex)
            val strokeColor = if (dataSetIndex == 0) style.strokeColor else dataColor
            val fillColor = if (dataSetIndex == 0) style.fillColor else dataColor

            // 构建数据多边形路径
            val dataPath = Path()
            for (i in 0 until axisCount) {
                val angle = i * angleStep
                val value = dataSet.values.getOrElse(i) { 0f }
                val ratio = (value / effectiveMaxValue).coerceIn(0f, 1f)
                val radius = maxRadius * ratio * animatedProgress
                val point = polarToCartesian(angle, radius)
                if (i == 0) dataPath.moveTo(point.x, point.y)
                else dataPath.lineTo(point.x, point.y)
            }
            dataPath.close()

            // 绘制半透明填充区域
            drawPath(
                path = dataPath,
                color = ChartColors.withAlpha(fillColor, 0.2f),
            )

            // 绘制边框
            drawPath(
                path = dataPath,
                color = strokeColor,
                style = Stroke(width = style.strokeWidth.dp.toPx()),
            )

            // 绘制数据点（圆形标记）
            for (i in 0 until axisCount) {
                val angle = i * angleStep
                val value = dataSet.values.getOrElse(i) { 0f }
                val ratio = (value / effectiveMaxValue).coerceIn(0f, 1f)
                val radius = maxRadius * ratio * animatedProgress
                val point = polarToCartesian(angle, radius)

                // 外圈白色边框
                drawCircle(
                    color = style.pointColor,
                    radius = style.pointRadius.dp.toPx(),
                    center = point,
                )
                // 内圈数据颜色
                drawCircle(
                    color = strokeColor,
                    radius = (style.pointRadius * 0.6f).dp.toPx(),
                    center = point,
                )
            }

            // ---- 显示数值 ----
            if (style.showValues && animatedProgress > 0.8f) {
                for (i in 0 until axisCount) {
                    val angle = i * angleStep
                    val value = dataSet.values.getOrElse(i) { 0f }
                    val ratio = (value / effectiveMaxValue).coerceIn(0f, 1f)
                    val radius = maxRadius * ratio * animatedProgress
                    val point = polarToCartesian(angle, radius)

                    val valueText = String.format("%.1f", value)
                    val valueLayoutResult = textMeasurer.measure(
                        text = valueText,
                        style = androidx.compose.ui.text.TextStyle(
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                        ),
                    )

                    // 数值显示在数据点外侧
                    val labelRadius = radius + 14.dp.toPx()
                    val labelPoint = polarToCartesian(angle, labelRadius)

                    drawContext.canvas.nativeCanvas.drawText(
                        textMeasurer = textMeasurer,
                        text = valueText,
                        topLeft = Offset(
                            x = labelPoint.x - valueLayoutResult.size.width / 2f,
                            y = labelPoint.y - valueLayoutResult.size.height / 2f,
                        ),
                        style = androidx.compose.ui.text.TextStyle(
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
        }

        // ---- 绘制维度标签 ----
        for (i in 0 until axisCount) {
            val angle = i * angleStep
            val labelRadius = maxRadius + 24.dp.toPx()
            val labelPoint = polarToCartesian(angle, labelRadius)
            val label = labels[i]

            val labelLayoutResult = textMeasurer.measure(
                text = label,
                style = androidx.compose.ui.text.TextStyle(
                    color = style.labelColor,
                    fontSize = style.labelFontSize.sp,
                    textAlign = TextAlign.Center,
                ),
            )

            // 根据标签位置调整对齐方式
            val textX = when {
                labelPoint.x < centerX -> labelPoint.x - labelLayoutResult.size.width
                labelPoint.x > centerX -> labelPoint.x
                else -> labelPoint.x - labelLayoutResult.size.width / 2f
            }
            val textY = when {
                labelPoint.y < centerY -> labelPoint.y - labelLayoutResult.size.height
                labelPoint.y > centerY -> labelPoint.y
                else -> labelPoint.y - labelLayoutResult.size.height / 2f
            }

            drawContext.canvas.nativeCanvas.drawText(
                textMeasurer = textMeasurer,
                text = label,
                topLeft = Offset(textX, textY),
                style = androidx.compose.ui.text.TextStyle(
                    color = style.labelColor,
                    fontSize = style.labelFontSize.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

/**
 * 单组数据雷达图便捷方法
 *
 * @param labels 维度标签列表
 * @param values 各维度数值列表
 * @param maxValue 自定义最大值
 * @param title 图表标题
 * @param style 雷达图样式配置
 * @param modifier Modifier
 */
@Composable
fun RadarChart(
    labels: List<String>,
    values: List<Float>,
    maxValue: Float = 0f,
    title: String = "",
    style: RadarChartStyle = RadarChartStyle(),
    modifier: Modifier = Modifier,
) {
    RadarChart(
        dataSets = listOf(
            RadarChartData(
                labels = labels,
                values = values,
                maxValue = maxValue,
                title = title,
            ),
        ),
        style = style,
        modifier = modifier,
    )
}
