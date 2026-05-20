/*
 * Copyright (c) 2024 AppCloneX. All rights reserved.
 *
 * 导出性能报告用例
 * 将性能回归报告导出为指定格式的字符串
 */

package com.appclonex.domain.usecase.monitoring

import com.appclonex.domain.model.RegressionReport
import com.appclonex.domain.repository.PerformanceRegressionRepository
import javax.inject.Inject

/**
 * 导出格式枚举
 * 支持的性能报告导出格式
 *
 * @property JSON JSON格式，适合程序化处理
 * @property CSV CSV格式，适合电子表格分析
 * @property MARKDOWN Markdown格式，适合文档展示
 */
enum class ExportFormat {
    /** JSON格式 */
    JSON,
    /** CSV格式 */
    CSV,
    /** Markdown格式 */
    MARKDOWN
}

/**
 * 导出性能报告用例
 *
 * 将性能回归报告转换为指定格式的字符串输出。
 * 支持JSON、CSV和Markdown三种格式。
 *
 * @property regressionRepository 性能回归检测仓库
 */
class ExportPerformanceReportUseCase @Inject constructor(
    private val regressionRepository: PerformanceRegressionRepository
) {

    /**
     * 导出性能回归报告
     *
     * 根据指定的格式将回归报告转换为字符串。
     * - JSON: 完整的结构化数据
     * - CSV: 表格化的指标数据，适合导入Excel
     * - Markdown: 格式化的报告文档，适合阅读
     *
     * @param format 导出格式
     * @param report 要导出的回归报告
     * @return 格式化后的报告字符串，失败时返回异常
     */
    operator fun invoke(
        format: ExportFormat,
        report: RegressionReport
    ): Result<String> {
        return try {
            val content = when (format) {
                ExportFormat.JSON -> exportAsJson(report)
                ExportFormat.CSV -> exportAsCsv(report)
                ExportFormat.MARKDOWN -> exportAsMarkdown(report)
            }
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 导出为JSON格式
     *
     * @param report 回归报告
     * @return JSON字符串
     */
    private fun exportAsJson(report: RegressionReport): String {
        val builder = StringBuilder()
        builder.appendLine("{")
        builder.appendLine("  \"reportId\": \"${report.id}\",")
        builder.appendLine("  \"timestamp\": ${report.timestamp},")
        builder.appendLine("  \"versionCode\": ${report.versionCode},")
        builder.appendLine("  \"versionName\": \"${report.versionName}\",")
        builder.appendLine("  \"overallScore\": ${report.overallScore},")
        builder.appendLine("  \"previousScore\": ${report.previousScore},")
        builder.appendLine("  \"scoreChange\": ${report.scoreChange},")
        builder.appendLine("  \"deviceInfo\": {")
        builder.appendLine("    \"model\": \"${report.deviceInfo.model}\",")
        builder.appendLine("    \"androidVersion\": \"${report.deviceInfo.androidVersion}\",")
        builder.appendLine("    \"sdkVersion\": ${report.deviceInfo.sdkVersion},")
        builder.appendLine("    \"totalMemory\": ${report.deviceInfo.totalMemory},")
        builder.appendLine("    \"cpuCores\": ${report.deviceInfo.cpuCores}")
        builder.appendLine("  },")
        builder.appendLine("  \"regressions\": [")
        report.regressions.forEachIndexed { index, item ->
            builder.appendLine("    {")
            builder.appendLine("      \"type\": \"${item.type}\",")
            builder.appendLine("      \"metricName\": \"${item.metricName}\",")
            builder.appendLine("      \"currentValue\": ${item.currentValue},")
            builder.appendLine("      \"previousValue\": ${item.previousValue},")
            builder.appendLine("      \"changePercent\": ${item.changePercent},")
            builder.appendLine("      \"severity\": \"${item.severity}\"")
            builder.append("    }")
            if (index < report.regressions.lastIndex) builder.appendLine(",")
            else builder.appendLine()
        }
        builder.appendLine("  ],")
        builder.appendLine("  \"recommendations\": [")
        report.recommendations.forEachIndexed { index, rec ->
            builder.append("    \"$rec\"")
            if (index < report.recommendations.lastIndex) builder.appendLine(",")
            else builder.appendLine()
        }
        builder.appendLine("  ]")
        builder.appendLine("}")
        return builder.toString()
    }

    /**
     * 导出为CSV格式
     *
     * @param report 回归报告
     * @return CSV字符串
     */
    private fun exportAsCsv(report: RegressionReport): String {
        val builder = StringBuilder()
        // CSV表头
        builder.appendLine("类型,指标名称,当前值,上次值,变化百分比,严重程度,描述")
        // 数据行
        report.regressions.forEach { item ->
            builder.appendLine(
                "${item.type},${item.metricName},${item.currentValue}," +
                "${item.previousValue},${item.changePercent}%,${item.severity}," +
                "\"${item.description}\""
            )
        }
        // 摘要信息
        builder.appendLine()
        builder.appendLine("总体评分,${report.overallScore}")
        builder.appendLine("上次评分,${report.previousScore}")
        builder.appendLine("评分变化,${report.scoreChange}")
        builder.appendLine("设备型号,${report.deviceInfo.model}")
        builder.appendLine("Android版本,${report.deviceInfo.androidVersion}")
        builder.appendLine("检测时间,${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(report.timestamp))}")
        return builder.toString()
    }

    /**
     * 导出为Markdown格式
     *
     * @param report 回归报告
     * @return Markdown字符串
     */
    private fun exportAsMarkdown(report: RegressionReport): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val builder = StringBuilder()
        builder.appendLine("# 性能回归检测报告")
        builder.appendLine()
        builder.appendLine("## 概览")
        builder.appendLine()
        builder.appendLine("| 项目 | 值 |")
        builder.appendLine("|------|-----|")
        builder.appendLine("| 检测时间 | ${dateFormat.format(java.util.Date(report.timestamp))} |")
        builder.appendLine("| 应用版本 | ${report.versionName} (${report.versionCode}) |")
        builder.appendLine("| 总体评分 | ${report.overallScore} |")
        builder.appendLine("| 上次评分 | ${report.previousScore} |")
        builder.appendLine("| 评分变化 | ${report.scoreChange} |")
        builder.appendLine("| 设备型号 | ${report.deviceInfo.model} |")
        builder.appendLine("| Android版本 | ${report.deviceInfo.androidVersion} |")
        builder.appendLine()
        builder.appendLine("## 回归检测结果")
        builder.appendLine()
        builder.appendLine("| 类型 | 指标 | 当前值 | 上次值 | 变化 | 严重程度 |")
        builder.appendLine("|------|------|--------|--------|------|----------|")
        report.regressions.forEach { item ->
            val changeDisplay = if (item.changePercent >= 0) "+${item.changePercent}%" else "${item.changePercent}%"
            builder.appendLine(
                "| ${item.type} | ${item.metricName} | ${item.currentValue} | " +
                "${item.previousValue} | ${changeDisplay} | ${item.severity} |"
            )
        }
        if (report.recommendations.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("## 改善建议")
            builder.appendLine()
            report.recommendations.forEach { rec ->
                builder.appendLine("- $rec")
            }
        }
        return builder.toString()
    }
}
