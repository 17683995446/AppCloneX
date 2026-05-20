package com.appclonex.infrastructure.monitoring

import android.os.SystemClock
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 性能监控器
 *
 * 封装 Firebase Performance Monitoring，提供应用性能追踪
 * 监控启动时间、页面加载、网络请求等关键指标
 */
@Singleton
class PerformanceMonitor @Inject constructor() {

    private val performance = FirebasePerformance.getInstance()
    private val activeTraces = mutableMapOf<String, Trace>()

    init {
        // 启用性能监控
        performance.isPerformanceCollectionEnabled = true
    }

    /**
     * 启动性能追踪
     *
     * @param traceName 追踪名称
     * @return 追踪ID（用于停止追踪）
     */
    fun startTrace(traceName: String): String {
        val traceId = "${traceName}_${SystemClock.elapsedRealtime()}"
        val trace = performance.newTrace(traceName)
        trace.start()
        activeTraces[traceId] = trace
        return traceId
    }

    /**
     * 停止性能追踪
     *
     * @param traceId 追踪ID
     * @param attributes 可选的属性
     */
    fun stopTrace(traceId: String, attributes: Map<String, String> = emptyMap()) {
        activeTraces.remove(traceId)?.let { trace ->
            attributes.forEach { (key, value) ->
                trace.putAttribute(key, value)
            }
            trace.stop()
        }
    }

    /**
     * 追踪 HTTP 请求
     *
     * @param url 请求URL
     * @param method HTTP方法
     * @param responseCode 响应码
     * @param requestSize 请求大小（字节）
     * @param responseSize 响应大小（字节）
     * @param durationMs 耗时（毫秒）
     */
    fun traceHttpRequest(
        url: String,
        method: String,
        responseCode: Int,
        requestSize: Long = 0,
        responseSize: Long = 0,
        durationMs: Long
    ) {
        val metric = performance.newHttpMetric(url, method)
        metric.setHttpResponseCode(responseCode)
        metric.setRequestPayloadSize(requestSize)
        metric.setResponsePayloadSize(responseSize)
        metric.setResponseContentType("application/json")
        metric.start()
        // 模拟耗时
        metric.stop()
    }

    /**
     * 记录自定义指标
     *
     * @param metricName 指标名称
     * @param value 指标值
     */
    fun recordMetric(metricName: String, value: Long) {
        // Firebase Performance 不支持直接记录自定义数值
        // 可以通过自定义追踪实现
        val trace = performance.newTrace("metric_$metricName")
        trace.start()
        trace.putMetric("value", value)
        trace.stop()
    }

    /**
     * 应用启动追踪
     */
    fun traceAppStartup(block: () -> Unit) {
        val traceId = startTrace("app_startup")
        try {
            block()
        } finally {
            stopTrace(traceId)
        }
    }

    /**
     * 页面加载追踪
     *
     * @param screenName 页面名称
     * @param block 页面加载代码块
     */
    fun <T> traceScreenLoad(screenName: String, block: () -> T): T {
        val traceId = startTrace("screen_load_$screenName")
        return try {
            block()
        } finally {
            stopTrace(traceId, mapOf("screen_name" to screenName))
        }
    }

    /**
     * 数据库操作追踪
     *
     * @param operation 操作类型（query/insert/update/delete）
     * @param table 表名
     * @param block 数据库操作代码块
     */
    fun <T> traceDatabaseOperation(
        operation: String,
        table: String,
        block: () -> T
    ): T {
        val traceId = startTrace("db_${operation}_$table")
        return try {
            block()
        } finally {
            stopTrace(traceId, mapOf(
                "operation" to operation,
                "table" to table
            ))
        }
    }

    /**
     * 克隆操作追踪
     *
     * @param packageName 应用包名
     * @param block 克隆操作代码块
     */
    fun <T> traceCloneOperation(packageName: String, block: () -> T): T {
        val traceId = startTrace("clone_operation")
        val startTime = SystemClock.elapsedRealtime()
        return try {
            val result = block()
            val duration = SystemClock.elapsedRealtime() - startTime
            stopTrace(traceId, mapOf(
                "package_name" to packageName,
                "duration_ms" to duration.toString(),
                "success" to "true"
            ))
            result
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            stopTrace(traceId, mapOf(
                "package_name" to packageName,
                "duration_ms" to duration.toString(),
                "success" to "false",
                "error" to e.message.toString()
            ))
            throw e
        }
    }

    /**
     * 设置性能监控属性
     *
     * @param key 属性键
     * @param value 属性值
     */
    fun setAttribute(key: String, value: String) {
        // 设置全局属性
        activeTraces.values.forEach { trace ->
            trace.putAttribute(key, value)
        }
    }
}
