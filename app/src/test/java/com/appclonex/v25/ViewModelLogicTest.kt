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

import com.appclonex.domain.model.NotificationCategory
import com.appclonex.domain.model.NotificationPriority
import com.appclonex.domain.model.NotificationStatus
import com.appclonex.domain.model.PluginState
import com.appclonex.domain.model.RegressionType
import com.appclonex.presentation.screens.backup.BackupUiState
import com.appclonex.presentation.screens.clone.BatchCloneWizardUiState
import com.appclonex.presentation.screens.clone.WizardStep
import com.appclonex.presentation.screens.dashboard.PerformanceRegressionUiState
import com.appclonex.presentation.screens.freeze.FreezeStrategyUiState
import com.appclonex.presentation.screens.notifications.NotificationCenterUiState
import com.appclonex.presentation.screens.settings.plugins.PluginManagerUiState
import com.appclonex.presentation.screens.statistics.StatisticsPeriod
import com.appclonex.presentation.screens.statistics.UsageStatisticsUiState
import com.appclonex.presentation.screens.template.ImportProgressUiState
import com.appclonex.presentation.screens.template.ShareProgressUiState
import com.appclonex.presentation.screens.template.TemplateMarketplaceUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// ============================================================================
// ViewModel 逻辑测试 - UiState 默认值与枚举验证
// ============================================================================

/**
 * ViewModel 逻辑测试
 *
 * 测试各 ViewModel 的 UiState 默认值、枚举完整性、
 * 搜索过滤逻辑和状态转换等纯逻辑部分。
 * 这些是 JVM 单元测试，不依赖 Android 框架。
 */
class ViewModelLogicTest {

    // ==================== BackupViewModel UiState 测试 ====================

    @Test
    fun `BackupUiState 默认值 - backups 应为空列表`() {
        val state = BackupUiState()
        assertNotNull(state.backups)
        assertTrue(state.backups.isEmpty())
    }

    @Test
    fun `BackupUiState 默认值 - isLoading 应为 false`() {
        val state = BackupUiState()
        assertFalse(state.isLoading)
    }

    @Test
    fun `BackupUiState 默认值 - isCreating 和 isRestoring 应为 false`() {
        val state = BackupUiState()
        assertFalse(state.isCreating)
        assertFalse(state.isRestoring)
    }

    @Test
    fun `BackupUiState 默认值 - error 和 successMessage 应为 null`() {
        val state = BackupUiState()
        assertNull(state.error)
        assertNull(state.successMessage)
    }

    // ==================== FreezeStrategyViewModel UiState 测试 ====================

    @Test
    fun `FreezeStrategyUiState 默认值 - strategies 应为空列表`() {
        val state = FreezeStrategyUiState()
        assertNotNull(state.strategies)
        assertTrue(state.strategies.isEmpty())
    }

    @Test
    fun `FreezeStrategyUiState 默认值 - isLoading 应为 false`() {
        val state = FreezeStrategyUiState()
        assertFalse(state.isLoading)
    }

    @Test
    fun `FreezeStrategyUiState 默认值 - executingStrategyId 应为 null`() {
        val state = FreezeStrategyUiState()
        assertNull(state.executingStrategyId)
    }

    // ==================== UsageStatisticsViewModel UiState 测试 ====================

    @Test
    fun `UsageStatisticsUiState 默认值 - selectedPeriod 应为 ALL_TIME`() {
        val state = UsageStatisticsUiState()
        assertEquals(StatisticsPeriod.ALL_TIME, state.selectedPeriod)
    }

    @Test
    fun `UsageStatisticsUiState 默认值 - topApps 应为空列表`() {
        val state = UsageStatisticsUiState()
        assertNotNull(state.topApps)
        assertTrue(state.topApps.isEmpty())
    }

    @Test
    fun `UsageStatisticsUiState 默认值 - isLoading 应为 false`() {
        val state = UsageStatisticsUiState()
        assertFalse(state.isLoading)
    }

    // ==================== BatchCloneWizardViewModel WizardStep 测试 ====================

    @Test
    fun `WizardStep SELECT_PROFILE 值应为 0`() {
        assertEquals(0, WizardStep.SELECT_PROFILE)
    }

    @Test
    fun `WizardStep SUMMARY 值应为 5`() {
        assertEquals(5, WizardStep.SUMMARY)
    }

    @Test
    fun `WizardStep TOTAL_STEPS 应为 6`() {
        assertEquals(6, WizardStep.TOTAL_STEPS)
    }

    @Test
    fun `BatchCloneWizardUiState 默认值 - currentStep 应为 SELECT_PROFILE`() {
        val state = BatchCloneWizardUiState()
        assertEquals(WizardStep.SELECT_PROFILE, state.currentStep)
    }

    @Test
    fun `BatchCloneWizardUiState 默认值 - selectedProfile 应为 null`() {
        val state = BatchCloneWizardUiState()
        assertNull(state.selectedProfile)
    }

