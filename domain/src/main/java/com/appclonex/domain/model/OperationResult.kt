package com.appclonex.domain.model

import com.github.michaelbull.result.*

/**
 * 操作结果密封类
 * 使用 kotlin-result 库实现函数式错误处理
 */
typealias OperationResult<T> = Result<T, OperationError>

/**
 * 操作错误类型
 */
sealed class OperationError {
    data class ShizukuNotAvailable(val message: String = "Shizuku service not available") : OperationError()
    data class PermissionDenied(val message: String = "Permission denied") : OperationError()
    data class ApplicationNotFound(val packageName: String) : OperationError()
    data class CloneFailed(val reason: String) : OperationError()
    data class FreezeFailed(val reason: String) : OperationError()
    data class SandboxError(val reason: String) : OperationError()
    data class DatabaseError(val reason: String) : OperationError()
    data class NetworkError(val reason: String) : OperationError()
    data class UnknownError(val throwable: Throwable) : OperationError()
}

/**
 * 操作结果扩展函数
 */
object OperationResultExtensions {

    /**
     * 安全执行操作，捕获异常并转换为 Result
     */
    inline fun <T> safeExecute(block: () -> T): OperationResult<T> {
        return runCatching(block)
            .mapError { OperationError.UnknownError(it) }
    }

    /**
     * 安全执行挂起操作，捕获异常并转换为 Result
     */
    suspend inline fun <T> safeExecuteSuspend(crossinline block: suspend () -> T): OperationResult<T> {
        return runCatching { block() }
            .mapError { OperationError.UnknownError(it) }
    }

    /**
     * 检查 Shizuku 是否可用
     */
    inline fun <T> requireShizuku(isAvailable: Boolean, block: () -> T): OperationResult<T> {
        return if (isAvailable) {
            safeExecute(block)
        } else {
            Err(OperationError.ShizukuNotAvailable())
        }
    }

    /**
     * 检查权限
     */
    inline fun <T> requirePermission(hasPermission: Boolean, block: () -> T): OperationResult<T> {
        return if (hasPermission) {
            safeExecute(block)
        } else {
            Err(OperationError.PermissionDenied())
        }
    }
}

/**
 * Result 扩展：获取错误消息
 */
fun <T> OperationResult<T>.getErrorMessage(): String {
    return when (val error = error) {
        is OperationError.ShizukuNotAvailable -> error.message
        is OperationError.PermissionDenied -> error.message
        is OperationError.ApplicationNotFound -> "Application not found: ${error.packageName}"
        is OperationError.CloneFailed -> "Clone failed: ${error.reason}"
        is OperationError.FreezeFailed -> "Freeze failed: ${error.reason}"
        is OperationError.SandboxError -> "Sandbox error: ${error.reason}"
        is OperationError.DatabaseError -> "Database error: ${error.reason}"
        is OperationError.NetworkError -> "Network error: ${error.reason}"
        is OperationError.UnknownError -> "Unknown error: ${error.throwable.message}"
        null -> "Success"
    }
}

/**
 * Result 扩展：是否可重试
 */
fun <T> OperationResult<T>.isRetryable(): Boolean {
    return when (error) {
        is OperationError.NetworkError -> true
        is OperationError.ShizukuNotAvailable -> true
        is OperationError.CloneFailed -> true
        else -> false
    }
}
