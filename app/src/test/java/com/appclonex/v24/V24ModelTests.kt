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

package com.appclonex.v24

import com.appclonex.domain.model.DeviceInfo
import com.appclonex.domain.model.GoogleDriveAuthState
import com.appclonex.domain.model.GoogleDriveConflictResolution
import com.appclonex.domain.model.GoogleDriveFile
import com.appclonex.domain.model.GoogleDriveSyncStatus
import com.appclonex.domain.model.GoogleDriveUploadProgress
import com.appclonex.domain.model.PerformanceAlert
import com.appclonex.domain.model.PerformanceBaseline
import com.appclonex.domain.model.RegressionItem
import com.appclonex.domain.model.RegressionReport
import com.appclonex.domain.model.RegressionSeverity
import com.appclonex.domain.model.RegressionThreshold
import com.appclonex.domain.model.RegressionTrend
import com.appclonex.domain.model.RegressionType
import com.appclonex.domain.model.SortField
import com.appclonex.domain.model.SortOrder
import com.appclonex.domain.model.SyncConflictInfo
import com.appclonex.domain.model.TemplateCategory
import com.appclonex.domain.model.TemplateCategoryStats
import com.appclonex.domain.model.TemplateImportResult
import com.appclonex.domain.model.TemplateMarketplaceStats
import com.appclonex.domain.model.TemplateRating
import com.appclonex.domain.model.TemplateSearchFilter
import com.appclonex.domain.model.TemplateShareInfo
import com.appclonex.domain.model.TemplateValidationResult
import com.appclonex.domain.model.TrendDataPoint
import com.appclonex.domain.model.TrendDirection
import com.appclonex.domain.model.TransferState
import com.appclonex.domain.usecase.monitoring.ExportFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// ============================================================================
// GoogleDriveModels 测试
// ============================================================================

/**
 * Google Drive 模型测试
 * 验证 GoogleDriveModels 中所有数据类的创建和默认值
 */
class GoogleDriveModelsTests {

    @Test
    fun `GoogleDriveFile 默认值正确`() {
        val file = GoogleDriveFile()
        assertEquals("", file.id)
        assertEquals("", file.name)
        assertEquals("", file.mimeType)
        assertNull(file.size)
        assertEquals("", file.modifiedTime)
        assertNull(file.md5Checksum)
        assertNull(file.webViewLink)
    }

    @Test
    fun `GoogleDriveFile 自定义值正确`() {
        val file = GoogleDriveFile(
            id = "file123",
            name = "backup.zip",
            mimeType = "application/zip",
            size = 1024L,
        )
        assertEquals("file123", file.id)
        assertEquals("backup.zip", file.name)
        assertEquals(1024L, file.size)
    }

    @Test
    fun `TransferState 枚举包含 5 种状态`() {
        assertEquals(5, TransferState.values().size)
        assertNotNull(TransferState.IDLE)
        assertNotNull(TransferState.UPLOADING)
        assertNotNull(TransferState.PAUSED)
        assertNotNull(TransferState.COMPLETED)
        assertNotNull(TransferState.FAILED)
    }

    @Test
    fun `GoogleDriveUploadProgress 默认值和进度计算`() {
        val progress = GoogleDriveUploadProgress()
        assertEquals(0L, progress.bytesTransferred)
        assertEquals(-1L, progress.totalBytes)
        assertEquals(TransferState.IDLE, progress.state)
        // 总大小未知时返回 -1
        assertEquals(-1, progress.progressPercent())
    }

    @Test
    fun `GoogleDriveUploadProgress 进度百分比计算正确`() {
        val progress = GoogleDriveUploadProgress(
            bytesTransferred = 500L,
            totalBytes = 1000L,
            state = TransferState.UPLOADING,
        )
        assertEquals(50, progress.progressPercent())
    }

    @Test
    fun `GoogleDriveSyncStatus 枚举包含 5 种状态`() {
        assertEquals(5, GoogleDriveSyncStatus.values().size)
    }

    @Test
    fun `GoogleDriveConflictResolution 枚举包含 4 种策略`() {
        assertEquals(4, GoogleDriveConflictResolution.values().size)
    }

    @Test
    fun `SyncConflictInfo 默认值和比较方法`() {
        val conflict = SyncConflictInfo()
        assertFalse(conflict.isLocalNewer())
        assertFalse(conflict.isRemoteNewer())
    }

