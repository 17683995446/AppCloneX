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

package com.appclonex.presentation.charts

import com.appclonex.presentation.screens.statistics.charts.ChartAnimationDefaults
import com.appclonex.presentation.screens.statistics.charts.ChartColors
import com.appclonex.presentation.screens.statistics.charts.ChartConfig
import com.appclonex.presentation.screens.statistics.charts.ChartType
import com.appclonex.presentation.screens.statistics.charts.LegendItem
import com.appclonex.presentation.screens.statistics.charts.RadarChartData
import com.appclonex.presentation.screens.statistics.charts.RadarChartStyle
import com.appclonex.presentation.screens.statistics.charts.ScatterChartData
import com.appclonex.presentation.screens.statistics.charts.ScatterChartStyle
import com.appclonex.presentation.screens.statistics.charts.ScatterPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 图表组件单元测试
 * 测试图表数据类、枚举和配置的正确性
 *
 * 注意：这是 JVM 单元测试，不依赖 Android 框架
 * Composable 函数的渲染测试需要使用 Robolectric 或 Compose UI 测试框架
 */
class ChartTypeTests {

    @Test
    fun `ChartType 枚举包含所有五种图表类型`() {
        val types = ChartType.values()
        assertEquals(5, types.size)
    }

    @Test
    fun `ChartType 枚举包含 BAR 类型`() {
        assertNotNull(ChartType.BAR)
    }

    @Test
    fun `ChartType 枚举包含 PIE 类型`() {
        assertNotNull(ChartType.PIE)
    }

    @Test
    fun `ChartType 枚举包含 LINE 类型`() {
        assertNotNull(ChartType.LINE)
    }

    @Test
    fun `ChartType 枚举包含 RADAR 类型`() {
        assertNotNull(ChartType.RADAR)
    }

    @Test
    fun `ChartType 枚举包含 SCATTER 类型`() {
        assertNotNull(ChartType.SCATTER)
    }

    @Test
    fun `ChartType valueOf 返回正确值`() {
        assertEquals(ChartType.BAR, ChartType.valueOf("BAR"))
        assertEquals(ChartType.SCATTER, ChartType.valueOf("SCATTER"))
    }
}

class ChartColorsTests {

    @Test
    fun `ChartColors 默认调色板包含 10 种颜色`() {
        assertEquals(10, ChartColors.DefaultPalette.size)
    }

    @Test
    fun `ChartColors paletteColor 正常索引返回对应颜色`() {
        val color = ChartColors.paletteColor(0)
        assertEquals(ChartColors.Blue, color)
    }

    @Test
    fun `ChartColors paletteColor 超出索引时循环`() {
        val color = ChartColors.paletteColor(10)
        assertEquals(ChartColors.Blue, color)
    }

    @Test
    fun `ChartColors withAlpha 返回半透明颜色`() {
        val result = ChartColors.withAlpha(ChartColors.Red, 0.5f)
        assertEquals(0.5f, result.alpha, 0.001f)
        assertEquals(ChartColors.Red.red, result.red)
    }

    @Test
    fun `ChartColors withAlpha 默认 alpha 为 0_2`() {
        val result = ChartColors.withAlpha(ChartColors.Green)
        assertEquals(0.2f, result.alpha, 0.001f)
    }
}

class ChartAnimationDefaultsTests {

    @Test
    fun `动画持续时间默认 800ms`() {
        assertEquals(800, ChartAnimationDefaults.durationMillis)
    }

    @Test
    fun `动画延迟默认 100ms`() {
        assertEquals(100, ChartAnimationDefaults.staggerDelayMillis)
    }
}

class RadarChartDataTests {

    @Test
    fun `RadarChartData 默认值正确`() {
        val data = RadarChartData(labels = emptyList(), values = emptyList())
        assertEquals(emptyList<String>(), data.labels)
        assertEquals(emptyList<Float>(), data.values)
        assertEquals(0f, data.maxValue, 0.001f)
        assertEquals("", data.title)
        assertNull(data.color)
    }

