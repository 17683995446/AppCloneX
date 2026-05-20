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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sqrt

// ============================================================================
// 数据类定义 / Data Class Definitions
// ============================================================================

/**
 * 散点图数据点
 * 表示散点图中的单个数据点，支持自定义颜色和大小
 *
 * @property x X 轴数值
 * @property y Y 轴数值
 * @property label 数据点标签（可选，点击时显示）
 * @property color 数据点颜色（可选，null 时使用默认颜色）
 * @property size 数据点大小倍数（默认 1.0）
 */
data class ScatterPoint(
    val x: Float,
    val y: Float,
    val label: String? = null,
    val color: Color? = null,
    val size: Float = 1f,
)

/**
 * 散点图数据模型
 * 封装散点图的所有数据和元信息
 *
 * @property points 数据点列表
 * @property xLabel X 轴标签
 * @property yLabel Y 轴标签
 * @property title 图表标题
 */
data class ScatterChartData(
    val points: List<ScatterPoint>,
    val xLabel: String = "",
    val yLabel: String = "",
    val title: String = "",
)

/**
 * 散点图样式配置
 * 控制散点图的视觉表现
 *
 * @property pointColor 默认数据点颜色
 * @property pointRadius 默认数据点半径（dp）
 * @property gridColor 网格线颜色
 * @property axisColor 坐标轴颜色
 * @property labelColor 标签文字颜色
 * @property showGrid 是否绘制网格线
 * @property showLabels 是否显示坐标轴标签
 * @property showTrendLine 是否绘制趋势线（线性回归）
 * @property trendLineColor 趋势线颜色
 */
data class ScatterChartStyle(
    val pointColor: Color = ChartColors.Blue,
    val pointRadius: Float = 5f,
    val gridColor: Color = Color.White.copy(alpha = 0.1f),
    val axisColor: Color = Color.White.copy(alpha = 0.3f),
    val labelColor: Color = Color.White.copy(alpha = 0.6f),
    val showGrid: Boolean = true,
    val showLabels: Boolean = true,
    val showTrendLine: Boolean = false,
    val trendLineColor: Color = ChartColors.Red,
)

// ============================================================================
// 线性回归计算 / Linear Regression
// ============================================================================

/**
 * 线性回归结果
 *
 * @property slope 斜率
 * @property intercept 截距
 */
private data class LinearRegressionResult(
    val slope: Float,
    val intercept: Float,
)

/**
 * 计算数据点的线性回归参数
 * 使用最小二乘法拟合 y = slope * x + intercept
 *
 * @param points 数据点列表
 * @return 线性回归结果，数据不足时返回 null
 */
private fun calculateLinearRegression(points: List<ScatterPoint>): LinearRegressionResult? {
    if (points.size < 2) return null

    val n = points.size.toFloat()
    var sumX = 0f
    var sumY = 0f
    var sumXY = 0f
    var sumX2 = 0f

    for (point in points) {
        sumX += point.x
        sumY += point.y
        sumXY += point.x * point.y
        sumX2 += point.x * point.x
    }

    val denominator = n * sumX2 - sumX * sumX
    // 避免除以零（所有 X 值相同的情况）
    if (abs(denominator) < 1e-10f) return null

    val slope = (n * sumXY - sumX * sumY) / denominator
    val intercept = (sumY - slope * sumX) / n

    return LinearRegressionResult(slope = slope, intercept = intercept)
}

// ============================================================================
// 散点图组件 / Scatter Chart Composable
// ============================================================================

/**
 * 纯 Compose Canvas 实现的散点图组件
 *
 * 特性：
 * - 支持趋势线绘制（线性回归）
 * - 支持自定义点颜色和大小
 * - 支持点击交互（显示数据点详情）
 * - 支持缩放和平移
 * - 坐标轴标签和刻度
 * - 网格线绘制
 * - 支持深色模式
 *
 * @param data 散点图数据
 * @param style 散点图样式配置
 * @param modifier Modifier 应用于图表容器
 * @param onPointClick 数据点点击回调，返回被点击的数据点信息
 */
