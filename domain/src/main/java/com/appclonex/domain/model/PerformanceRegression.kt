/*
 * Copyright (c) 2024 AppCloneX. All rights reserved.
 *
 * 性能回归检测模型定义
 * 定义性能回归相关的数据模型、枚举和序列化结构
 * 用于自动检测性能回归、历史趋势分析和阈值告警
 */

package com.appclonex.domain.model

import kotlinx.serialization.Serializable

// ============================================================================
// 枚举定义 / Enum Definitions
// ============================================================================

/**
 * 回归严重程度
 * 表示性能指标变化的严重级别
 *
 * @property NONE 无回归，变化在可接受范围内（<5%）
 * @property MINOR 轻微回归（5%-10%）
 * @property MODERATE 中等回归（10%-20%）
 * @property SEVERE 严重回归（20%-50%）
 * @property CRITICAL 关键回归（>50%）
 */
@Serializable
enum class RegressionSeverity {
    /** 无回归 */
    NONE,
    /** 轻微回归 */
    MINOR,
    /** 中等回归 */
    MODERATE,
    /** 严重回归 */
    SEVERE,
    /** 关键回归 */
    CRITICAL
}

/**
 * 回归类型
 * 表示被监测的性能指标类别
 *
 * @property COLD_START 冷启动时间
 * @property MEMORY_USAGE 内存使用量
 * @property DATABASE_QUERY 数据库查询性能
 * @property UI_RENDERING UI渲染性能
 * @property BATTERY_USAGE 电池使用
 * @property STORAGE_IO 存储IO性能
 */
@Serializable
enum class RegressionType {
    /** 冷启动时间 */
    COLD_START,
    /** 内存使用量 */
    MEMORY_USAGE,
    /** 数据库查询性能 */
    DATABASE_QUERY,
    /** UI渲染性能 */
    UI_RENDERING,
    /** 电池使用 */
    BATTERY_USAGE,
    /** 存储IO性能 */
    STORAGE_IO
}

/**
 * 趋势方向
 * 表示性能指标的变化趋势
 *
 * @property IMPROVING 性能持续改善
 * @property STABLE 性能保持稳定
 * @property DEGRADING 性能持续下降
 * @property UNKNOWN 趋势未知（数据不足）
 */
@Serializable
enum class TrendDirection {
    /** 性能持续改善 */
    IMPROVING,
    /** 性能保持稳定 */
    STABLE,
    /** 性能持续下降 */
    DEGRADING,
    /** 趋势未知 */
    UNKNOWN
}

// ============================================================================
// 数据类定义 / Data Class Definitions
// ============================================================================

/**
 * 设备信息
 * 记录运行基准测试的设备硬件和系统信息
 *
 * @property model 设备型号
 * @property androidVersion Android版本
 * @property sdkVersion SDK版本号
 * @property totalMemory 总内存（MB）
 * @property cpuCores CPU核心数
 * @property screenDensity 屏幕密度
 */
@Serializable
data class DeviceInfo(
    val model: String = "",
    val androidVersion: String = "",
    val sdkVersion: Int = 0,
    val totalMemory: Long = 0L,
    val cpuCores: Int = 0,
    val screenDensity: Int = 0
)

/**
 * 回归项
 * 表示单个性能指标的回归检测结果
 *
 * @property type 回归类型
 * @property metricName 指标名称
 * @property currentValue 当前值
 * @property previousValue 上次值
 * @property changePercent 变化百分比（正值表示性能下降）
 * @property severity 严重程度
 * @property threshold 阈值
 * @property description 描述信息
 */
@Serializable
data class RegressionItem(
    val type: RegressionType = RegressionType.COLD_START,
    val metricName: String = "",
    val currentValue: Float = 0f,
    val previousValue: Float = 0f,
    val changePercent: Float = 0f,
    val severity: RegressionSeverity = RegressionSeverity.NONE,
    val threshold: Float = 0f,
    val description: String = ""
)

/**
 * 回归报告
 * 一次完整的性能回归检测结果
 *
 * @property id 报告唯一标识
 * @property timestamp 检测时间戳
 * @property versionCode 应用版本号
 * @property versionName 应用版本名称
 * @property regressions 回归项列表
 * @property overallScore 总体评分（0-100）
 * @property previousScore 上次总体评分
 * @property scoreChange 评分变化值
 * @property deviceInfo 设备信息
 * @property recommendations 改善建议列表
 */
@Serializable
data class RegressionReport(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val versionCode: Int = 0,
    val versionName: String = "",
    val regressions: List<RegressionItem> = emptyList(),
    val overallScore: Float = 0f,
    val previousScore: Float = 0f,
    val scoreChange: Float = 0f,
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val recommendations: List<String> = emptyList()
)

/**
 * 回归阈值配置
 * 为每种回归类型定义警告和关键阈值
 *
 * @property type 回归类型
 * @property warningThreshold 警告阈值（变化百分比）
 * @property criticalThreshold 关键阈值（变化百分比）
 * @property maxAcceptableChange 最大可接受变化百分比
 */
@Serializable
data class RegressionThreshold(
    val type: RegressionType = RegressionType.COLD_START,
    val warningThreshold: Float = 10f,
    val criticalThreshold: Float = 50f,
    val maxAcceptableChange: Float = 5f
)

/**
 * 趋势数据点
 * 性能指标在某个时间点的采样值
 *
 * @property timestamp 采样时间戳
 * @property value 采样值
 * @property versionCode 对应的应用版本号
 */
@Serializable
data class TrendDataPoint(
    val timestamp: Long = 0L,
    val value: Float = 0f,
    val versionCode: Int = 0
)

/**
 * 回归趋势
 * 性能指标的历史趋势分析结果
 *
 * @property type 回归类型
 * @property dataPoints 历史数据点列表
 * @property slope 线性回归斜率（正值表示性能下降）
 * @property direction 趋势方向
 * @property prediction 预测下一期值
 */
@Serializable
data class RegressionTrend(
    val type: RegressionType = RegressionType.COLD_START,
    val dataPoints: List<TrendDataPoint> = emptyList(),
    val slope: Float = 0f,
    val direction: TrendDirection = TrendDirection.UNKNOWN,
    val prediction: Float = 0f
)

/**
 * 性能告警
 * 当检测到严重性能回归时自动生成的告警
 *
 * @property id 告警唯一标识
 * @property type 回归类型
 * @property severity 严重程度
 * @property message 告警消息
 * @property timestamp 告警时间戳
 * @property isRead 是否已读
 * @property actionUrl 操作链接（可选）
 */
@Serializable
data class PerformanceAlert(
    val id: String = "",
    val type: RegressionType = RegressionType.COLD_START,
    val severity: RegressionSeverity = RegressionSeverity.NONE,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionUrl: String? = null
)

/**
 * 性能基线
 * 特定设备型号和Android版本下的性能基准值
 *
 * @property deviceModel 设备型号
 * @property androidVersion Android版本
 * @property metrics 各项性能指标基准值映射
 * @property sampleSize 采样数量
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 */
@Serializable
data class PerformanceBaseline(
    val deviceModel: String = "",
    val androidVersion: String = "",
    val metrics: Map<String, Float> = emptyMap(),
    val sampleSize: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
