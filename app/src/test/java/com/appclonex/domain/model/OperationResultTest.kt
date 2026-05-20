package com.appclonex.domain.model

import com.github.michaelbull.result.*
import org.junit.Assert.*
import org.junit.Test

/**
 * OperationResult 单元测试
 * 
 * 测试覆盖：
 * - 成功结果处理
 * - 错误结果处理
 * - 错误消息获取
 * - 可重试判断
 */
class OperationResultTest {

    // ==================== 成功结果测试 ====================

    @Test
    fun `success result should contain correct value`() {
        val result: OperationResult<String> = Ok("test value")

        assertTrue(result.isOk)
        assertEquals("test value", result.value)
    }

    @Test
    fun `success result should have null error`() {
        val result: OperationResult<String> = Ok("test")

        assertNull(result.error)
    }

    @Test
    fun `map on success should transform value`() {
        val result: OperationResult<Int> = Ok(5)
        val mapped = result.map { it * 2 }

        assertTrue(mapped.isOk)
        assertEquals(10, mapped.value)
    }

    @Test
    fun `mapError on success should not change result`() {
        val result: OperationResult<String> = Ok("test")
        val mapped = result.mapError { OperationError.NetworkError("error") }

        assertTrue(mapped.isOk)
        assertEquals("test", mapped.value)
    }

    // ==================== 错误结果测试 ====================

    @Test
    fun `error result should contain correct error`() {
        val error = OperationError.ShizukuNotAvailable()
        val result: OperationResult<String> = Err(error)

        assertTrue(result.isErr)
        assertEquals(error, result.error)
    }

    @Test
    fun `error result should have null value`() {
        val result: OperationResult<String> = Err(OperationError.PermissionDenied())

        assertNull(result.value)
    }

    @Test
    fun `mapError on error should transform error`() {
        val result: OperationResult<String> = Err(OperationError.PermissionDenied())
        val mapped = result.mapError { OperationError.NetworkError("network") }

        assertTrue(mapped.isErr)
        assertTrue(mapped.error is OperationError.NetworkError)
    }

    @Test
    fun `map on error should not change result`() {
        val result: OperationResult<Int> = Err(OperationError.CloneFailed("failed"))
        val mapped = result.map { it * 2 }

        assertTrue(mapped.isErr)
        assertTrue(mapped.error is OperationError.CloneFailed)
    }

    // ==================== 错误消息测试 ====================

    @Test
    fun `ShizukuNotAvailable error message should be correct`() {
        val result: OperationResult<String> = Err(OperationError.ShizukuNotAvailable("Shizuku not running"))

        assertEquals("Shizuku not running", result.getErrorMessage())
    }

    @Test
    fun `PermissionDenied error message should be correct`() {
        val result: OperationResult<String> = Err(OperationError.PermissionDenied("No permission"))

        assertEquals("No permission", result.getErrorMessage())
    }

    @Test
    fun `ApplicationNotFound error message should be correct`() {
        val result: OperationResult<String> = Err(OperationError.ApplicationNotFound("com.example.app"))

        assertEquals("Application not found: com.example.app", result.getErrorMessage())
    }

    @Test
    fun `CloneFailed error message should be correct`() {
        val result: OperationResult<String> = Err(OperationError.CloneFailed("Installation failed"))

        assertEquals("Clone failed: Installation failed", result.getErrorMessage())
    }

    @Test
    fun `UnknownError error message should be correct`() {
        val exception = RuntimeException("Something went wrong")
        val result: OperationResult<String> = Err(OperationError.UnknownError(exception))

        assertEquals("Unknown error: Something went wrong", result.getErrorMessage())
    }

    @Test
    fun `success result error message should be Success`() {
        val result: OperationResult<String> = Ok("test")

        assertEquals("Success", result.getErrorMessage())
    }

    // ==================== 可重试测试 ====================

    @Test
    fun `NetworkError should be retryable`() {
        val result: OperationResult<String> = Err(OperationError.NetworkError("timeout"))

        assertTrue(result.isRetryable())
    }