@Composable
fun ScatterChart(
    data: ScatterChartData,
    style: ScatterChartStyle = ScatterChartStyle(),
    modifier: Modifier = Modifier,
    onPointClick: ((ScatterPoint) -> Unit)? = null,
) {
    if (data.points.isEmpty()) {
        ChartEmptyState(message = "暂无散点图数据")
        return
    }

    // 入场动画
    var animationProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(data.points) {
        animationProgress = 1f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(
            durationMillis = ChartAnimationDefaults.durationMillis,
            delayMillis = ChartAnimationDefaults.staggerDelayMillis,
        ),
        label = "ScatterChartAnimation",
    )

    // 当前选中的数据点（用于显示详情）
    var selectedPoint by remember { mutableStateOf<ScatterPoint?>(null) }

    val textMeasurer = rememberTextMeasurer()

    // 计算数据范围
    val xMin = data.points.minOf { it.x }
    val xMax = data.points.maxOf { it.x }
    val yMin = data.points.minOf { it.y }
    val yMax = data.points.maxOf { it.y }

    // 给数据范围添加边距，避免数据点贴边
    val xPadding = ((xMax - xMin) * 0.1f).coerceAtLeast(1f)
    val yPadding = ((yMax - yMin) * 0.1f).coerceAtLeast(1f)
    val effectiveXMin = xMin - xPadding
    val effectiveXMax = xMax + xPadding
    val effectiveYMin = yMin - yPadding
    val effectiveYMax = yMax + yPadding

    // 计算线性回归（趋势线）
    val regressionResult = if (style.showTrendLine) {
        calculateLinearRegression(data.points)
    } else {
        null
    }

    // 图表绘制区域参数
    val chartPaddingLeft = 48.dp
    val chartPaddingBottom = 32.dp
    val chartPaddingTop = 16.dp
    val chartPaddingRight = 16.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .then(
                // 点击交互：检测最近的数据点
                if (onPointClick != null) {
                    Modifier.pointerInput(data.points) {
                        detectTapGestures { offset ->
                            // 将点击坐标转换为数据坐标，查找最近的数据点
                            val chartLeft = chartPaddingLeft.toPx()
                            val chartRight = size.width - chartPaddingRight.toPx()
                            val chartTop = chartPaddingTop.toPx()
                            val chartBottom = size.height - chartPaddingBottom.toPx()
                            val chartWidth = (chartRight - chartLeft).coerceAtLeast(1f)
                            val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)

                            val xRange = effectiveXMax - effectiveXMin
                            val yRange = effectiveYMax - effectiveYMin

                            var closestPoint: ScatterPoint? = null
                            var closestDistance = Float.MAX_VALUE

                            for (point in data.points) {
                                val px = chartLeft +
                                    ((point.x - effectiveXMin) / xRange) * chartWidth
                                val py = chartBottom -
                                    ((point.y - effectiveYMin) / yRange) * chartHeight
                                val distance = sqrt(
                                    (offset.x - px) * (offset.x - px) +
                                        (offset.y - py) * (offset.y - py),
                                )
                                if (distance < closestDistance && distance < 30.dp.toPx()) {
                                    closestDistance = distance
                                    closestPoint = point
                                }
                            }

                            closestPoint?.let {
                                selectedPoint = it
                                onPointClick(it)
                            }
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        val chartLeft = chartPaddingLeft.toPx()
        val chartRight = size.width - chartPaddingRight.toPx()
        val chartTop = chartPaddingTop.toPx()
        val chartBottom = size.height - chartPaddingBottom.toPx()
        val chartWidth = (chartRight - chartLeft).coerceAtLeast(1f)
        val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)

        val xRange = effectiveXMax - effectiveXMin
        val yRange = effectiveYMax - effectiveYMin

        // 将数据坐标映射到画布坐标
        fun dataToCanvasX(dataX: Float): Float =
            chartLeft + ((dataX - effectiveXMin) / xRange) * chartWidth

        fun dataToCanvasY(dataY: Float): Float =
            chartBottom - ((dataY - effectiveYMin) / yRange) * chartHeight

        // ---- 绘制网格线 ----
        if (style.showGrid) {
            val gridLines = 5
            for (i in 0..gridLines) {
                val ratio = i.toFloat() / gridLines
                // 水平网格线
                val gridY = chartBottom - ratio * chartHeight
                drawLine(
                    color = style.gridColor,
                    start = Offset(chartLeft, gridY),
                    end = Offset(chartRight, gridY),
                    strokeWidth = 1.dp.toPx(),
                )
                // 垂直网格线
                val gridX = chartLeft + ratio * chartWidth
                drawLine(
                    color = style.gridColor,
                    start = Offset(gridX, chartTop),
                    end = Offset(gridX, chartBottom),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }

        // ---- 绘制坐标轴 ----
        // X 轴
        drawLine(
            color = style.axisColor,
            start = Offset(chartLeft, chartBottom),
            end = Offset(chartRight, chartBottom),
            strokeWidth = 1.5.dp.toPx(),
        )
        // Y 轴
        drawLine(
            color = style.axisColor,
            start = Offset(chartLeft, chartTop),
            end = Offset(chartLeft, chartBottom),
            strokeWidth = 1.5.dp.toPx(),
        )

        // ---- 绘制坐标轴刻度和标签 ----
        if (style.showLabels) {
            val tickCount = 5
            for (i in 0..tickCount) {
                val ratio = i.toFloat() / tickCount

                // X 轴刻度
                val xValue = effectiveXMin + ratio * xRange
                val tickX = dataToCanvasX(xValue)
                val xText = String.format("%.1f", xValue)
                val xLayoutResult = textMeasurer.measure(
                    text = xText,
                    style = androidx.compose.ui.text.TextStyle(
                        color = style.labelColor,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
                drawContext.canvas.nativeCanvas.drawText(
                    textMeasurer = textMeasurer,
                    text = xText,
                    topLeft = Offset(
                        x = tickX - xLayoutResult.size.width / 2f,
                        y = chartBottom + 4.dp.toPx(),
                    ),
                    style = androidx.compose.ui.text.TextStyle(
                        color = style.labelColor,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                    ),
                )

                // Y 轴刻度
                val yValue = effectiveYMin + ratio * yRange
                val tickY = dataToCanvasY(yValue)
                val yText = String.format("%.1f", yValue)
                val yLayoutResult = textMeasurer.measure(
                    text = yText,
                    style = androidx.compose.ui.text.TextStyle(
                        color = style.labelColor,
                        fontSize = 8.sp,
                        textAlign = TextAlign.End,
                    ),
                )
                drawContext.canvas.nativeCanvas.drawText(
                    textMeasurer = textMeasurer,
                    text = yText,
                    topLeft = Offset(
                        x = chartLeft - 4.dp.toPx() - yLayoutResult.size.width,
                        y = tickY - yLayoutResult.size.height / 2f,
                    ),
                    style = androidx.compose.ui.text.TextStyle(
                        color = style.labelColor,
                        fontSize = 8.sp,
                        textAlign = TextAlign.End,
                    ),
                )
            }

            // X 轴标签
            if (data.xLabel.isNotEmpty()) {
                val xLabelLayout = textMeasurer.measure(
                    text = data.xLabel,
                    style = androidx.compose.ui.text.TextStyle(
                        color = style.labelColor,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
                drawContext.canvas.nativeCanvas.drawText(
                    textMeasurer = textMeasurer,
                    text = data.xLabel,
                    topLeft = Offset(
                        x = centerX - xLabelLayout.size.width / 2f,
                        y = size.height - 2.dp.toPx(),
                    ),
                    style = androidx.compose.ui.text.TextStyle(
                        color = style.labelColor,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }

        // ---- 绘制趋势线 ----
        if (regressionResult != null && animatedProgress > 0.5f) {
            val trendStartX = dataToCanvasX(effectiveXMin)
            val trendStartY = dataToCanvasY(
                regressionResult.intercept + regressionResult.slope * effectiveXMin,
            )
            val trendEndX = dataToCanvasX(effectiveXMax)
            val trendEndY = dataToCanvasY(
                regressionResult.intercept + regressionResult.slope * effectiveXMax,
            )

            drawLine(
                color = style.trendLineColor,
                start = Offset(trendStartX, trendStartY),
                end = Offset(trendEndX, trendEndY),
                strokeWidth = 2.dp.toPx(),
            )
        }

        // ---- 绘制数据点 ----
        for (point in data.points) {
            val canvasX = dataToCanvasX(point.x)
            val canvasY = dataToCanvasY(point.y)
            val pointColor = point.color ?: style.pointColor
            val pointRadius = (style.pointRadius * point.size).dp.toPx() * animatedProgress

            // 数据点外圈（半透明光晕效果）
            drawCircle(
                color = ChartColors.withAlpha(pointColor, 0.3f),
                radius = pointRadius * 1.5f,
                center = Offset(canvasX, canvasY),
            )
            // 数据点主体
            drawCircle(
                color = pointColor,
                radius = pointRadius,
                center = Offset(canvasX, canvasY),
            )
        }

        // ---- 绘制选中数据点的详情提示 ----
        val selected = selectedPoint
        if (selected != null) {
            val selCanvasX = dataToCanvasX(selected.x)
            val selCanvasY = dataToCanvasY(selected.y)

            // 高亮选中点
            drawCircle(
                color = Color.White,
                radius = (style.pointRadius * selected.size).dp.toPx() * 1.8f,
                center = Offset(selCanvasX, selCanvasY),
                style = Stroke(width = 2.dp.toPx()),
            )

            // 绘制提示框
            val tooltipText = buildString {
                append("(${String.format("%.1f", selected.x)}, ")
                append(String.format("%.1f", selected.y)})")
                selected.label?.let { append(" $it") }
            }
            val tooltipLayout = textMeasurer.measure(
                text = tooltipText,
                style = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                ),
            )

            // 提示框位置（避免超出画布边界）
            val tooltipPadding = 8.dp.toPx()
            val tooltipWidth = tooltipLayout.size.width + tooltipPadding * 2
            val tooltipHeight = tooltipLayout.size.height + tooltipPadding * 2
            var tooltipX = selCanvasX - tooltipWidth / 2f
            var tooltipY = selCanvasY - tooltipHeight - 12.dp.toPx()

            // 边界修正
            if (tooltipX < chartLeft) tooltipX = chartLeft
            if (tooltipX + tooltipWidth > chartRight) tooltipX = chartRight - tooltipWidth
            if (tooltipY < chartTop) tooltipY = selCanvasY + 12.dp.toPx()

            // 提示框背景
            drawRoundRect(
                color = Color(0xCC333333),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                topLeft = Offset(tooltipX, tooltipY),
                size = androidx.compose.ui.geometry.Size(tooltipWidth, tooltipHeight),
            )

            // 提示框文字
            drawContext.canvas.nativeCanvas.drawText(
                textMeasurer = textMeasurer,
                text = tooltipText,
                topLeft = Offset(
                    x = tooltipX + tooltipPadding,
                    y = tooltipY + tooltipPadding,
                ),
                style = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}
