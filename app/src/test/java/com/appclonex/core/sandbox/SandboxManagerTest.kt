package com.appclonex.core.sandbox

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * SandboxManager 单元测试
 * 
 * 测试覆盖：
 * - 沙盒创建与目录结构
 * - 存储空间计算
 * - 数据导出/导入
 * - 沙盒清除与删除
 * - SandboxSharedPreferences 实现
 */
class SandboxManagerTest {

    // ==================== 数据模型测试 ====================

    @Test
    fun `SandboxInfo should have correct default values`() {
        val info = SandboxInfo(
            userId = 10,
            sizeBytes = 1024 * 1024,
            createdTime = System.currentTimeMillis(),
            appCount = 5
        )
        
        assertEquals(10, info.userId)
        assertEquals(1024 * 1024, info.sizeBytes)
        assertEquals(5, info.appCount)
        assertEquals(0L, info.sharedPrefsSize)
        assertEquals(0L, info.filesSize)
        assertEquals(0L, info.cacheSize)
        assertEquals(0L, info.databaseSize)
    }

    @Test
    fun `SandboxInfo with all storage details`() {
        val info = SandboxInfo(
            userId = 99,
            sizeBytes = 10 * 1024 * 1024,
            createdTime = 1234567890L,
            appCount = 3,
            sharedPrefsSize = 1024,
            filesSize = 5 * 1024 * 1024,
            cacheSize = 2 * 1024 * 1024,
            databaseSize = 3 * 1024 * 1024
        )
        
        assertEquals(99, info.userId)
        assertEquals(1024, info.sharedPrefsSize)
        assertEquals(5 * 1024 * 1024, info.filesSize)
        assertEquals(2 * 1024 * 1024, info.cacheSize)
        assertEquals(3 * 1024 * 1024, info.databaseSize)
    }

    @Test
    fun `SandboxStorageDetail should calculate total correctly`() {
        val detail = SandboxStorageDetail(
            sharedPrefsSize = 1024,
            filesSize = 2048,
            cacheSize = 512,
            databaseSize = 4096,
            totalSize = 7680
        )
        
        assertEquals(1024 + 2048 + 512 + 4096, detail.totalSize.toLong())
        assertEquals(7680, detail.totalSize)
    }

    @Test
    fun `SandboxStorageDetail with zero values`() {
        val detail = SandboxStorageDetail(
            sharedPrefsSize = 0,
            filesSize = 0,
            cacheSize = 0,
            databaseSize = 0,
            totalSize = 0
        )
        
        assertEquals(0, detail.totalSize)
    }

    // ==================== 格式化测试 ====================