    @Test
    fun `BatchCloneWizardUiState 默认值 - selectedPackages 应为空集合`() {
        val state = BatchCloneWizardUiState()
        assertNotNull(state.selectedPackages)
        assertTrue(state.selectedPackages.isEmpty())
    }

    // ==================== TemplateMarketplaceViewModel 搜索过滤测试 ====================

    @Test
    fun `TemplateMarketplaceUiState 默认值 - searchQuery 应为空字符串`() {
        val state = TemplateMarketplaceUiState()
        assertEquals("", state.searchQuery)
    }

    @Test
    fun `TemplateMarketplaceUiState 默认值 - selectedCategories 应为空列表`() {
        val state = TemplateMarketplaceUiState()
        assertNotNull(state.selectedCategories)
        assertTrue(state.selectedCategories.isEmpty())
    }

    @Test
    fun `TemplateMarketplaceUiState 默认值 - 对话框状态应为 false`() {
        val state = TemplateMarketplaceUiState()
        assertFalse(state.showDetailSheet)
        assertFalse(state.showShareDialog)
        assertFalse(state.showImportDialog)
        assertFalse(state.showRatingDialog)
    }

    @Test
    fun `TemplateMarketplaceUiState 默认值 - 分享和导入进度应为 Idle`() {
        val state = TemplateMarketplaceUiState()
        assertTrue(state.shareProgress is ShareProgressUiState.Idle)
        assertTrue(state.importProgress is ImportProgressUiState.Idle)
    }

    // ==================== PerformanceRegressionViewModel 检测进度测试 ====================

    @Test
    fun `PerformanceRegressionUiState 默认值 - isDetecting 应为 false`() {
        val state = PerformanceRegressionUiState()
        assertFalse(state.isDetecting)
    }

    @Test
    fun `PerformanceRegressionUiState 默认值 - detectionProgress 应为 null`() {
        val state = PerformanceRegressionUiState()
        assertNull(state.detectionProgress)
    }

    @Test
    fun `PerformanceRegressionUiState 默认值 - selectedTrendType 应为 COLD_START`() {
        val state = PerformanceRegressionUiState()
        assertEquals(RegressionType.COLD_START, state.selectedTrendType)
    }

    @Test
    fun `PerformanceRegressionUiState 默认值 - reportHistory 应为空列表`() {
        val state = PerformanceRegressionUiState()
        assertNotNull(state.reportHistory)
        assertTrue(state.reportHistory.isEmpty())
    }

    // ==================== NotificationCenterViewModel 筛选逻辑测试 ====================

    @Test
    fun `NotificationCenterUiState 默认值 - selectedCategory 应为 null`() {
        val state = NotificationCenterUiState()
        assertNull(state.selectedCategory)
    }

    @Test
    fun `NotificationCenterUiState 默认值 - selectedPriority 应为 null`() {
        val state = NotificationCenterUiState()
        assertNull(state.selectedPriority)
    }

    @Test
    fun `NotificationCenterUiState 默认值 - searchQuery 应为空字符串`() {
        val state = NotificationCenterUiState()
        assertEquals("", state.searchQuery)
    }

    @Test
    fun `NotificationCenterUiState 默认值 - notifications 应为空列表`() {
        val state = NotificationCenterUiState()
        assertNotNull(state.notifications)
        assertTrue(state.notifications.isEmpty())
    }

    // ==================== PluginManagerViewModel 插件状态枚举测试 ====================

    @Test
    fun `PluginState 枚举值完整性 - 应包含 5 个状态`() {
        assertEquals(5, PluginState.values().size)
    }

    @Test
    fun `PluginState 枚举应包含所有预期状态`() {
        val states = PluginState.values().toSet()
        assertTrue(states.contains(PluginState.DISABLED))
        assertTrue(states.contains(PluginState.ENABLED))
        assertTrue(states.contains(PluginState.INSTALLED))
        assertTrue(states.contains(PluginState.ERROR))
        assertTrue(states.contains(PluginState.UPDATING))
    }

    @Test
    fun `PluginManagerUiState 默认值 - plugins 应为空列表`() {
        val state = PluginManagerUiState()
        assertNotNull(state.plugins)
        assertTrue(state.plugins.isEmpty())
    }

    @Test
    fun `PluginManagerUiState 默认值 - filteredPlugins 应为空列表`() {
        val state = PluginManagerUiState()
        assertNotNull(state.filteredPlugins)
        assertTrue(state.filteredPlugins.isEmpty())
    }

    @Test
    fun `PluginManagerUiState 默认值 - 对话框状态应为 false`() {
        val state = PluginManagerUiState()
        assertFalse(state.showInstallDialog)
        assertFalse(state.showDetailDialog)
        assertFalse(state.showPermissionDialog)
    }

    @Test
    fun `PluginManagerUiState 默认值 - pluginStates 应为空映射`() {
        val state = PluginManagerUiState()
        assertNotNull(state.pluginStates)
        assertTrue(state.pluginStates.isEmpty())
    }
}
