/*
 * Copyright (c) 2024 AppCloneX Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appclonex.v25

import com.appclonex.domain.model.AppNotification
import com.appclonex.domain.model.AppNotificationId
import com.appclonex.domain.model.ConfigField
import com.appclonex.domain.model.ConfigFieldType
import com.appclonex.domain.model.NextcloudConfig
import com.appclonex.domain.model.NextcloudShareInfo
import com.appclonex.domain.model.NotificationCategory
import com.appclonex.domain.model.NotificationFilter
import com.appclonex.domain.model.NotificationGroup
import com.appclonex.domain.model.NotificationPreferences
import com.appclonex.domain.model.NotificationPriority
import com.appclonex.domain.model.NotificationStatus
import com.appclonex.domain.model.NotificationSummary
import com.appclonex.domain.model.PluginConfig
import com.appclonex.domain.model.PluginDependency
import com.appclonex.domain.model.PluginId
import com.appclonex.domain.model.PluginInstallResult
import com.appclonex.domain.model.PluginManifest
import com.appclonex.domain.model.PluginMetadata
import com.appclonex.domain.model.PluginPermission
import com.appclonex.domain.model.PluginRuntimeInfo
import com.appclonex.domain.model.PluginSearchResult
import com.appclonex.domain.model.PluginState
import com.appclonex.domain.model.WebDavAuthType
import com.appclonex.domain.model.WebDavConfig
import com.appclonex.domain.model.WebDavResource
import com.appclonex.domain.model.WebDavSyncResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// ============================================================================
// V25 Domain 模型测试
// ============================================================================

/**
 * V25 Domain 模型测试
 *
 * 测试 WebDavModels、PluginSystem、NotificationModels 中所有模型的
 * 创建、默认值、枚举完整性和 data class copy 功能。
 * 这些是 JVM 单元测试，不依赖 Android 框架。
 */
class V25ModelTests {

    // ==================== WebDavModels 测试 ====================

    @Test
    fun `WebDavResource 默认值正确`() {
        val resource = WebDavResource()
        assertEquals("", resource.href)
        assertEquals("", resource.displayName)
        assertEquals(0L, resource.contentLength)
        assertEquals(0L, resource.lastModified)
        assertEquals("", resource.mimeType)
        assertFalse(resource.isCollection)
        assertEquals("", resource.etag)
    }

    @Test
    fun `WebDavResource 自定义值正确`() {
        val resource = WebDavResource(
            href = "/remote.php/dav/files/user/docs/",
            displayName = "docs",
            contentLength = 4096L,
            lastModified = 1700000000000L,
            mimeType = "httpd/unix-directory",
            isCollection = true,
            etag = "\"abc123\""
        )
        assertEquals("/remote.php/dav/files/user/docs/", resource.href)
        assertEquals("docs", resource.displayName)
        assertEquals(4096L, resource.contentLength)
        assertTrue(resource.isCollection)
        assertEquals("\"abc123\"", resource.etag)
    }

    @Test
    fun `WebDavResource getFileName 返回 displayName`() {
        val resource = WebDavResource(displayName = "myfile.txt", href = "/path/to/myfile.txt")
        assertEquals("myfile.txt", resource.getFileName())
    }

    @Test
    fun `WebDavResource getFileName 从 href 提取文件名`() {
        val resource = WebDavResource(href = "/path/to/document.pdf", displayName = "")
        assertEquals("document.pdf", resource.getFileName())
    }

    @Test
    fun `WebDavConfig 默认值正确`() {
        val config = WebDavConfig()
        assertEquals("", config.serverUrl)
        assertEquals("", config.username)
        assertEquals("", config.password)
        assertEquals("/", config.basePath)
        assertEquals(30_000, config.connectTimeout)
        assertEquals(60_000, config.readTimeout)
    }

    @Test
    fun `WebDavConfig buildFullUrl 拼接正确`() {
        val config = WebDavConfig(serverUrl = "https://dav.example.com", basePath = "/dav/")
        val url = config.buildFullUrl("backup.json")
        assertEquals("https://dav.example.com/dav/backup.json", url)
    }