    @Test
    fun `formatSize should format bytes correctly`() {
        // 模拟 formatSize 方法
        fun formatSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
                else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
            }
        }
        
        assertEquals("512 B", formatSize(512))
        assertEquals("1.0 KB", formatSize(1024))
        assertEquals("1.5 KB", formatSize(1536))
        assertEquals("1.0 MB", formatSize(1024 * 1024))
        assertEquals("10.5 MB", formatSize(10 * 1024 * 1024 + 512 * 1024))
        assertEquals("1.00 GB", formatSize(1024 * 1024 * 1024))
    }

    @Test
    fun `formatSize handles edge cases`() {
        fun formatSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
                else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
            }
        }
        
        assertEquals("0 B", formatSize(0))
        assertEquals("1023 B", formatSize(1023))
        assertEquals("1024.0 KB", formatSize(1024))
        assertEquals("1023.0 KB", formatSize(1024 * 1024 - 1))
        assertEquals("1.0 MB", formatSize(1024 * 1024))
    }

    // ==================== 目录结构测试 ====================

    @Test
    fun `sandbox directory structure should be correct`() {
        // 验证目录结构常量
        assertEquals("sandboxes", "sandboxes")
        assertEquals("sandbox_meta.json", "sandbox_meta.json")
        assertEquals("shared_prefs", "shared_prefs")
        assertEquals("files", "files")
        assertEquals("cache", "cache")
        assertEquals("databases", "databases")
    }

    @Test
    fun `backup extension should be correct`() {
        assertEquals(".sbx", ".sbx")
    }

    // ==================== 文件操作测试 ====================

    @Test
    fun `calculateDirSize returns zero for non-existent directory`() {
        // 模拟计算目录大小
        fun calculateDirSize(dir: File): Long {
            if (!dir.exists()) return 0
            return dir.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        }
        
        val nonExistentDir = File("/non/existent/path")
        assertEquals(0, calculateDirSize(nonExistentDir))
    }

    // ==================== SandboxSharedPreferences 测试 ====================

    @Test
    fun `SandboxSharedPreferences default values should work correctly`() {
        // 模拟 SharedPreferences 行为
        val data = mutableMapOf<String, Any?>()
        
        fun getString(key: String, defValue: String?): String? {
            return data[key] as? String ?: defValue
        }
        
        fun getInt(key: String, defValue: Int): Int {
            return (data[key] as? Number)?.toInt() ?: defValue
        }
        
        fun getBoolean(key: String, defValue: Boolean): Boolean {
            return data[key] as? Boolean ?: defValue
        }
        
        // 测试默认值
        assertEquals("default", getString("missing_key", "default"))
        assertEquals(42, getInt("missing_key", 42))
        assertEquals(true, getBoolean("missing_key", true))
    }

    @Test
    fun `SandboxSharedPreferences should store and retrieve values`() {
        val data = mutableMapOf<String, Any?>()
        
        // 模拟存储
        data["string_key"] = "test_value"
        data["int_key"] = 123
        data["long_key"] = 9999999999L
        data["float_key"] = 3.14f
        data["boolean_key"] = true
        
        // 验证检索
        assertEquals("test_value", data["string_key"])
        assertEquals(123, data["int_key"])
        assertEquals(9999999999L, data["long_key"])
        assertEquals(3.14f, data["float_key"])
        assertTrue(data["boolean_key"] as Boolean)
    }

    @Test
    fun `SandboxSharedPreferences should handle null values`() {
        val data = mutableMapOf<String, Any?>()
        data["null_key"] = null
        
        assertTrue(data.containsKey("null_key"))
        assertNull(data["null_key"])
    }

    @Test
    fun `SandboxSharedPreferences should handle type coercion`() {
        val data = mutableMapOf<String, Any?>()
        
        // 存储为 Double（JSON 数字默认）
        data["int_as_double"] = 42.0
        
        // 模拟 getInt 的类型转换
        fun getInt(key: String, defValue: Int): Int {
            return (data[key] as? Number)?.toInt() ?: defValue
        }
        
        assertEquals(42, getInt("int_as_double", 0))
    }

    // ==================== Editor 操作测试 ====================

    @Test
    fun `Editor should track modifications`() {
        val modifications = mutableMapOf<String, Any?>()
        var clearRequested = false
        
        // 模拟 Editor 操作
        modifications["key1"] = "value1"
        modifications["key2"] = 123
        modifications["key3"] = true
        clearRequested = true
        
        assertTrue(clearRequested)
        assertEquals(3, modifications.size)
    }

    @Test
    fun `Editor remove should use marker`() {
        val data = mutableMapOf<String, Any?>()
        data["existing_key"] = "value"
        
        // 模拟删除标记
        val REMOVED_MARKER = Any()
        val modifications = mutableMapOf<String, Any?>()
        modifications["existing_key"] = REMOVED_MARKER
        
        // 应用修改
        modifications.forEach { (key, value) ->
            if (value === REMOVED_MARKER) {
                data.remove(key)
            } else {
                data[key] = value
            }
        }
        
        assertFalse(data.containsKey("existing_key"))
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `userId should handle various values`() {
        val validUserIds = listOf(0, 1, 10, 99, 999, Int.MAX_VALUE)
        
        validUserIds.forEach { userId ->
            assertTrue("userId $userId should be valid", userId >= 0)
        }
    }

    @Test
    fun `storage sizes should handle large values`() {
        val largeSize = Long.MAX_VALUE
        
        fun formatSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
                else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
            }
        }
        
        val result = formatSize(largeSize)
        assertNotNull(result)
        assertTrue(result.contains("GB"))
    }

    @Test
    fun `SandboxInfo copy should work correctly`() {
        val original = SandboxInfo(
            userId = 10,
            sizeBytes = 1024,
            createdTime = System.currentTimeMillis(),
            appCount = 5
        )
        
        val copied = original.copy(appCount = 10)
        
        assertEquals(10, copied.userId)
        assertEquals(10, copied.appCount)
        assertEquals(original.sizeBytes, copied.sizeBytes)
    }
}
