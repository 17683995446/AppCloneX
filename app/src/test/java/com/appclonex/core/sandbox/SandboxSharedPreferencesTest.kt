package com.appclonex.core.sandbox

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * SandboxSharedPreferences 单元测试
 * 
 * 测试覆盖：
 * - 基本读写操作
 * - 监听器功能
 * - 并发安全
 * - 文件持久化
 */
class SandboxSharedPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var prefsDir: File
    private lateinit var prefs: SandboxSharedPreferences

    @Before
    fun setup() {
        prefsDir = tempFolder.newFolder("prefs")
        prefs = SandboxSharedPreferences(prefsDir, "test_prefs")
    }

    // ==================== 基本读写测试 ====================

    @Test
    fun `put and get string should work`() {
        prefs.edit().putString("key1", "value1").apply()
        assertEquals("value1", prefs.getString("key1", null))
    }

    @Test
    fun `put and get int should work`() {
        prefs.edit().putInt("int_key", 42).apply()
        assertEquals(42, prefs.getInt("int_key", 0))
    }

    @Test
    fun `put and get long should work`() {
        prefs.edit().putLong("long_key", 9999999999L).apply()
        assertEquals(9999999999L, prefs.getLong("long_key", 0))
    }

    @Test
    fun `put and get float should work`() {
        prefs.edit().putFloat("float_key", 3.14f).apply()
        assertEquals(3.14f, prefs.getFloat("float_key", 0f), 0.01f)
    }

    @Test
    fun `put and get boolean should work`() {
        prefs.edit().putBoolean("bool_key", true).apply()
        assertTrue(prefs.getBoolean("bool_key", false))
    }

    @Test
    fun `put and get string set should work`() {
        val set = setOf("a", "b", "c")
        prefs.edit().putStringSet("set_key", set).apply()
        assertEquals(set, prefs.getStringSet("set_key", null))
    }

    // ==================== 默认值测试 ====================

    @Test
    fun `get string with default value should return default when key not exists`() {
        assertEquals("default", prefs.getString("nonexistent", "default"))
    }

    @Test
    fun `get int with default value should return default when key not exists`() {
        assertEquals(100, prefs.getInt("nonexistent", 100))
    }

    @Test
    fun `get boolean with default value should return default when key not exists`() {
        assertTrue(prefs.getBoolean("nonexistent", true))
    }

    // ==================== 删除和清空测试 ====================

    @Test
    fun `remove should delete key`() {
        prefs.edit().putString("key_to_remove", "value").apply()
        assertTrue(prefs.contains("key_to_remove"))

        prefs.edit().remove("key_to_remove").apply()
        assertFalse(prefs.contains("key_to_remove"))
        assertNull(prefs.getString("key_to_remove", null))
    }

    @Test
    fun `clear should remove all keys`() {
        prefs.edit()
            .putString("key1", "value1")
            .putInt("key2", 42)
            .putBoolean("key3", true)
            .apply()

        prefs.edit().clear().apply()

        assertFalse(prefs.contains("key1"))
        assertFalse(prefs.contains("key2"))
        assertFalse(prefs.contains("key3"))
        assertTrue(prefs.getAll().isEmpty())
    }

    // ==================== contains 测试 ====================

    @Test
    fun `contains should return true for existing key`() {
        prefs.edit().putString("existing_key", "value").apply()
        assertTrue(prefs.contains("existing_key"))
    }

    @Test
    fun `contains should return false for non-existing key`() {
        assertFalse(prefs.contains("non_existing_key"))
    }

    // ==================== getAll 测试 ====================

    @Test
    fun `getAll should return all key-value pairs`() {
        prefs.edit()
            .putString("key1", "value1")
            .putInt("key2", 42)
            .putBoolean("key3", true)
            .apply()

        val all = prefs.getAll()
        assertEquals(3, all.size)
        assertEquals("value1", all["key1"])
        assertEquals(42, all["key2"])
        assertEquals(true, all["key3"])
    }

    // ==================== 监听器测试 ====================

    @Test
    fun `listener should be notified on value change`() {
        val listener = mockk<SharedPreferences.OnSharedPreferenceChangeListener>(relaxed = true)
        prefs.registerOnSharedPreferenceChangeListener(listener)

        prefs.edit().putString("test_key", "test_value").apply()

        verify { listener.onSharedPreferenceChanged(prefs, "test_key") }

        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    @Test
    fun `listener should not be notified after unregister`() {
        val listener = mockk<SharedPreferences.OnSharedPreferenceChangeListener>(relaxed = true)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        prefs.unregisterOnSharedPreferenceChangeListener(listener)

        prefs.edit().putString("test_key", "test_value").apply()

        verify(exactly = 0) { listener.onSharedPreferenceChanged(any(), any()) }
    }

    @Test
    fun `multiple listeners should all be notified`() {
        val listener1 = mockk<SharedPreferences.OnSharedPreferenceChangeListener>(relaxed = true)
        val listener2 = mockk<SharedPreferences.OnSharedPreferenceChangeListener>(relaxed = true)

        prefs.registerOnSharedPreferenceChangeListener(listener1)
        prefs.registerOnSharedPreferenceChangeListener(listener2)

        prefs.edit().putString("test_key", "test_value").apply()

        verify { listener1.onSharedPreferenceChanged(prefs, "test_key") }
        verify { listener2.onSharedPreferenceChanged(prefs, "test_key") }
    }

    // ==================== commit vs apply 测试 ====================

    @Test
    fun `commit should return true on success`() {
        val result = prefs.edit().putString("key", "value").commit()
        assertTrue(result)
        assertEquals("value", prefs.getString("key", null))
    }

    @Test
    fun `apply should work asynchronously`() {
        prefs.edit().putString("key", "value").apply()
        // apply 是异步的，但在这个测试中应该很快完成
        Thread.sleep(100)
        assertEquals("value", prefs.getString("key", null))
    }

    // ==================== 类型转换测试 ====================

    @Test
    fun `getInt should convert Double to Int`() {
        // JSON 数字默认解析为 Double
        prefs.edit().putFloat("number_key", 42f).apply()
        // Float 存储为 Double，获取 Int 时应该能正确转换
        assertEquals(42, prefs.getInt("number_key", 0))
    }

    @Test
    fun `getLong should convert Double to Long`() {
        prefs.edit().putInt("number_key", 42).apply()
        assertEquals(42L, prefs.getLong("number_key", 0))
    }

    @Test
    fun `getFloat should convert Double to Float`() {
        prefs.edit().putInt("number_key", 42).apply()
        assertEquals(42f, prefs.getFloat("number_key", 0f), 0.01f)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `empty string value should work`() {
        prefs.edit().putString("empty_key", "").apply()
        assertEquals("", prefs.getString("empty_key", "default"))
    }

    @Test
    fun `null string value should work`() {
        prefs.edit().putString("null_key", null).apply()
        assertNull(prefs.getString("null_key", "default"))
    }

    @Test
    fun `empty string set should work`() {
        prefs.edit().putStringSet("empty_set", emptySet()).apply()
        assertTrue(prefs.getStringSet("empty_set", null)?.isEmpty() == true)
    }

    @Test
    fun `large string value should work`() {
        val largeString = "x".repeat(10000)
        prefs.edit().putString("large_key", largeString).apply()
        assertEquals(largeString, prefs.getString("large_key", null))
    }

    @Test
    fun `special characters in key should work`() {
        prefs.edit().putString("key.with.special!chars@123", "value").apply()
        assertEquals("value", prefs.getString("key.with.special!chars@123", null))
    }

    // ==================== 文件持久化测试 ====================

    @Test
    fun `data should persist to file`() {
        prefs.edit().putString("persist_key", "persist_value").apply()
        Thread.sleep(100) // 等待写入完成

        // 创建新的实例，应该能读取到之前的数据
        val newPrefs = SandboxSharedPreferences(prefsDir, "test_prefs")
        assertEquals("persist_value", newPrefs.getString("persist_key", null))
    }

    @Test
    fun `multiple instances should share data`() {
        val prefs1 = SandboxSharedPreferences(prefsDir, "shared")
        val prefs2 = SandboxSharedPreferences(prefsDir, "shared")

        prefs1.edit().putString("shared_key", "shared_value").apply()
        Thread.sleep(100)

        assertEquals("shared_value", prefs2.getString("shared_key", null))
    }

    // ==================== 并发安全测试 ====================

    @Test
    fun `concurrent writes should be safe`() {
        val threads = (1..10).map { index ->
            Thread {
                repeat(10) { inner ->
                    prefs.edit().putString("concurrent_key_$index", "value_$inner").apply()
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // 所有写入都应该成功
        (1..10).forEach { index ->
            assertNotNull(prefs.getString("concurrent_key_$index", null))
        }
    }
}