    @Test
    fun `WebDavAuthType 枚举值完整性`() {
        assertEquals(4, WebDavAuthType.values().size)
        assertTrue(WebDavAuthType.values().contains(WebDavAuthType.BASIC))
        assertTrue(WebDavAuthType.values().contains(WebDavAuthType.DIGEST))
        assertTrue(WebDavAuthType.values().contains(WebDavAuthType.OAUTH2))
        assertTrue(WebDavAuthType.values().contains(WebDavAuthType.NONE))
    }

    @Test
    fun `WebDavSyncResult 默认值正确`() {
        val result = WebDavSyncResult()
        assertEquals(0, result.uploadedFiles)
        assertEquals(0, result.downloadedFiles)
        assertEquals(0, result.skippedFiles)
        assertEquals(0, result.failedFiles)
        assertEquals(0L, result.totalBytes)
        assertEquals(0L, result.durationMs)
    }

    @Test
    fun `WebDavSyncResult isSuccessful 无失败文件时返回 true`() {
        val result = WebDavSyncResult(uploadedFiles = 5, failedFiles = 0)
        assertTrue(result.isSuccessful())
    }

    @Test
    fun `WebDavSyncResult isSuccessful 有失败文件时返回 false`() {
        val result = WebDavSyncResult(uploadedFiles = 5, failedFiles = 2)
        assertFalse(result.isSuccessful())
    }

    @Test
    fun `WebDavSyncResult totalFiles 计算正确`() {
        val result = WebDavSyncResult(
            uploadedFiles = 3,
            downloadedFiles = 2,
            skippedFiles = 1,
            failedFiles = 1
        )
        assertEquals(7, result.totalFiles())
    }

    @Test
    fun `NextcloudConfig 默认值正确`() {
        val config = NextcloudConfig()
        assertEquals("", config.serverUrl)
        assertEquals("", config.username)
        assertEquals("", config.password)
        assertEquals("", config.appPassword)
    }

    @Test
    fun `NextcloudConfig getEffectivePassword 优先使用 appPassword`() {
        val config = NextcloudConfig(password = "userpass", appPassword = "apppass")
        assertEquals("apppass", config.getEffectivePassword())
    }

    @Test
    fun `NextcloudConfig getEffectivePassword appPassword 为空时回退到 password`() {
        val config = NextcloudConfig(password = "userpass", appPassword = "")
        assertEquals("userpass", config.getEffectivePassword())
    }

    @Test
    fun `NextcloudConfig toWebDavConfig 转换正确`() {
        val config = NextcloudConfig(
            serverUrl = "https://nc.example.com",
            username = "admin",
            appPassword = "token123"
        )
        val webDavConfig = config.toWebDavConfig()
        assertEquals("admin", webDavConfig.username)
        assertEquals("token123", webDavConfig.password)
    }

    @Test
    fun `NextcloudShareInfo 默认值正确`() {
        val share = NextcloudShareInfo()
        assertEquals("", share.shareId)
        assertEquals("", share.shareUrl)
        assertEquals("", share.shareWith)
        assertEquals(1, share.permissions)
        assertNull(share.expireDate)
    }

    @Test
    fun `NextcloudShareInfo canRead 权限检查正确`() {
        val share = NextcloudShareInfo(permissions = NextcloudShareInfo.PERMISSION_READ)
        assertTrue(share.canRead())
    }

    @Test
    fun `NextcloudShareInfo canWrite 权限检查正确`() {
        val share = NextcloudShareInfo(permissions = NextcloudShareInfo.PERMISSION_READ)
        assertFalse(share.canWrite())
    }

    @Test
    fun `NextcloudShareInfo canDelete 权限检查正确`() {
        val share = NextcloudShareInfo(permissions = NextcloudShareInfo.PERMISSION_ALL)
        assertTrue(share.canDelete())
    }

    // ==================== PluginSystem 测试 ====================

