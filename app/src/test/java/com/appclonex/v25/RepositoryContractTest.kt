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

import com.appclonex.domain.repository.GoogleDriveRepository
import com.appclonex.domain.repository.NextcloudRepository
import com.appclonex.domain.repository.NotificationRepository
import com.appclonex.domain.repository.PerformanceRegressionRepository
import com.appclonex.domain.repository.PluginRepository
import com.appclonex.domain.repository.TemplateMarketplaceRepository
import com.appclonex.domain.repository.WebDavRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.isAbstract
import kotlin.reflect.full.memberFunctions

// ============================================================================
// Repository 接口契约测试
// ============================================================================

/**
 * Repository 接口契约测试
 *
 * 使用 Kotlin 反射验证各 Repository 接口定义的抽象方法数量，
 * 确保接口契约的稳定性和完整性。
 * 这些是 JVM 单元测试，不依赖 Android 框架。
 */
class RepositoryContractTest {

    /**
     * 辅助函数：计算接口中声明的抽象方法数量
     * 不包含来自 Object/Any 的方法（如 equals、hashCode、toString）
     */
    private fun countAbstractMethods(klass: KClass<*>): Int {
        val objectMethodNames = setOf("equals", "hashCode", "toString")
        return klass.memberFunctions.count { method ->
            method.isAbstract && method.name !in objectMethodNames
        }
    }

    // ==================== WebDavRepository ====================

    @Test
    fun `WebDavRepository 接口应存在`() {
        assertNotNull(WebDavRepository::class)
        assertTrue(WebDavRepository::class.isAbstract)
    }

    @Test
    fun `WebDavRepository 接口应声明 9 个抽象方法`() {
        val count = countAbstractMethods(WebDavRepository::class)
        assertEquals(9, count)
    }

    @Test
    fun `WebDavRepository 应包含 testConnection 方法`() {
        val methods = WebDavRepository::class.memberFunctions.map { it.name }
        assertTrue("WebDavRepository 应包含 testConnection 方法", "testConnection" in methods)
    }

    @Test
    fun `WebDavRepository 应包含 syncBackupFiles 方法`() {
        val methods = WebDavRepository::class.memberFunctions.map { it.name }
        assertTrue("WebDavRepository 应包含 syncBackupFiles 方法", "syncBackupFiles" in methods)
    }

    // ==================== NextcloudRepository ====================

    @Test
    fun `NextcloudRepository 接口应存在`() {
        assertNotNull(NextcloudRepository::class)
        assertTrue(NextcloudRepository::class.isAbstract)
    }

    @Test
    fun `NextcloudRepository 接口应声明 5 个抽象方法（不含继承的 WebDavRepository 方法）`() {
        val count = countAbstractMethods(NextcloudRepository::class)
        assertEquals(5, count)
    }

    @Test
    fun `NextcloudRepository 应包含 createShare 方法`() {
        val methods = NextcloudRepository::class.memberFunctions.map { it.name }
        assertTrue("NextcloudRepository 应包含 createShare 方法", "createShare" in methods)
    }

    @Test
    fun `NextcloudRepository 应包含 getUserQuota 方法`() {
        val methods = NextcloudRepository::class.memberFunctions.map { it.name }
        assertTrue("NextcloudRepository 应包含 getUserQuota 方法", "getUserQuota" in methods)
    }

    // ==================== PluginRepository ====================

    @Test
    fun `PluginRepository 接口应存在`() {
        assertNotNull(PluginRepository::class)
        assertTrue(PluginRepository::class.isAbstract)
    }

    @Test
    fun `PluginRepository 接口应声明至少 18 个抽象方法`() {
        val count = countAbstractMethods(PluginRepository::class)
        assertTrue(
            "PluginRepository 应包含至少 18 个抽象方法，实际: $count",
            count >= 18
        )
    }

    @Test
    fun `PluginRepository 应包含 installPlugin 方法`() {
        val methods = PluginRepository::class.memberFunctions.map { it.name }
        assertTrue("PluginRepository 应包含 installPlugin 方法", "installPlugin" in methods)
    }

    @Test
    fun `PluginRepository 应包含 observePluginStates 方法`() {
        val methods = PluginRepository::class.memberFunctions.map { it.name }
        assertTrue("PluginRepository 应包含 observePluginStates 方法", "observePluginStates" in methods)
    }

    // ==================== NotificationRepository ====================

    @Test
    fun `NotificationRepository 接口应存在`() {
        assertNotNull(NotificationRepository::class)
        assertTrue(NotificationRepository::class.isAbstract)
    }

