package com.appclonex.system

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appclonex.infrastructure.utility.PinyinSearchConverter
import com.appclonex.infrastructure.utility.RetryOperationHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 端到端系统测试
 * 
 * 测试覆盖：
 * - 完整业务流程
 * - 性能基准
 * - 内存压力测试
 * - 边界条件
 */
@RunWith(AndroidJUnit4::class)
class EndToEndSystemTest {

    // ==================== 拼音搜索系统测试 ====================

    @Test
    fun pinyinSearch_fullWorkflow() {
        val appName = "微信"
        
        val pinyin = PinyinSearchConverter.toPinyin(appName)
        assertNotNull(pinyin)
        
        val initials = PinyinSearchConverter.getFirstLetters(appName)
        assertNotNull(initials)
        
        assertTrue(PinyinSearchConverter.match(appName, "wx"))
        assertTrue(PinyinSearchConverter.match(appName, "weixin"))
    }

    @Test
    fun pinyinSearch_mixedLanguage() {
        val appName = "WeChat微信"
        
        assertTrue(PinyinSearchConverter.match(appName, "wechat"))
        assertTrue(PinyinSearchConverter.match(appName, "wx"))
    }

    // ==================== 重试机制系统测试 ====================

    @Test
    fun retryMechanism_successAfterRetries() = runBlocking {
        var attempts = 0
        
        val result = RetryOperationHelper.retry(
            maxRetries = 3,
            initialDelayMs = 100
        ) {
            attempts++
            if (attempts < 2) {
                throw Exception("Simulated failure")
            }
            "success"
        }
        
        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(2, attempts)
    }

    @Test
    fun retryMechanism_failureAfterMaxRetries() = runBlocking {
        var attempts = 0
        
        val result = RetryOperationHelper.retry<String>(
            maxRetries = 2,
            initialDelayMs = 50
        ) {
            attempts++
            throw Exception("Always fails")
        }
        
        assertTrue(result.isFailure)
        assertEquals(3, attempts)
    }

    // ==================== 性能基准测试 ====================

    @Test
    fun pinyinSearch_performanceBenchmark() {
        val testStrings = listOf(
            "微信", "支付宝", "抖音", "快手", "淘宝",
            "京东", "美团", "饿了么", "滴滴出行", "高德地图"
        )
        
        val startTime = System.currentTimeMillis()
        
        repeat(100) {
            testStrings.forEach { str ->
                PinyinSearchConverter.toPinyin(str)
                PinyinSearchConverter.getFirstLetters(str)
                PinyinSearchConverter.match(str, "wx")
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        
        assertTrue("Performance test took too long: ${duration}ms", duration < 5000)
    }

    // ==================== 边界条件系统测试 ====================

    @Test
    fun pinyinUtils_emptyString() {
        val result = PinyinSearchConverter.toPinyin("")
        assertEquals("", result)
    }

    @Test
    fun pinyinUtils_specialCharacters() {
        val result = PinyinSearchConverter.toPinyin("!@#$%")
        assertNotNull(result)
    }

    // ==================== 内存压力测试 ====================

    @Test
    fun memoryUsage_underLoad() {
        val items = mutableListOf<Pair<String, String>>()
        
        repeat(10000) { index ->
            val pinyin = PinyinSearchConverter.toPinyin("应用$index")
            items.add("应用$index" to pinyin)
        }
        
        assertEquals(10000, items.size)
        
        val randomItem = items[5000]
        assertNotNull(randomItem.first)
        assertNotNull(randomItem.second)
    }

    // ==================== 并发安全测试 ====================

    @Test
    fun concurrentAccess_isSafe() = runBlocking {
        val results = mutableListOf<Result<String>>()
        val threads = (1..5).map { index ->
            Thread {
                val result = RetryOperationHelper.retry(
                    maxRetries = 2,
                    initialDelayMs = 50
                ) {
                    "Thread $index result"
                }
                synchronized(results) {
                    results.add(result)
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(5, results.size)
        results.forEach { assertTrue(it.isSuccess) }
    }
}
