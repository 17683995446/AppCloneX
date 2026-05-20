package com.appclonex

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appclonex.core.sandbox.SandboxInfo
import com.appclonex.core.sandbox.SandboxStorageDetail
import com.appclonex.utils.PinyinUtils
import com.appclonex.utils.RetryHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 端到端系统测试
 * 
 * 测试完整业务流程
 */
@RunWith(AndroidJUnit4::class)
class EndToEndSystemTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    // ==================== 拼音搜索系统测试 ====================

    @Test
    fun pinyinSearch_fullWorkflow() {
        val appName = "微信"
        
        // 测试拼音转换
        val pinyin = PinyinUtils.toPinyin(appName)
        assertNotNull(pinyin)
        
        // 测试首字母
        val initials = PinyinUtils.getFirstLetters(appName)
        assertNotNull(initials)
        
        // 测试搜索匹配
        assertTrue(PinyinUtils.match(appName, "wx"))
        assertTrue(PinyinUtils.match(appName, "weixin"))
    }

    @Test
    fun pinyinSearch_mixedLanguage() {
        val appName = "WeChat微信"
        
        // 应该能匹配英文和拼音
        assertTrue(PinyinUtils.match(appName, "wechat"))
        assertTrue(PinyinUtils.match(appName, "wx"))
    }

    // ==================== 重试机制系统测试 ====================

    @Test
    fun retryMechanism_successAfterRetries() = runBlocking {
        var attempts = 0
        
        val result = RetryHelper.retry(
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
        
        val result = RetryHelper.retry<String>(
            maxRetries = 2,
            initialDelayMs = 50
        ) {
            attempts++
            throw Exception("Always fails")
        }
        
        assertTrue(result.isFailure)
        assertEquals(3, attempts) // 初始 + 2 次重试
    }

    // ==================== 数据模型系统测试 ====================

    @Test
    fun sandboxInfo_fullWorkflow() {
        // 创建沙盒信息
        val info = SandboxInfo(
            userId = 10,
            sizeBytes = 10 * 1024 * 1024,
            createdTime = System.currentTimeMillis(),
            appCount = 5,
            sharedPrefsSize = 1024,
            filesSize = 5 * 1024 * 1024,
            cacheSize = 2 * 1024 * 1024,
            databaseSize = 3 * 1024 * 1024
        )
        
        // 验证数据完整性
        assertEquals(10, info.userId)
        assertEquals(5, info.appCount)
        
        // 验证存储计算
        val expectedTotal = 1024 + 5 * 1024 * 1024 + 2 * 1024 * 1024 + 3 * 1024 * 1024
        assertEquals(expectedTotal, info.sharedPrefsSize + info.filesSize + info.cacheSize + info.databaseSize)
    }

    @Test
    fun sandboxStorageDetail_calculationCorrect() {
        val detail = SandboxStorageDetail(
            sharedPrefsSize = 1024,
            filesSize = 2048,
            cacheSize = 512,
            databaseSize = 4096,
            totalSize = 7680
        )
        
        // 验证总计计算正确
        val calculatedTotal = detail.sharedPrefsSize + detail.filesSize + 
                              detail.cacheSize + detail.databaseSize
        assertEquals(detail.totalSize, calculatedTotal)
    }

    // ==================== 边界条件系统测试 ====================

    @Test
    fun pinyinUtils_emptyString() {
        val result = PinyinUtils.toPinyin("")
        assertEquals("", result)
    }

    @Test
    fun pinyinUtils_specialCharacters() {
        val result = PinyinUtils.toPinyin("!@#$%")
        // 特殊字符应该被保留或过滤
        assertNotNull(result)
    }

    @Test
    fun sandboxInfo_zeroValues() {
        val info = SandboxInfo(
            userId = 0,
            sizeBytes = 0,
            createdTime = 0,
            appCount = 0
        )
        
        assertEquals(0, info.userId)
        assertEquals(0, info.sizeBytes)
        assertEquals(0, info.appCount)
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
                PinyinUtils.toPinyin(str)
                PinyinUtils.getFirstLetters(str)
                PinyinUtils.match(str, "wx")
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        
        // 1000 次操作应该在合理时间内完成（例如 < 5 秒）
        assertTrue("Performance test took too long: ${duration}ms", duration < 5000)
    }

    // ==================== 内存压力测试 ====================

    @Test
    fun memoryUsage_underLoad() {
        val items = mutableListOf<SandboxInfo>()
        
        // 创建大量对象
        repeat(10000) { index ->
            items.add(SandboxInfo(
                userId = index,
                sizeBytes = index.toLong() * 1024,
                createdTime = System.currentTimeMillis(),
                appCount = index % 10
            ))
        }
        
        // 验证所有对象创建成功
        assertEquals(10000, items.size)
        
        // 验证随机访问
        val randomItem = items[5000]
        assertEquals(5000, randomItem.userId)
    }
}
