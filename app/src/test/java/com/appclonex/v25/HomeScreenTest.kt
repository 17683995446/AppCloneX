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

import com.appclonex.domain.model.ApplicationUsageStatisticsModel
import com.appclonex.presentation.screen.home.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// ============================================================================
// HomeScreen UI 状态测试
// ============================================================================

/**
 * HomeScreen UI 状态测试
 *
 * 测试 HomeUiState 的初始值、默认值、data class copy 功能等。
 * 这些是 JVM 单元测试，不依赖 Android 框架。
 */
class HomeScreenTest {

    // ==================== 初始值测试 ====================

    @Test
    fun `HomeUiState 默认实例的 isLoading 应为 false`() {
        // Arrange
        val state = HomeUiState()

        // Assert
        assertFalse(state.isLoading)
    }

    @Test
    fun `HomeUiState 默认实例的 message 应为 null`() {
        // Arrange
        val state = HomeUiState()

        // Assert
        assertNull(state.message)
    }

    @Test
    fun `HomeUiState 默认实例的 error 应为 null`() {
        // Arrange
        val state = HomeUiState()

        // Assert
        assertNull(state.error)
    }

    @Test
    fun `HomeUiState 默认实例的 totalProfiles 应为 0`() {
        // Arrange
        val state = HomeUiState()

        // Assert
        assertEquals(0, state.totalProfiles)
    }

    @Test
    fun `HomeUiState 默认实例的 totalClonedApps 应为 0`() {
        // Arrange
        val state = HomeUiState()

        // Assert
        assertEquals(0, state.totalClonedApps)
    }

    @Test
    fun `HomeUiState 默认实例的 recentActivity 应为空列表`() {
        // Arrange
        val state = HomeUiState()

        // Assert
        assertNotNull(state.recentActivity)
        assertTrue(state.recentActivity.isEmpty())
    }

    // ==================== 自定义值测试 ====================

    @Test
    fun `HomeUiState 使用自定义 totalProfiles 创建时值正确`() {
        // Arrange & Act
        val state = HomeUiState(totalProfiles = 5)

        // Assert
        assertEquals(5, state.totalProfiles)
    }

    @Test
    fun `HomeUiState 使用自定义 totalClonedApps 创建时值正确`() {
        // Arrange & Act
        val state = HomeUiState(totalClonedApps = 12)

        // Assert
        assertEquals(12, state.totalClonedApps)
    }

    @Test
    fun `HomeUiState 使用自定义 isLoading 创建时值正确`() {
        // Arrange & Act
        val state = HomeUiState(isLoading = true)

        // Assert
        assertTrue(state.isLoading)
    }

    @Test
    fun `HomeUiState 使用自定义 message 创建时值正确`() {
        // Arrange & Act
        val state = HomeUiState(message = "加载中")

        // Assert
        assertEquals("加载中", state.message)
    }

    @Test
    fun `HomeUiState 使用自定义 error 创建时值正确`() {
        // Arrange & Act
        val state = HomeUiState(error = "网络错误")

        // Assert
        assertEquals("网络错误", state.error)
    }

    // ==================== data class copy 测试 ====================

    @Test
    fun `HomeUiState copy 修改 totalProfiles 时其他字段保持不变`() {
        // Arrange
        val original = HomeUiState(
            totalProfiles = 3,
            totalClonedApps = 10,
            isLoading = false,
            message = null,
            error = null
        )

        // Act
        val copied = original.copy(totalProfiles = 8)

        // Assert
        assertEquals(8, copied.totalProfiles)
        assertEquals(10, copied.totalClonedApps)
        assertFalse(copied.isLoading)
        assertNull(copied.message)
        assertNull(copied.error)
    }

    @Test
    fun `HomeUiState copy 修改 totalClonedApps 时其他字段保持不变`() {
        // Arrange
        val original = HomeUiState(
            totalProfiles = 2,
            totalClonedApps = 5,
            isLoading = true,
            message = "加载中",
            error = null
        )

        // Act
        val copied = original.copy(totalClonedApps = 20)

        // Assert
        assertEquals(2, copied.totalProfiles)
        assertEquals(20, copied.totalClonedApps)
        assertTrue(copied.isLoading)
        assertEquals("加载中", copied.message)
        assertNull(copied.error)
    }

    @Test
    fun `HomeUiState copy 同时修改多个字段`() {
        // Arrange
        val original = HomeUiState()

        // Act
        val copied = original.copy(
            totalProfiles = 3,
            totalClonedApps = 15,
            isLoading = true,
            error = "同步失败"
        )

        // Assert
        assertEquals(3, copied.totalProfiles)
        assertEquals(15, copied.totalClonedApps)
        assertTrue(copied.isLoading)
        assertEquals("同步失败", copied.error)
        assertNull(copied.message)
    }

    // ==================== recentActivity 测试 ====================

    @Test
    fun `HomeUiState 使用自定义 recentActivity 创建时值正确`() {
        // Arrange
        val activity = listOf(
            ApplicationUsageStatisticsModel(packageName = "com.example.app1", appName = "App1")
        )

        // Act
        val state = HomeUiState(recentActivity = activity)

        // Assert
        assertEquals(1, state.recentActivity.size)
        assertEquals("com.example.app1", state.recentActivity[0].packageName)
        assertEquals("App1", state.recentActivity[0].appName)
    }

    @Test
    fun `HomeUiState copy 修改 recentActivity 时其他字段保持不变`() {
        // Arrange
        val original = HomeUiState(
            totalProfiles = 1,
            totalClonedApps = 2,
            recentActivity = emptyList()
        )
        val newActivity = listOf(
            ApplicationUsageStatisticsModel(packageName = "com.test.app", appName = "TestApp")
        )

        // Act
        val copied = original.copy(recentActivity = newActivity)

        // Assert
        assertEquals(1, copied.totalProfiles)
        assertEquals(2, copied.totalClonedApps)
        assertEquals(1, copied.recentActivity.size)
    }
}
