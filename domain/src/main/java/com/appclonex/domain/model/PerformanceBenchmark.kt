package com.appclonex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BenchmarkResult(
    val testName: String = "",
    val score: Float = 0f,
    val unit: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val details: Map<String, String> = emptyMap(),
    val rating: BenchmarkRating = BenchmarkRating.UNKNOWN
)

@Serializable
enum class BenchmarkRating {
    EXCELLENT,  // 90+
    GOOD,       // 70-89
    AVERAGE,    // 50-69
    POOR,       // 30-49
    CRITICAL    // <30
    UNKNOWN
}

@Serializable
data class BenchmarkSuite(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val results: List<BenchmarkResult> = emptyList(),
    val overallScore: Float = 0f,
    val overallRating: BenchmarkRating = BenchmarkRating.UNKNOWN,
    val deviceModel: String = "",
    val androidVersion: String = "",
    val appVersion: String = ""
)

@Serializable
data class PerformanceMetrics(
    val coldStartMs: Long = 0,
    val warmStartMs: Long = 0,
    val memoryUsageMb: Float = 0f,
    val peakMemoryMb: Float = 0f,
    val dbQueryAvgMs: Float = 0f,
    val uiFrameTimeMs: Float = 0f,
    val apkSizeKb: Long = 0,
    val startupMemoryMb: Float = 0f
)
