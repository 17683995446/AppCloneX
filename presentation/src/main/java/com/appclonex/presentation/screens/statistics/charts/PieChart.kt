package com.appclonex.presentation.screens.statistics.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Represents a single slice of a pie chart.
 *
 * @param label The display name for this slice.
 * @param value The numeric value; used to calculate the proportional arc.
 * @param color The fill color for this slice.
 */
data class PieSlice(
    val label: String,
    val value: Float,
    val color: Color,
)

/**
 * A reusable pie chart component rendered with Compose Canvas.
 *
 * Features:
 * - Animated sweep angles on first composition.
 * - Labels with leader lines pointing to each slice.
 * - Total value displayed in the center.
 *
 * @param data     List of [PieSlice] entries to render.
 * @param modifier Modifier applied to the chart canvas.
 */
@Composable
fun PieChart(
    data: List<PieSlice>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) {
        ChartEmptyState(message = "暂无饼图数据")
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
        label = "PieChartAnimation",
    )

    val total = data.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.size(200.dp)) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
        val radius = (canvasWidth.coerceAtMost(canvasHeight) / 2f - 24.dp.toPx())
            .coerceAtLeast(1f)
        val innerRadius = radius * 0.55f // donut hole

        var startAngle = -90f // start from 12 o'clock

        for (slice in data) {
            val sweepAngle = (slice.value / total) * 360f * animatedProgress

            // Draw the arc slice
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2f, radius * 2f),
            )

            // Draw thin white border between slices
            drawArc(
                color = Color.White.copy(alpha = 0.3f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 1.dp.toPx()),
            )

            // Label with leader line ------------------------------------------
            if (animatedProgress > 0.5f) {
                val midAngleDeg = startAngle + sweepAngle / 2f
                val midAngleRad = Math.toRadians(midAngleDeg.toDouble())

                // Point on the outer edge of the slice
                val edgeX = centerX + (radius * cos(midAngleRad)).toFloat()
                val edgeY = centerY + (radius * sin(midAngleRad)).toFloat()

                // End point of the leader line (slightly outside)
                val leaderLength = 16.dp.toPx()
                val endX = centerX + ((radius + leaderLength) * cos(midAngleRad)).toFloat()
                val endY = centerY + ((radius + leaderLength) * sin(midAngleRad)).toFloat()

                // Horizontal extension for the label
                val isRight = endX >= centerX
                val horizontalExtension = 12.dp.toPx()
                val labelAnchorX = endX + (if (isRight) horizontalExtension else -horizontalExtension)

                // Draw leader line
                drawLine(
                    color = slice.color,
                    start = Offset(edgeX, edgeY),
                    end = Offset(endX, endY),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = slice.color,
                    start = Offset(endX, endY),
                    end = Offset(labelAnchorX, endY),
                    strokeWidth = 1.dp.toPx(),
                )

                // Draw label text
                val labelText = buildString {
                    append(slice.label)
                    append(" ")
                    append(String.format("%.0f%%", (slice.value / total) * 100f))
                }

                val labelLayoutResult = textMeasurer.measure(
                    text = labelText,
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 9.sp,
                        textAlign = if (isRight) TextAlign.Start else TextAlign.End,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )

                val textX = if (isRight) {
                    labelAnchorX + 4.dp.toPx()
                } else {
                    labelAnchorX - labelLayoutResult.size.width - 4.dp.toPx()
                }

                drawContext.canvas.nativeCanvas.drawText(
                    textMeasurer = textMeasurer,
                    text = labelText,
                    topLeft = Offset(textX, endY - labelLayoutResult.size.height / 2f),
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 9.sp,
                        textAlign = if (isRight) TextAlign.Start else TextAlign.End,
                    ),
                )
            }

            startAngle += sweepAngle
        }

        // Center total --------------------------------------------------------
        drawCircle(
            color = Color(0xFF1E1E2E), // dark background matching typical theme
            radius = innerRadius,
            center = Offset(centerX, centerY),
        )

        val totalText = String.format("%.0f", total)
        val totalLayoutResult = textMeasurer.measure(
            text = totalText,
            style = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            ),
        )
        drawContext.canvas.nativeCanvas.drawText(
            textMeasurer = textMeasurer,
            text = totalText,
            topLeft = Offset(
                x = centerX - totalLayoutResult.size.width / 2f,
                y = centerY - totalLayoutResult.size.height / 2f - 4.dp.toPx(),
            ),
            style = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            ),
        )

        val subtitleText = "总计"
        val subtitleLayoutResult = textMeasurer.measure(
            text = subtitleText,
            style = androidx.compose.ui.text.TextStyle(
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            ),
        )
        drawContext.canvas.nativeCanvas.drawText(
            textMeasurer = textMeasurer,
            text = subtitleText,
            topLeft = Offset(
                x = centerX - subtitleLayoutResult.size.width / 2f,
                y = centerY + 4.dp.toPx(),
            ),
            style = androidx.compose.ui.text.TextStyle(
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
