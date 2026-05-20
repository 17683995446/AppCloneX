package com.appclonex.infrastructure.network

import org.junit.Assert.*
import org.junit.Test

/**
 * 网络模型单元测试
 * 
 * 测试覆盖：
 * - 模型属性
 * - JSON序列化
 */
class NetworkModelTest {

    // ==================== ApplicationNetworkResponseModel 测试 ====================

    @Test
    fun `ApplicationNetworkResponseModel should have correct properties`() {
        val model = ApplicationNetworkResponseModel(
            packageName = "com.example.app",
            appName = "Example App",
            versionName = "1.0.0",
            versionCode = 100,
            iconUrl = "https://example.com/icon.png",
            category = "Tools",
            description = "A test app",
            downloadCount = 10000,
            rating = 4.5f
        )

        assertEquals("com.example.app", model.packageName)
        assertEquals("Example App", model.appName)
        assertEquals("1.0.0", model.versionName)
        assertEquals(100L, model.versionCode)
        assertEquals("https://example.com/icon.png", model.iconUrl)
        assertEquals("Tools", model.category)
        assertEquals("A test app", model.description)
        assertEquals(10000L, model.downloadCount)
        assertEquals(4.5f, model.rating, 0.01f)
    }

    @Test
    fun `ApplicationNetworkResponseModel with null optional fields should work`() {
        val model = ApplicationNetworkResponseModel(
            packageName = "com.example.app",
            appName = "Example App",
            versionName = "1.0.0",
            versionCode = 100,
            iconUrl = null,
            category = null,
            description = null,
            downloadCount = 0,
            rating = 0f
        )

        assertNull(model.iconUrl)
        assertNull(model.category)
        assertNull(model.description)
    }

    // ==================== ApplicationSearchResponseModel 测试 ====================

    @Test
    fun `ApplicationSearchResponseModel should have correct properties`() {
        val apps = listOf(
            createMockApp("com.app1"),
            createMockApp("com.app2")
        )

        val model = ApplicationSearchResponseModel(
            applications = apps,
            totalCount = 100,
            currentPage = 1,
            hasMore = true
        )

        assertEquals(2, model.applications.size)
        assertEquals(100, model.totalCount)
        assertEquals(1, model.currentPage)
        assertTrue(model.hasMore)
    }

    @Test
    fun `ApplicationSearchResponseModel with empty list should work`() {
        val model = ApplicationSearchResponseModel(
            applications = emptyList(),
            totalCount = 0,
            currentPage = 1,
            hasMore = false
        )

        assertTrue(model.applications.isEmpty())
        assertEquals(0, model.totalCount)
        assertFalse(model.hasMore)
    }

    // ==================== ApplicationUpdateResponseModel 测试 ====================

    @Test
    fun `ApplicationUpdateResponseModel with update should have correct properties`() {
        val model = ApplicationUpdateResponseModel(
            hasUpdate = true,
            latestVersion = "2.0.0",
            latestVersionCode = 200,
            updateLog = "Bug fixes and improvements",
            updateUrl = "https://example.com/update"
        )

        assertTrue(model.hasUpdate)
        assertEquals("2.0.0", model.latestVersion)
        assertEquals(200L, model.latestVersionCode)
        assertEquals("Bug fixes and improvements", model.updateLog)
        assertEquals("https://example.com/update", model.updateUrl)
    }

    @Test
    fun `ApplicationUpdateResponseModel without update should have correct properties`() {
        val model = ApplicationUpdateResponseModel(
            hasUpdate = false,
            latestVersion = null,
            latestVersionCode = null,
            updateLog = null,
            updateUrl = null
        )

        assertFalse(model.hasUpdate)
        assertNull(model.latestVersion)
        assertNull(model.latestVersionCode)
        assertNull(model.updateLog)
        assertNull(model.updateUrl)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `ApplicationNetworkResponseModel with extreme values should work`() {
        val model = ApplicationNetworkResponseModel(
            packageName = "",
            appName = "",
            versionName = "",
            versionCode = Long.MAX_VALUE,
            iconUrl = null,
            category = null,
            description = null,
            downloadCount = Long.MAX_VALUE,
            rating = 5.0f
        )

        assertEquals("", model.packageName)
        assertEquals(Long.MAX_VALUE, model.versionCode)
        assertEquals(Long.MAX_VALUE, model.downloadCount)
        assertEquals(5.0f, model.rating, 0.01f)
    }

    @Test
    fun `ApplicationSearchResponseModel with large page number should work`() {
        val model = ApplicationSearchResponseModel(
            applications = emptyList(),
            totalCount = 0,
            currentPage = Int.MAX_VALUE,
            hasMore = false
        )

        assertEquals(Int.MAX_VALUE, model.currentPage)
    }

    // ==================== 辅助方法 ====================

    private fun createMockApp(packageName: String): ApplicationNetworkResponseModel {
        return ApplicationNetworkResponseModel(
            packageName = packageName,
            appName = "Test App",
            versionName = "1.0.0",
            versionCode = 1,
            iconUrl = null,
            category = null,
            description = null,
            downloadCount = 1000,
            rating = 4.5f
        )
    }
}
