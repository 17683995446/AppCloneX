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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A reusable line chart component rendered with Compose Canvas.
 *
 * Features:
 * - Smooth line path connecting data points.
 * - Semi-transparent fill beneath the line.
 * - Dot markers at each data point.
 * - X-axis labels.
 * - Entry animation.
 *
 * @param data      List of float values plotted along the Y-axis.
 * @param labels    List of string labels displayed along the X-axis.
 * @param lineColor The color of the line stroke and dot markers.
 * @param fillColor The semi-transparent fill color beneath the line.
 */
@Composable
fun LineChart(
    data: List<Float>,
    labels: List<String>,
    lineColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) {
        ChartEmptyState(message = "暂无折线图数据")
        return
    }

    // Animation ---------------------------------------------------------------
    var animationProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(data) {
        animationProgress = 1f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(
            durationMillis = ChartAnimationDefaults.durationMillis,
            delayMillis = ChartAnimationDefaults.staggerDelayMillis,
        ),
        label = "LineChartAnimation",
    )

    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        val maxValue = data.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val chartTop = 16.dp.toPx()
        val chartBottom = size.height - 28.dp.toPx()
        val chartLeft = 8.dp.toPx()
        val chartRight = size.width - 8.dp.toPx()
        val chartWidth = (chartRight - chartLeft).coerceAtLeast(1f)
        val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)

        // Map data index to X coordinate
        fun xForIndex(index: Int): Float {
            return if (data.size <= 1) {
                chartLeft + chartWidth / 2f
            } else {
                chartLeft + (index.toFloat() / (data.size - 1)) * chartWidth
            }
        }

        // Map value to Y coordinate (inverted because canvas Y grows downward)
        fun yForValue(value: Float): Float {
            val animatedValue = value * animatedProgress
            return chartBottom - (animatedValue / maxValue) * chartHeight
        }

        // Build the line path -------------------------------------------------
        val linePath = Path()
        for (i in data.indices) {
            val x = xForIndex(i)
            val y = yForValue(data[i])
            if (i == 0) {
                linePath.moveTo(x, y)
            } else {
                linePath.lineTo(x, y)
            }
        }

        // Build the fill path (close to bottom) --------------------------------
        val fillPath = Path()
        for (i in data.indices) {
            val x = xForIndex(i)
            val y = yForValue(data[i])
            if (i == 0) {
                fillPath.moveTo(x, y)
            } else {
                fillPath.lineTo(x, y)
            }
        }
        // Close the path along the bottom edge
        fillPath.lineTo(xForIndex(data.lastIndex), chartBottom)
        fillPath.lineTo(xForIndex(0), chartBottom)
        fillPath.close()

        // Draw filled area beneath the line ------------------------------------
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    fillColor.copy(alpha = 0.4f * animatedProgress),
                    fillColor.copy(alpha = 0.05f * animatedProgress),
                ),
                startY = chartTop,
                endY = chartBottom,
            ),
        )

        // Draw the line -------------------------------------------------------
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
            ),
        )

        // Draw dots at data points ---------------------------------------------
        for (i in data.indices) {
            val x = xForIndex(i)
            val y = yForValue(data[i])
            val dotRadius = 4.dp.toPx()

            // Outer dot (white border)
            drawCircle(
                color = Color.White,
                radius = dotRadius,
                center = Offset(x, y),
            )
            // Inner dot (line color)
            drawCircle(
                color = lineColor,
                radius = dotRadius * 0.6f,
                center = Offset(x, y),
            )
        }

        // Draw X-axis labels ---------------------------------------------------
        // Only draw labels if there are not too many to avoid overlap.
        val maxVisibleLabels = (chartWidth / 40.dp.toPx()).toInt().coerceAtLeast(1)
        val labelStep = if (data.size > maxVisibleLabels) {
            data.size / maxVisibleLabels
        } else {
            1
        }

        for (i in data.indices) {
            if (i % labelStep != 0 && i != data.lastIndex) continue

            val x = xForIndex(i)
            val label = labels.getOrElse(i) { "" }

            val labelLayoutResult = textMeasurer.measure(
                text = label,
                style = androidx.compose.ui.text.TextStyle(
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )

            drawContext.canvas.nativeCanvas.drawText(
                textMeasurer = textMeasurer,
                text = label,
                topLeft = Offset(
                    x = x - labelLayoutResult.size.width / 2f,
                    y = chartBottom + 6.dp.toPx(),
                ),
                style = androidx.compose.ui.text.TextStyle(
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }

        // Draw baseline --------------------------------------------------------
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = Offset(chartLeft, chartBottom),
            end = Offset(chartRight, chartBottom),
            strokeWidth = 1.dp.toPx(),
        )
    }
}
