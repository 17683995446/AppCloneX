package com.appclonex.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * 领域模型单元测试
 * 
 * 测试覆盖：
 * - ClonedApplication 数据模型
 * - UserProfileConfig 数据模型
 * - ApplicationUsageStatistics 数据模型
 */
class DomainModelTest {

    // ==================== ClonedApplication 测试 ====================

    @Test
    fun `ClonedApplication should have correct default values`() {
        val app = ClonedApplication(
            packageName = "com.example.app",
            appName = "Example App",
            userId = 10
        )

        assertEquals("com.example.app", app.packageName)
        assertEquals("Example App", app.appName)
        assertEquals(10, app.userId)
    }

    @Test
    fun `ClonedApplication copy should work correctly`() {
        val original = ClonedApplication(
            packageName = "com.example.app",
            appName = "Example App",
            userId = 10
        )

        val copied = original.copy(appName = "Modified App")

        assertEquals("com.example.app", copied.packageName)
        assertEquals("Modified App", copied.appName)
        assertEquals(10, copied.userId)
    }

    @Test
    fun `ClonedApplication equality should work correctly`() {
        val app1 = ClonedApplication("com.example.app", "App", 10)
        val app2 = ClonedApplication("com.example.app", "App", 10)
        val app3 = ClonedApplication("com.example.app", "Different", 10)

        assertEquals(app1, app2)
        assertNotEquals(app1, app3)
    }

    // ==================== UserProfileConfig 测试 ====================

    @Test
    fun `UserProfileConfig should have correct properties`() {
        val config = UserProfileConfig(
            id = 1,
            name = "Work Profile",
            userId = 10,
            iconPath = "/path/to/icon.png",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        assertEquals(1L, config.id)
        assertEquals("Work Profile", config.name)
        assertEquals(10, config.userId)
        assertEquals("/path/to/icon.png", config.iconPath)
    }

    @Test
    fun `UserProfileConfig with null iconPath should work`() {
        val config = UserProfileConfig(
            id = 1,
            name = "Default Profile",
            userId = 0,
            iconPath = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        assertNull(config.iconPath)
    }

    // ==================== ApplicationUsageStatistics 测试 ====================

    @Test
    fun `ApplicationUsageStatistics should calculate correctly`() {
        val stats = ApplicationUsageStatistics(
            packageName = "com.example.app",
            totalUsageTimeMs = 3600000, // 1 hour
            launchCount = 10,
            lastUsedTime = System.currentTimeMillis()
        )

        assertEquals("com.example.app", stats.packageName)
        assertEquals(3600000L, stats.totalUsageTimeMs)
        assertEquals(10, stats.launchCount)
    }

    @Test
    fun `ApplicationUsageStatistics average usage time should be correct`() {
        val stats = ApplicationUsageStatistics(
            packageName = "com.example.app",
            totalUsageTimeMs = 600000, // 10 minutes
            launchCount = 5,
            lastUsedTime = System.currentTimeMillis()
        )

        val avgUsageTime = stats.totalUsageTimeMs / stats.launchCount
        assertEquals(120000L, avgUsageTime) // 2 minutes average
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `ClonedApplication with empty strings should work`() {
        val app = ClonedApplication(
            packageName = "",
            appName = "",
            userId = 0
        )

        assertEquals("", app.packageName)
        assertEquals("", app.appName)
        assertEquals(0, app.userId)
    }

    @Test
    fun `UserProfileConfig timestamps should be valid`() {
        val now = System.currentTimeMillis()
        val config = UserProfileConfig(
            id = 1,
            name = "Test",
            userId = 0,
            iconPath = null,
            createdAt = now,
            updatedAt = now
        )

        assertTrue(config.createdAt > 0)
        assertTrue(config.updatedAt >= config.createdAt)
    }

    @Test
    fun `ApplicationUsageStatistics with zero values should work`() {
        val stats = ApplicationUsageStatistics(
            packageName = "com.example.app",
            totalUsageTimeMs = 0,
            launchCount = 0,
            lastUsedTime = 0
        )

        assertEquals(0L, stats.totalUsageTimeMs)
        assertEquals(0, stats.launchCount)
    }
}