    @Test
    fun `PluginId generate 生成唯一ID`() {
        val id1 = PluginId.generate()
        val id2 = PluginId.generate()
        assertNotNull(id1.value)
        assertNotNull(id2.value)
        assertTrue(id1.value.startsWith("plugin_"))
        assertTrue(id1.value != id2.value)
    }

    @Test
    fun `PluginState 枚举值完整性`() {
        assertEquals(5, PluginState.values().size)
    }

    @Test
    fun `PluginPermission 枚举值完整性`() {
        assertEquals(7, PluginPermission.values().size)
    }

    @Test
    fun `PluginMetadata 创建正确`() {
        val id = PluginId.generate()
        val metadata = PluginMetadata(
            id = id,
            name = "测试插件",
            version = "1.0.0",
            description = "测试描述",
            author = "开发者",
            minAppVersion = "2.0.0"
        )
        assertEquals("测试插件", metadata.name)
        assertEquals("1.0.0", metadata.version)
        assertEquals("开发者", metadata.author)
        assertTrue(metadata.permissions.isEmpty())
        assertNull(metadata.iconUrl)
        assertNull(metadata.homepageUrl)
    }

    @Test
    fun `PluginConfig 默认值正确`() {
        val id = PluginId.generate()
        val config = PluginConfig(pluginId = id)
        assertTrue(config.settings.isEmpty())
        assertTrue(config.isEnabled)
    }

    @Test
    fun `PluginRuntimeInfo 默认值正确`() {
        val id = PluginId.generate()
        val info = PluginRuntimeInfo(pluginId = id)
        assertEquals(0L, info.memoryUsage)
        assertEquals(0L, info.cpuTime)
        assertFalse(info.isRunning)
        assertNull(info.lastError)
        assertEquals(0L, info.startTime)
    }

    @Test
    fun `PluginDependency 创建正确`() {
        val dep = PluginDependency(pluginId = "plugin_base", minVersion = "1.0.0", maxVersion = "2.0.0")
        assertEquals("plugin_base", dep.pluginId)
        assertEquals("1.0.0", dep.minVersion)
        assertEquals("2.0.0", dep.maxVersion)
    }

    @Test
    fun `PluginDependency maxVersion 默认为 null`() {
        val dep = PluginDependency(pluginId = "plugin_base", minVersion = "1.0.0")
        assertNull(dep.maxVersion)
    }

    @Test
    fun `ConfigFieldType 枚举值完整性`() {
        assertEquals(6, ConfigFieldType.values().size)
        assertTrue(ConfigFieldType.values().contains(ConfigFieldType.TEXT))
        assertTrue(ConfigFieldType.values().contains(ConfigFieldType.NUMBER))
        assertTrue(ConfigFieldType.values().contains(ConfigFieldType.BOOLEAN))
        assertTrue(ConfigFieldType.values().contains(ConfigFieldType.SELECT))
        assertTrue(ConfigFieldType.values().contains(ConfigFieldType.MULTISELECT))
        assertTrue(ConfigFieldType.values().contains(ConfigFieldType.PASSWORD))
    }

    @Test
    fun `ConfigField 创建正确`() {
        val field = ConfigField(
            key = "max_retry",
            label = "最大重试次数",
            type = ConfigFieldType.NUMBER,
            defaultValue = "3",
            description = "设置最大重试次数",
            required = true
        )
        assertEquals("max_retry", field.key)
        assertEquals(ConfigFieldType.NUMBER, field.type)
        assertEquals("3", field.defaultValue)
        assertTrue(field.required)
    }

    @Test
    fun `ConfigField 默认值正确`() {
        val field = ConfigField(key = "name", label = "名称", type = ConfigFieldType.TEXT)
        assertNull(field.defaultValue)
        assertNull(field.description)
        assertFalse(field.required)
    }

