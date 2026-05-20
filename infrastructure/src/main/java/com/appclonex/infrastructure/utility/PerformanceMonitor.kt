package com.appclonex.infrastructure.utility

import android.os.SystemClock
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 性能监控工具
 *
 * 用于追踪关键操作的执行时间
 */
@Singleton
class PerformanceMonitor @Inject constructor() {

    private val traces = mutableMapOf<String, Long>()

    /**
     * 开始追踪
     *
     * @param tag 追踪标签
     */
    fun startTrace(tag: String) {
        traces[tag] = SystemClock.elapsedRealtime()
    }

    /**
     * 结束追踪并记录
     *
     * @param tag 追踪标签
     * @param thresholdMs 阈值（超过则记录警告）
     */
    fun endTrace(tag: String, thresholdMs: Long = 1000) {
        val startTime = traces.remove(tag) ?: return
        val duration = SystemClock.elapsedRealtime() - startTime

        if (duration > thresholdMs) {
            ApplicationLoggingServiceManager.w(
                "Performance",
                "Slow operation [$tag]: ${duration}ms (threshold: ${thresholdMs}ms)"
            )
        } else {
            ApplicationLoggingServiceManager.d(
                "Performance",
                "Operation [$tag] completed in ${duration}ms"
            )
        }
    }

    /**
     * 执行并追踪代码块
     *
     * @param tag 追踪标签
     * @param thresholdMs 阈值
     * @param block 要执行的代码块
     */
    inline fun <T> trace(tag: String, thresholdMs: Long = 1000, block: () -> T): T {
        startTrace(tag)
        return try {
            block()
        } finally {
            endTrace(tag, thresholdMs)
        }
    }
}