    @Test
    fun `SyncConflictInfo 本地更新判断正确`() {
        val conflict = SyncConflictInfo(
            localModifiedTime = 2000L,
            remoteModifiedTime = 1000L,
        )
        assertTrue(conflict.isLocalNewer())
        assertFalse(conflict.isRemoteNewer())
    }

    @Test
    fun `GoogleDriveAuthState 默认值正确`() {
        val auth = GoogleDriveAuthState()
        assertFalse(auth.isSignedIn)
        assertEquals("", auth.accountEmail)
        assertEquals("", auth.accountName)
        assertEquals(0L, auth.lastAuthTime)
    }
}

// ============================================================================
// PerformanceRegression 测试
// ============================================================================

/**
 * 性能回归模型测试
 * 验证 PerformanceRegression 中所有枚举和数据类
 */
class PerformanceRegressionModelsTests {

    @Test
    fun `RegressionSeverity 枚举包含 5 种严重程度`() {
        assertEquals(5, RegressionSeverity.values().size)
        assertNotNull(RegressionSeverity.NONE)
        assertNotNull(RegressionSeverity.MINOR)
        assertNotNull(RegressionSeverity.MODERATE)
        assertNotNull(RegressionSeverity.SEVERE)
        assertNotNull(RegressionSeverity.CRITICAL)
    }

    @Test
    fun `RegressionType 枚举包含 6 种类型`() {
        assertEquals(6, RegressionType.values().size)
        assertNotNull(RegressionType.COLD_START)
        assertNotNull(RegressionType.MEMORY_USAGE)
        assertNotNull(RegressionType.DATABASE_QUERY)
        assertNotNull(RegressionType.UI_RENDERING)
        assertNotNull(RegressionType.BATTERY_USAGE)
        assertNotNull(RegressionType.STORAGE_IO)
    }

    @Test
    fun `TrendDirection 枚举包含 4 种方向`() {
        assertEquals(4, TrendDirection.values().size)
        assertNotNull(TrendDirection.IMPROVING)
        assertNotNull(TrendDirection.STABLE)
        assertNotNull(TrendDirection.DEGRADING)
        assertNotNull(TrendDirection.UNKNOWN)
    }

    @Test
    fun `RegressionThreshold 默认值正确`() {
        val threshold = RegressionThreshold()
        assertEquals(RegressionType.COLD_START, threshold.type)
        assertEquals(10f, threshold.warningThreshold, 0.001f)
        assertEquals(50f, threshold.criticalThreshold, 0.001f)
        assertEquals(5f, threshold.maxAcceptableChange, 0.001f)
    }

    @Test
    fun `RegressionItem 默认值正确`() {
        val item = RegressionItem()
        assertEquals(RegressionType.COLD_START, item.type)
        assertEquals("", item.metricName)
        assertEquals(0f, item.currentValue, 0.001f)
        assertEquals(0f, item.previousValue, 0.001f)
        assertEquals(RegressionSeverity.NONE, item.severity)
    }

    @Test
    fun `RegressionReport 默认值正确`() {
        val report = RegressionReport()
        assertEquals("", report.id)
        assertEquals(0, report.versionCode)
        assertTrue(report.regressions.isEmpty())
        assertEquals(0f, report.overallScore, 0.001f)
        assertTrue(report.recommendations.isEmpty())
    }

    @Test
    fun `PerformanceBaseline 创建正确`() {
        val baseline = PerformanceBaseline(
            deviceModel = "Pixel 7",
            androidVersion = "14",
            metrics = mapOf("cold_start" to 500f),
            sampleSize = 100,
        )
        assertEquals("Pixel 7", baseline.deviceModel)
        assertEquals("14", baseline.androidVersion)
        assertEquals(1, baseline.metrics.size)
        assertEquals(500f, baseline.metrics["cold_start"]!!, 0.001f)
        assertEquals(100, baseline.sampleSize)
    }

    @Test
    fun `PerformanceAlert 默认值正确`() {
        val alert = PerformanceAlert()
        assertEquals("", alert.id)
        assertFalse(alert.isRead)
        assertNull(alert.actionUrl)
    }

    @Test
    fun `TrendDataPoint 默认值正确`() {
        val point = TrendDataPoint()
        assertEquals(0L, point.timestamp)
        assertEquals(0f, point.value, 0.001f)
        assertEquals(0, point.versionCode)
    }

    @Test
    fun `RegressionTrend 默认值正确`() {
        val trend = RegressionTrend()
        assertEquals(RegressionType.COLD_START, trend.type)
        assertTrue(trend.dataPoints.isEmpty())
        assertEquals(0f, trend.slope, 0.001f)
        assertEquals(TrendDirection.UNKNOWN, trend.direction)
        assertEquals(0f, trend.prediction, 0.001f)
    }
}