    @Test
    fun `RadarChartData 自定义值正确`() {
        val labels = listOf("A", "B", "C")
        val values = listOf(10f, 20f, 30f)
        val data = RadarChartData(
            labels = labels,
            values = values,
            maxValue = 100f,
            title = "测试雷达图",
        )
        assertEquals(labels, data.labels)
        assertEquals(values, data.values)
        assertEquals(100f, data.maxValue, 0.001f)
        assertEquals("测试雷达图", data.title)
    }

    @Test
    fun `RadarChartStyle 默认值正确`() {
        val style = RadarChartStyle()
        assertTrue(style.showGrid)
        assertTrue(style.showValues)
        assertTrue(style.animate)
        assertEquals(2f, style.strokeWidth, 0.001f)
        assertEquals(4f, style.pointRadius, 0.001f)
    }
}

class ScatterChartDataTests {

    @Test
    fun `ScatterChartData 默认值正确`() {
        val data = ScatterChartData(points = emptyList())
        assertEquals(emptyList<ScatterPoint>(), data.points)
        assertEquals("", data.xLabel)
        assertEquals("", data.yLabel)
        assertEquals("", data.title)
    }

    @Test
    fun `ScatterChartData 自定义值正确`() {
        val points = listOf(ScatterPoint(x = 1f, y = 2f))
        val data = ScatterChartData(
            points = points,
            xLabel = "X轴",
            yLabel = "Y轴",
            title = "测试散点图",
        )
        assertEquals(points, data.points)
        assertEquals("X轴", data.xLabel)
        assertEquals("Y轴", data.yLabel)
        assertEquals("测试散点图", data.title)
    }

    @Test
    fun `ScatterPoint 默认值正确`() {
        val point = ScatterPoint(x = 5f, y = 10f)
        assertEquals(5f, point.x, 0.001f)
        assertEquals(10f, point.y, 0.001f)
        assertNull(point.label)
        assertNull(point.color)
        assertEquals(1f, point.size, 0.001f)
    }

    @Test
    fun `ScatterPoint 自定义值正确`() {
        val point = ScatterPoint(
            x = 3.5f,
            y = 7.2f,
            label = "数据A",
            size = 2f,
        )
        assertEquals(3.5f, point.x, 0.001f)
        assertEquals(7.2f, point.y, 0.001f)
        assertEquals("数据A", point.label)
        assertEquals(2f, point.size, 0.001f)
    }

    @Test
    fun `ScatterChartStyle 默认值正确`() {
        val style = ScatterChartStyle()
        assertTrue(style.showGrid)
        assertTrue(style.showLabels)
        assertFalse(style.showTrendLine)
        assertEquals(5f, style.pointRadius, 0.001f)
    }
}

class ChartConfigTests {

    @Test
    fun `ChartConfig 默认值正确`() {
        val config = ChartConfig(type = ChartType.BAR)
        assertEquals(ChartType.BAR, config.type)
        assertEquals("", config.title)
        assertEquals("", config.subtitle)
        assertNull(config.data)
        assertNull(config.style)
        assertTrue(config.showLegend)
        assertTrue(config.showEmptyState)
    }

    @Test
    fun `ChartConfig 自定义值正确`() {
        val config = ChartConfig(
            type = ChartType.SCATTER,
            title = "散点图",
            subtitle = "子标题",
            showLegend = false,
            showEmptyState = false,
        )
        assertEquals(ChartType.SCATTER, config.type)
        assertEquals("散点图", config.title)
        assertEquals("子标题", config.subtitle)
        assertFalse(config.showLegend)
        assertFalse(config.showEmptyState)
    }
}

class LegendItemTests {

    @Test
    fun `LegendItem 创建正确`() {
        val item = LegendItem(label = "系列A", color = ChartColors.Blue)
        assertEquals("系列A", item.label)
        assertEquals(ChartColors.Blue, item.color)
    }

    @Test
    fun `LegendItem 列表创建正确`() {
        val items = listOf(
            LegendItem("A", ChartColors.Red),
            LegendItem("B", ChartColors.Green),
        )
        assertEquals(2, items.size)
        assertEquals("A", items[0].label)
        assertEquals("B", items[1].label)
    }
}
