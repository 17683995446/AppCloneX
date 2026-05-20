package com.appclonex.infrastructure.utility

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * 基础设施层工具类单元测试
 * 
 * 测试覆盖：
 * - PinyinSearchConverter 拼音转换
 * - RetryOperationHelper 重试机制
 */
class InfrastructureUtilityTest {

    // ==================== PinyinSearchConverter 测试 ====================

    @Test
    fun `PinyinSearchConverter should convert Chinese to Pinyin`() {
        val result = PinyinSearchConverter.toPinyin("微信")
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `PinyinSearchConverter should get first letters`() {
        val result = PinyinSearchConverter.getFirstLetters("微信支付")
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `PinyinSearchConverter should match pinyin`() {
        assertTrue(PinyinSearchConverter.match("微信", "wx"))
        assertTrue(PinyinSearchConverter.match("微信", "weixin"))
    }

    @Test
    fun `PinyinSearchConverter should handle empty string`() {
        val result = PinyinSearchConverter.toPinyin("")
        assertEquals("", result)
    }

    @Test
    fun `PinyinSearchConverter should handle English letters`() {
        val result = PinyinSearchConverter.toPinyin("ABC")
        assertTrue(result.contains("ABC", ignoreCase = true))
    }

    @Test
    fun `PinyinSearchConverter should handle mixed content`() {
        val result = PinyinSearchConverter.toPinyin("WeChat微信")
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    // ==================== RetryOperationHelper 测试 ====================

    @Test
    fun `RetryOperationHelper should succeed on first try`(): Unit = runBlocking {
        var attempts = 0

        val result = RetryOperationHelper.retry(
            maxRetries = 3,
            initialDelayMs = 100
        ) {
            attempts++
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(1, attempts)
    }

    @Test
    fun `RetryOperationHelper should retry on failure`(): Unit = runBlocking {
        var attempts = 0

        val result = RetryOperationHelper.retry<String>(
            maxRetries = 3,
            initialDelayMs = 50
        ) {
            attempts++
            if (attempts < 3) {
                throw RuntimeException("Simulated failure")
            }
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(3, attempts)
    }

    @Test
    fun `RetryOperationHelper should fail after max retries`(): Unit = runBlocking {
        var attempts = 0

        val result = RetryOperationHelper.retry<String>(
            maxRetries = 2,
            initialDelayMs = 50
        ) {
            attempts++
            throw RuntimeException("Always fails")
        }

        assertTrue(result.isFailure)
        assertEquals(3, attempts) // 初始 + 2 次重试
    }

    @Test
    fun `RetryOperationHelper with zero retries should fail immediately`(): Unit = runBlocking {
        var attempts = 0

        val result = RetryOperationHelper.retry<String>(
            maxRetries = 0,
            initialDelayMs = 50
        ) {
            attempts++
            throw RuntimeException("Always fails")
        }

        assertTrue(result.isFailure)
        assertEquals(1, attempts)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `PinyinSearchConverter special characters should be handled`() {
        val result = PinyinSearchConverter.toPinyin("!@#$%")
        assertNotNull(result)
    }

    @Test
    fun `PinyinSearchConverter numbers should be preserved`() {
        val result = PinyinSearchConverter.toPinyin("123")
        assertTrue(result.contains("123"))
    }

    @Test
    fun `RetryOperationHelper should handle null result`(): Unit = runBlocking {
        val result = RetryOperationHelper.retry<String?>(
            maxRetries = 1,
            initialDelayMs = 50
        ) {
            null
        }

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
}