// ============================================================================
// TemplateMarketplace 测试
// ============================================================================

/**
 * 模板市场模型测试
 * 验证 TemplateMarketplace 中所有枚举和数据类
 */
class TemplateMarketplaceModelsTests {

    @Test
    fun `TemplateCategory 枚举包含 6 种分类`() {
        assertEquals(6, TemplateCategory.values().size)
    }

    @Test
    fun `TemplateRating 默认值正确`() {
        val rating = TemplateRating()
        assertEquals("", rating.userId)
        assertEquals("", rating.userName)
        assertEquals(0f, rating.rating, 0.001f)
        assertEquals("", rating.comment)
    }

    @Test
    fun `TemplateShareInfo 默认值正确`() {
        val shareInfo = TemplateShareInfo()
        assertEquals("", shareInfo.shareCode)
        assertEquals("", shareInfo.shareUrl)
        assertEquals(0, shareInfo.downloadCount)
        assertEquals(0, shareInfo.shareCount)
    }

    @Test
    fun `TemplateValidationResult 默认有效`() {
        val result = TemplateValidationResult()
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `TemplateValidationResult 自定义错误`() {
        val result = TemplateValidationResult(
            isValid = false,
            errors = listOf("缺少必填字段"),
            warnings = listOf("建议添加描述"),
        )
        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        assertEquals(1, result.warnings.size)
    }

    @Test
    fun `TemplateImportResult 默认失败`() {
        val result = TemplateImportResult()
        assertFalse(result.success)
        assertNull(result.template)
        assertNull(result.errorMessage)
    }

    @Test
    fun `TemplateImportResult 成功状态`() {
        val result = TemplateImportResult(
            success = true,
            errorMessage = null,
        )
        assertTrue(result.success)
    }

    @Test
    fun `TemplateCategoryStats 默认值正确`() {
        val stats = TemplateCategoryStats()
        assertEquals(TemplateCategory.GENERAL, stats.category)
        assertEquals(0, stats.templateCount)
        assertEquals(0, stats.totalDownloads)
        assertEquals(0f, stats.averageRating, 0.001f)
    }

    @Test
    fun `TemplateMarketplaceStats 默认值正确`() {
        val stats = TemplateMarketplaceStats()
        assertEquals(0, stats.totalTemplates)
        assertEquals(0, stats.totalDownloads)
        assertEquals(0, stats.totalShares)
        assertNull(stats.topCategory)
    }
}

// ============================================================================
// 枚举和通用模型测试
// ============================================================================

/**
 * 枚举和通用模型测试
 * 验证排序字段、导出格式等枚举
 */
class EnumAndCommonModelTests {

    @Test
    fun `ExportFormat 枚举包含 3 种格式`() {
        assertEquals(3, ExportFormat.values().size)
        assertNotNull(ExportFormat.JSON)
        assertNotNull(ExportFormat.CSV)
        assertNotNull(ExportFormat.MARKDOWN)
    }

    @Test
    fun `SortField 枚举包含 5 种字段`() {
        assertEquals(5, SortField.values().size)
        assertNotNull(SortField.NAME)
        assertNotNull(SortField.RATING)
        assertNotNull(SortField.DOWNLOADS)
        assertNotNull(SortField.CREATED)
        assertNotNull(SortField.UPDATED)
    }

    @Test
    fun `SortOrder 枚举包含 2 种顺序`() {
        assertEquals(2, SortOrder.values().size)
        assertNotNull(SortOrder.ASCENDING)
        assertNotNull(SortOrder.DESCENDING)
    }

    @Test
    fun `TemplateSearchFilter 默认值正确`() {
        val filter = TemplateSearchFilter()
        assertEquals("", filter.query)
        assertTrue(filter.categories.isEmpty())
        assertEquals(0f, filter.minRating, 0.001f)
        assertEquals(SortField.UPDATED, filter.sortBy)
        assertEquals(SortOrder.DESCENDING, filter.sortOrder)
    }

    @Test
    fun `DeviceInfo 默认值正确`() {
        val info = DeviceInfo()
        assertEquals("", info.model)
        assertEquals("", info.androidVersion)
        assertEquals(0, info.sdkVersion)
        assertEquals(0L, info.totalMemory)
        assertEquals(0, info.cpuCores)
        assertEquals(0, info.screenDensity)
    }
}