    @Test
    fun `NotificationRepository 接口应声明至少 18 个抽象方法`() {
        val count = countAbstractMethods(NotificationRepository::class)
        assertTrue(
            "NotificationRepository 应包含至少 18 个抽象方法，实际: $count",
            count >= 18
        )
    }

    @Test
    fun `NotificationRepository 应包含 sendNotification 方法`() {
        val methods = NotificationRepository::class.memberFunctions.map { it.name }
        assertTrue("NotificationRepository 应包含 sendNotification 方法", "sendNotification" in methods)
    }

    @Test
    fun `NotificationRepository 应包含 observeSummary 方法`() {
        val methods = NotificationRepository::class.memberFunctions.map { it.name }
        assertTrue("NotificationRepository 应包含 observeSummary 方法", "observeSummary" in methods)
    }

    // ==================== GoogleDriveRepository ====================

    @Test
    fun `GoogleDriveRepository 接口应存在`() {
        assertNotNull(GoogleDriveRepository::class)
        assertTrue(GoogleDriveRepository::class.isAbstract)
    }

    @Test
    fun `GoogleDriveRepository 接口应声明至少 10 个抽象方法`() {
        val count = countAbstractMethods(GoogleDriveRepository::class)
        assertTrue(
            "GoogleDriveRepository 应包含至少 10 个抽象方法，实际: $count",
            count >= 10
        )
    }

    @Test
    fun `GoogleDriveRepository 应包含 authenticate 方法`() {
        val methods = GoogleDriveRepository::class.memberFunctions.map { it.name }
        assertTrue("GoogleDriveRepository 应包含 authenticate 方法", "authenticate" in methods)
    }

    @Test
    fun `GoogleDriveRepository 应包含 uploadBackup 方法`() {
        val methods = GoogleDriveRepository::class.memberFunctions.map { it.name }
        assertTrue("GoogleDriveRepository 应包含 uploadBackup 方法", "uploadBackup" in methods)
    }

    // ==================== TemplateMarketplaceRepository ====================

    @Test
    fun `TemplateMarketplaceRepository 接口应存在`() {
        assertNotNull(TemplateMarketplaceRepository::class)
        assertTrue(TemplateMarketplaceRepository::class.isAbstract)
    }

    @Test
    fun `TemplateMarketplaceRepository 接口应声明至少 13 个抽象方法`() {
        val count = countAbstractMethods(TemplateMarketplaceRepository::class)
        assertTrue(
            "TemplateMarketplaceRepository 应包含至少 13 个抽象方法，实际: $count",
            count >= 13
        )
    }

    @Test
    fun `TemplateMarketplaceRepository 应包含 searchTemplates 方法`() {
        val methods = TemplateMarketplaceRepository::class.memberFunctions.map { it.name }
        assertTrue("TemplateMarketplaceRepository 应包含 searchTemplates 方法", "searchTemplates" in methods)
    }

    @Test
    fun `TemplateMarketplaceRepository 应包含 shareTemplate 方法`() {
        val methods = TemplateMarketplaceRepository::class.memberFunctions.map { it.name }
        assertTrue("TemplateMarketplaceRepository 应包含 shareTemplate 方法", "shareTemplate" in methods)
    }

    // ==================== PerformanceRegressionRepository ====================

    @Test
    fun `PerformanceRegressionRepository 接口应存在`() {
        assertNotNull(PerformanceRegressionRepository::class)
        assertTrue(PerformanceRegressionRepository::class.isAbstract)
    }

    @Test
    fun `PerformanceRegressionRepository 接口应声明至少 14 个抽象方法`() {
        val count = countAbstractMethods(PerformanceRegressionRepository::class)
        assertTrue(
            "PerformanceRegressionRepository 应包含至少 14 个抽象方法，实际: $count",
            count >= 14
        )
    }

    @Test
    fun `PerformanceRegressionRepository 应包含 runFullBenchmark 方法`() {
        val methods = PerformanceRegressionRepository::class.memberFunctions.map { it.name }
        assertTrue(
            "PerformanceRegressionRepository 应包含 runFullBenchmark 方法",
            "runFullBenchmark" in methods
        )
    }

    @Test
    fun `PerformanceRegressionRepository 应包含 getAlerts 方法`() {
        val methods = PerformanceRegressionRepository::class.memberFunctions.map { it.name }
        assertTrue(
            "PerformanceRegressionRepository 应包含 getAlerts 方法",
            "getAlerts" in methods
        )
    }
}