    @Test
    fun `PluginManifest 创建正确`() {
        val id = PluginId.generate()
        val metadata = PluginMetadata(
            id = id, name = "插件", version = "1.0.0",
            description = "描述", author = "作者", minAppVersion = "2.0.0"
        )
        val manifest = PluginManifest(
            metadata = metadata,
            dependencies = listOf(PluginDependency("base", "1.0.0")),
            configSchema = mapOf("key" to ConfigField("key", "标签", ConfigFieldType.TEXT))
        )
        assertEquals(1, manifest.dependencies.size)
        assertEquals(1, manifest.configSchema.size)
    }

    @Test
    fun `PluginManifest 默认值正确`() {
        val id = PluginId.generate()
        val metadata = PluginMetadata(
            id = id, name = "插件", version = "1.0.0",
            description = "描述", author = "作者", minAppVersion = "2.0.0"
        )
        val manifest = PluginManifest(metadata = metadata)
        assertTrue(manifest.dependencies.isEmpty())
        assertTrue(manifest.configSchema.isEmpty())
    }

    @Test
    fun `PluginInstallResult 成功状态正确`() {
        val id = PluginId.generate()
        val result = PluginInstallResult(success = true, pluginId = id)
        assertTrue(result.success)
        assertEquals(id, result.pluginId)
        assertNull(result.errorMessage)
        assertTrue(result.conflicts.isEmpty())
    }

    @Test
    fun `PluginInstallResult 失败状态正确`() {
        val result = PluginInstallResult(
            success = false,
            errorMessage = "版本不兼容",
            conflicts = listOf(PluginId.generate())
        )
        assertFalse(result.success)
        assertEquals("版本不兼容", result.errorMessage)
        assertEquals(1, result.conflicts.size)
    }

    @Test
    fun `PluginSearchResult 默认值正确`() {
        val result = PluginSearchResult()
        assertTrue(result.plugins.isEmpty())
        assertEquals(0, result.totalCount)
        assertEquals(1, result.page)
        assertEquals(20, result.pageSize)
    }

    // ==================== NotificationModels 测试 ====================

    @Test
    fun `AppNotificationId generate 生成递增ID`() {
        val id1 = AppNotificationId.generate()
        val id2 = AppNotificationId.generate()
        assertTrue(id2.value > id1.value)
    }

    @Test
    fun `NotificationPriority 枚举值完整性`() {
        assertEquals(4, NotificationPriority.values().size)
        assertTrue(NotificationPriority.values().contains(NotificationPriority.LOW))
        assertTrue(NotificationPriority.values().contains(NotificationPriority.NORMAL))
        assertTrue(NotificationPriority.values().contains(NotificationPriority.HIGH))
        assertTrue(NotificationPriority.values().contains(NotificationPriority.URGENT))
    }

    @Test
    fun `NotificationCategory 枚举值完整性`() {
        assertEquals(9, NotificationCategory.values().size)
    }

    @Test
    fun `NotificationStatus 枚举值完整性`() {
        assertEquals(5, NotificationStatus.values().size)
    }

    @Test
    fun `AppNotification 创建正确`() {
        val notification = AppNotification(
            title = "测试通知",
            content = "这是一条测试通知",
            category = NotificationCategory.SYSTEM,
            priority = NotificationPriority.NORMAL
        )
        assertEquals("测试通知", notification.title)
        assertEquals("这是一条测试通知", notification.content)
        assertEquals(NotificationCategory.SYSTEM, notification.category)
        assertEquals(NotificationPriority.NORMAL, notification.priority)
        assertEquals(NotificationStatus.PENDING, notification.status)
        assertNull(notification.expireAt)
        assertNull(notification.actionUrl)
        assertNull(notification.groupId)
        assertTrue(notification.metadata.isEmpty())
    }

    @Test
    fun `AppNotification 默认状态为 PENDING`() {
        val notification = AppNotification(title = "标题", content = "内容")
        assertEquals(NotificationStatus.PENDING, notification.status)
    }

