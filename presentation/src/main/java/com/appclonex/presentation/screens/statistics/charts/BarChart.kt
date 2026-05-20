package com.appclonex.presentation.screens.statistics.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A reusable bar chart component rendered with Compose Canvas.
 *
 * @param data       List of float values representing bar heights.
 * @param labels     List of string labels displayed below each bar.
 * @param maxValue   The maximum value on the Y-axis; bars are scaled relative to this.
 * @param barColor   The fill color for each bar.
 * @param backgroundColor The background color drawn behind the chart area.
 */
@Composable
fun BarChart(
    data: List<Float>,
    labels: List<String>,
    maxValue: Float,
    barColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) {
        ChartEmptyState(message = "暂无柱状图数据")
        return
    }

    // Determine whether horizontal scrolling is needed.
    val minBarWidth = 48.dp
    val minTotalWidth = minBarWidth * data.size
    val needsScroll = minTotalWidth > 360.dp // approximate screen width threshold

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
        label = "BarChartAnimation",
    )

    val textMeasurer = rememberTextMeasurer()

    val chartModifier = modifier
        .fillMaxWidth()
        .height(200.dp)
        .then(
            if (needsScroll) Modifier.horizontalScroll(rememberScrollState()) else Modifier
        )

    Box(modifier = chartModifier, contentAlignment = Alignment.Center) {
        val totalWidth = if (needsScroll) minTotalWidth else 360.dp

        Canvas(
            modifier = Modifier
                .width(totalWidth)
                .height(200.dp),
        ) {
            // Background
            drawRoundRect(
                color = backgroundColor,
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            )

            val effectiveMaxValue = maxValue.coerceAtLeast(1f)
            val barCount = data.size
            val availableWidth = size.width
            val barSpacing = (availableWidth / barCount).coerceAtLeast(1f)
            val barWidth = (barSpacing * 0.6f).coerceAtLeast(1f)
            val chartTop = 24.dp.toPx()
            val chartBottom = size.height - 32.dp.toPx()
            val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)

            for (i in data.indices) {
                val value = data[i]
                val barHeight = ((value / effectiveMaxValue) * chartHeight * animatedProgress)
                    .coerceAtLeast(0f)
                val barLeft = (i * barSpacing) + (barSpacing - barWidth) / 2f
                val barTop = chartBottom - barHeight

                // Draw bar with rounded top corners
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(
                        x = 4.dp.toPx(),
                        y = 4.dp.toPx(),
                    ),
                )

                // Draw value text above bar
                val valueText = if (value >= 1000) {
                    String.format("%.1fk", value / 1000f)
                } else {
                    String.format("%.0f", value)
                }

                val valueTextLayoutResult = textMeasurer.measure(
                    text = valueText,
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    ),
                )

                drawContext.canvas.nativeCanvas.drawText(
                    textMeasurer = textMeasurer,
                    text = valueText,
                    topLeft = Offset(
                        x = barLeft + barWidth / 2f - valueTextLayoutResult.size.width / 2f,
                        y = (barTop - 14.dp.toPx()).coerceAtLeast(0f),
                    ),
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    ),
                )

                // Draw label below bar
                val label = labels.getOrElse(i) { "" }
                val labelTextLayoutResult = textMeasurer.measure(
                    text = label,
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White.copy(alpha = 0.7f),
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
                        x = barLeft + barWidth / 2f - labelTextLayoutResult.size.width / 2f,
                        y = chartBottom + 6.dp.toPx(),
                    ),
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
            }

            // Draw baseline
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(0f, chartBottom),
                end = Offset(size.width, chartBottom),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
}