    @Test
    fun `ShizukuNotAvailable should be retryable`() {
        val result: OperationResult<String> = Err(OperationError.ShizukuNotAvailable())

        assertTrue(result.isRetryable())
    }

    @Test
    fun `CloneFailed should be retryable`() {
        val result: OperationResult<String> = Err(OperationError.CloneFailed("failed"))

        assertTrue(result.isRetryable())
    }

    @Test
    fun `PermissionDenied should not be retryable`() {
        val result: OperationResult<String> = Err(OperationError.PermissionDenied())

        assertFalse(result.isRetryable())
    }

    @Test
    fun `ApplicationNotFound should not be retryable`() {
        val result: OperationResult<String> = Err(OperationError.ApplicationNotFound("com.app"))

        assertFalse(result.isRetryable())
    }

    @Test
    fun `success result should not be retryable`() {
        val result: OperationResult<String> = Ok("test")

        assertFalse(result.isRetryable())
    }

    // ==================== safeExecute 测试 ====================

    @Test
    fun `safeExecute with success should return Ok`() {
        val result = OperationResultExtensions.safeExecute { 42 }

        assertTrue(result.isOk)
        assertEquals(42, result.value)
    }

    @Test
    fun `safeExecute with exception should return Err`() {
        val result = OperationResultExtensions.safeExecute<Int> {
            throw RuntimeException("Test error")
        }

        assertTrue(result.isErr)
        assertTrue(result.error is OperationError.UnknownError)
    }

    // ==================== requireShizuku 测试 ====================

    @Test
    fun `requireShizuku when available should execute block`() {
        val result = OperationResultExtensions.requireShizuku(true) { "success" }

        assertTrue(result.isOk)
        assertEquals("success", result.value)
    }

    @Test
    fun `requireShizuku when not available should return error`() {
        val result = OperationResultExtensions.requireShizuku<String>(false) { "success" }

        assertTrue(result.isErr)
        assertTrue(result.error is OperationError.ShizukuNotAvailable)
    }

    // ==================== requirePermission 测试 ====================

    @Test
    fun `requirePermission when granted should execute block`() {
        val result = OperationResultExtensions.requirePermission(true) { "granted" }

        assertTrue(result.isOk)
        assertEquals("granted", result.value)
    }

    @Test
    fun `requirePermission when denied should return error`() {
        val result = OperationResultExtensions.requirePermission<String>(false) { "granted" }

        assertTrue(result.isErr)
        assertTrue(result.error is OperationError.PermissionDenied)
    }

    // ==================== 组合操作测试 ====================

    @Test
    fun `chained map operations should work`() {
        val result: OperationResult<Int> = Ok(1)
            .map { it + 1 }
            .map { it * 2 }
            .map { it + 3 }

        assertTrue(result.isOk)
        assertEquals(7, result.value)
    }

    @Test
    fun `flatMap should chain operations`() {
        val result: OperationResult<Int> = Ok(5)
            .flatMap { Ok(it * 2) }
            .flatMap { Ok(it + 3) }

        assertTrue(result.isOk)
        assertEquals(13, result.value)
    }

    @Test
    fun `flatMap with error should short circuit`() {
        val result: OperationResult<Int> = Ok(5)
            .flatMap { Err(OperationError.NetworkError("failed")) }
            .flatMap { Ok(it + 3) }

        assertTrue(result.isErr)
        assertTrue(result.error is OperationError.NetworkError)
    }

    // ==================== 恢复操作测试 ====================

    @Test
    fun `getOrElse should return value on success`() {
        val result: OperationResult<String> = Ok("value")

        assertEquals("value", result.getOrElse { "default" })
    }

    @Test
    fun `getOrElse should return default on error`() {
        val result: OperationResult<String> = Err(OperationError.CloneFailed("failed"))

        assertEquals("default", result.getOrElse { "default" })
    }

    @Test
    fun `recover should transform error to success`() {
        val result: OperationResult<String> = Err(OperationError.NetworkError("timeout"))
        val recovered = result.recover { "recovered" }

        assertTrue(recovered.isOk)
        assertEquals("recovered", recovered.value)
    }
}