    @Test
    fun `NotificationGroup 创建正确`() {
        val group = NotificationGroup(
            id = "group1",
            name = "系统通知",
            icon = "ic_system",
            count = 5,
            unreadCount = 3
        )
        assertEquals("group1", group.id)
        assertEquals("系统通知", group.name)
        assertEquals("ic_system", group.icon)
        assertEquals(5, group.count)
        assertEquals(3, group.unreadCount)
        assertNull(group.latestNotification)
    }

    @Test
    fun `NotificationGroup 默认值正确`() {
        val group = NotificationGroup(id = "g1", name = "测试")
        assertNull(group.icon)
        assertEquals(0, group.count)
        assertNull(group.latestNotification)
        assertEquals(0, group.unreadCount)
    }

    @Test
    fun `NotificationFilter 默认值正确`() {
        val filter = NotificationFilter()
        assertTrue(filter.categories.isEmpty())
        assertTrue(filter.priorities.isEmpty())
        assertTrue(filter.statuses.isEmpty())
        assertEquals("", filter.searchQuery)
        assertNull(filter.fromDate)
        assertNull(filter.toDate)
        assertNull(filter.groupId)
    }

    @Test
    fun `NotificationPreferences 默认值正确`() {
        val prefs = NotificationPreferences()
        assertEquals(NotificationCategory.entries.toSet(), prefs.enabledCategories)
        assertNull(prefs.quietHoursStart)
        assertNull(prefs.quietHoursEnd)
        assertEquals(100, prefs.maxNotificationsPerDay)
        assertNull(prefs.autoDismissAfter)
        assertTrue(prefs.soundEnabled)
        assertTrue(prefs.vibrationEnabled)
        assertFalse(prefs.ledEnabled)
    }

    @Test
    fun `NotificationSummary 默认值正确`() {
        val summary = NotificationSummary()
        assertEquals(0, summary.totalCount)
        assertEquals(0, summary.unreadCount)
        assertEquals(0, summary.urgentCount)
        assertTrue(summary.byCategory.isEmpty())
        assertNull(summary.latestNotification)
    }

    @Test
    fun `NotificationSummary 自定义值正确`() {
        val summary = NotificationSummary(
            totalCount = 50,
            unreadCount = 10,
            urgentCount = 2,
            byCategory = mapOf(NotificationCategory.SYSTEM to 5)
        )
        assertEquals(50, summary.totalCount)
        assertEquals(10, summary.unreadCount)
        assertEquals(2, summary.urgentCount)
        assertEquals(1, summary.byCategory.size)
    }

    // ==================== data class copy 测试 ====================

    @Test
    fun `WebDavResource copy 修改 href 时其他字段不变`() {
        val original = WebDavResource(
            href = "/old/path",
            displayName = "old",
            contentLength = 100L
        )
        val copied = original.copy(href = "/new/path")
        assertEquals("/new/path", copied.href)
        assertEquals("old", copied.displayName)
        assertEquals(100L, copied.contentLength)
    }

    @Test
    fun `AppNotification copy 修改 status 时其他字段不变`() {
        val original = AppNotification(
            title = "标题",
            content = "内容",
            status = NotificationStatus.PENDING
        )
        val copied = original.copy(status = NotificationStatus.READ)
        assertEquals("标题", copied.title)
        assertEquals("内容", copied.content)
        assertEquals(NotificationStatus.READ, copied.status)
    }

    @Test
    fun `PluginMetadata copy 修改 version 时其他字段不变`() {
        val id = PluginId.generate()
        val original = PluginMetadata(
            id = id, name = "插件", version = "1.0.0",
            description = "描述", author = "作者", minAppVersion = "2.0.0"
        )
        val copied = original.copy(version = "2.0.0")
        assertEquals("插件", copied.name)
        assertEquals("2.0.0", copied.version)
        assertEquals("作者", copied.author)
    }

    @Test
    fun `NotificationFilter copy 修改 searchQuery 时其他字段不变`() {
        val original = NotificationFilter(searchQuery = "旧查询")
        val copied = original.copy(searchQuery = "新查询")
        assertEquals("新查询", copied.searchQuery)
        assertTrue(copied.categories.isEmpty())
    }
}
