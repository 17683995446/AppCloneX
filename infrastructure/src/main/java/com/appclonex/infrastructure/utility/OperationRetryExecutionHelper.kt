package com.appclonex.infrastructure.utility

import kotlinx.coroutines.delay
import kotlin.math.pow

/**
 * 重试操作帮助类
 * 提供指数退避重试机制
 */
object OperationRetryExecutionHelper {

    /**
     * 执行带重试的操作
     * @param times 最大重试次数
     * @param initialDelay 初始延迟（毫秒）
     * @param maxDelay 最大延迟（毫秒）
     * @param factor 退避因子
     * @param block 要执行的操作
     * @return 操作结果
     */
    suspend fun <T> withRetry(
        times: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelay
        
        repeat(times) { attempt ->
            try {
                return Result.success(block())
            } catch (e: Exception) {
                if (attempt == times - 1) {
                    return Result.failure(e)
                }
                
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        
        return Result.failure(IllegalStateException("Retry failed"))
    }

    /**
     * 执行带重试的操作（带条件判断）
     * @param times 最大重试次数
     * @param initialDelay 初始延迟（毫秒）
     * @param maxDelay 最大延迟（毫秒）
     * @param factor 退避因子
     * @param retryIf 重试条件
     * @param block 要执行的操作
     * @return 操作结果
     */
    suspend fun <T> withRetryIf(
        times: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        retryIf: (Throwable) -> Boolean = { true },
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelay
        
        repeat(times) { attempt ->
            try {
                return Result.success(block())
            } catch (e: Exception) {
                if (attempt == times - 1 || !retryIf(e)) {
                    return Result.failure(e)
                }
                
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        
        return Result.failure(IllegalStateException("Retry failed"))
    }
}

/**
 * 网络错误判断
 */
fun Throwable.isNetworkError(): Boolean {
    return this is java.net.UnknownHostException ||
            this is java.net.SocketTimeoutException ||
            this is java.net.ConnectException
}

/**
 * 权限错误判断
 */
fun Throwable.isPermissionError(): Boolean {
    return this is SecurityException ||
            message?.contains("permission", ignoreCase = true) == true
}
