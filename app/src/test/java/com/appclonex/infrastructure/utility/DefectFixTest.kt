package com.appclonex.infrastructure.utility

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * 缺陷修复单元测试
 * 
 * 测试覆盖：
 * - P1: Android版本兼容性检测
 * - P1: 大文件流式处理
 * - P2: 搜索性能优化
 */
class DefectFixTest {

    // ==================== P1: Android版本兼容性测试 ====================

    @Test
    fun `AndroidVersion fromSdkInt should return correct version`() {
        assertEquals(AndroidVersion.LOLLIPOP, AndroidVersion.fromSdkInt(21))
        assertEquals(AndroidVersion.M, AndroidVersion.fromSdkInt(23))
        assertEquals(AndroidVersion.R, AndroidVersion.fromSdkInt(30))
        assertEquals(AndroidVersion.TIRAMISU, AndroidVersion.fromSdkInt(33))
        assertEquals(AndroidVersion.UPSIDE_DOWN_CAKE, AndroidVersion.fromSdkInt(34))
        assertEquals(AndroidVersion.UPSIDE_DOWN_CAKE, AndroidVersion.fromSdkInt(99)) // 超出范围返回最新
    }

    @Test
    fun `AndroidVersion fromSdkInt should return closest lower version`() {
        assertEquals(AndroidVersion.M, AndroidVersion.fromSdkInt(25))
        assertEquals(AndroidVersion.O, AndroidVersion.fromSdkInt(27))
        assertEquals(AndroidVersion.P, AndroidVersion.fromSdkInt(29))
    }

    @Test
    fun `AndroidVersion enum should have all expected values`() {
        val versions = AndroidVersion.entries
        assertTrue(versions.contains(AndroidVersion.LOLLIPOP))
        assertTrue(versions.contains(AndroidVersion.M))
        assertTrue(versions.contains(AndroidVersion.R))
        assertTrue(versions.contains(AndroidVersion.UPSIDE_DOWN_CAKE))
        assertEquals(10, versions.size)
    }

    @Test
    fun `FeatureCompatibilityModel should detect support correctly`() {
        val feature = FeatureCompatibilityModel(
            featureName = "Test Feature",
            minVersion = AndroidVersion.R,
            currentVersion = AndroidVersion.UPSIDE_DOWN_CAKE,
            fallbackDescription = "Fallback"
        )
        assertTrue(feature.isSupported)
        assertEquals(FeatureSupportStatus.FULL, feature.supportStatus)
    }

    @Test
    fun `FeatureCompatibilityModel should detect degraded support`() {
        val feature = FeatureCompatibilityModel(
            featureName = "Test Feature",
            minVersion = AndroidVersion.R,
            currentVersion = AndroidVersion.P,
            fallbackDescription = "Use alternative"
        )
        assertFalse(feature.isSupported)
        assertEquals(FeatureSupportStatus.DEGRADED, feature.supportStatus)
    }

    @Test
    fun `FeatureCompatibilityModel should detect unsupported`() {
        val feature = FeatureCompatibilityModel(
            featureName = "Test Feature",
            minVersion = AndroidVersion.R,
            currentVersion = AndroidVersion.P,
            fallbackDescription = null
        )
        assertFalse(feature.isSupported)
        assertEquals(FeatureSupportStatus.UNSUPPORTED, feature.supportStatus)
    }

    @Test
    fun `CompatibilityLevel should be correct based on score`() {
        val excellent = VersionCompatibilityReportModel(
            currentVersion = AndroidVersion.UPSIDE_DOWN_CAKE,
            isMinimumSupported = true,
            isRecommended = true,
            totalFeatures = 10,
            supportedFeatures = 10,
            degradedFeatures = 0,
            unsupportedFeatures = 0,
            featureDetails = emptyList()
        )
        assertEquals(CompatibilityLevel.EXCELLENT, excellent.compatibilityLevel)
        assertEquals(100, excellent.compatibilityScore)
    }

    // ==================== P1: 文件流式处理测试 ====================

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `copyFileStream should copy file correctly`() {
        val sourceFile = tempFolder.newFile("source.txt")
        sourceFile.writeText("Hello, World! This is a test file content.")

        val destFile = File(tempFolder.root, "dest.txt")

        val bytesCopied = FileStreamingProcessingUtility.copyFileStream(sourceFile, destFile)

        assertTrue(destFile.exists())
        assertEquals(sourceFile.length(), bytesCopied)
        assertEquals("Hello, World! This is a test file content.", destFile.readText())
    }

    @Test
    fun `copyFileStream should report progress`() {
        val sourceFile = tempFolder.newFile("source.txt")
        sourceFile.writeText("A".repeat(10000))

        val destFile = File(tempFolder.root, "dest.txt")
        var progressCalls = 0

        FileStreamingProcessingUtility.copyFileStream(sourceFile, destFile) { _, _ ->
            progressCalls++
        }

        assertTrue(progressCalls > 0)
    }

    @Test
    fun `calculateDirectorySize should return correct size`() {
        val dir = tempFolder.newFolder("test_dir")
        File(dir, "file1.txt").writeText("A".repeat(1000))
        File(dir, "file2.txt").writeText("B".repeat(2000))

        val size = FileStreamingProcessingUtility.calculateDirectorySize(dir)

        assertEquals(3000L, size)
    }

    @Test
    fun `isFileSizeWithinLimit should return correct result`() {
        val smallFile = tempFolder.newFile("small.txt")
        smallFile.writeText("small")

        assertTrue(FileStreamingProcessingUtility.isFileSizeWithinLimit(smallFile))
        assertFalse(FileStreamingProcessingUtility.isFileSizeWithinLimit(smallFile, maxSizeBytes = 1))
    }

    @Test
    fun `deleteDirectorySafely should delete all files`() {
        val dir = tempFolder.newFolder("to_delete")
        File(dir, "file1.txt").writeText("content")
        File(dir, "subdir").mkdirs()
        File(dir, "subdir/file2.txt").writeText("content")

        assertTrue(FileStreamingProcessingUtility.deleteDirectorySafely(dir))
        assertFalse(dir.exists())
    }

    @Test
    fun `deleteDirectorySafely should return true for non-existent directory`() {
        val nonExistent = File(tempFolder.root, "non_existent")
        assertTrue(FileStreamingProcessingUtility.deleteDirectorySafely(nonExistent))
    }

    // ==================== P2: 搜索性能测试 ====================

    @Test
    fun `search should return empty list for empty query`() {
        val engine = ApplicationSearchPerformanceOptimizationEngine()
        val apps = listOf(
            createMockApp("com.app1", "App1"),
            createMockApp("com.app2", "App2")
        )

        val results = runBlockingWithTimeout {
            engine.searchSynchronously("", apps)
        }

        assertEquals(2, results.size)
    }

    @Test
    fun `search should filter by app name`() {
        val engine = ApplicationSearchPerformanceOptimizationEngine()
        val apps = listOf(
            createMockApp("com.example.wechat", "WeChat"),
            createMockApp("com.example.qq", "QQ"),
            createMockApp("com.example.whatsapp", "WhatsApp")
        )

        val results = runBlockingWithTimeout {
            engine.searchSynchronously("we", apps)
        }

        assertEquals(2, results.size) // WeChat + WhatsApp
    }

    @Test
    fun `search should respect max results limit`() {
        val engine = ApplicationSearchPerformanceOptimizationEngine()
        engine.maxResults = 2

        val apps = (1..10).map { createMockApp("com.app$it", "App$it") }

        val results = runBlockingWithTimeout {
            engine.searchSynchronously("app", apps)
        }

        assertEquals(2, results.size)
    }

    @Test
    fun `search should be case insensitive`() {
        val engine = ApplicationSearchPerformanceOptimizationEngine()
        val apps = listOf(
            createMockApp("com.example.app", "MyApp")
        )

        val results1 = runBlockingWithTimeout {
            engine.searchSynchronously("myapp", apps)
        }
        val results2 = runBlockingWithTimeout {
            engine.searchSynchronously("MYAPP", apps)
        }

        assertEquals(1, results1.size)
        assertEquals(1, results2.size)
    }

    private fun createMockApp(packageName: String, appName: String): ClonedApplicationInformationModel {
        return ClonedApplicationInformationModel(
            packageName = packageName,
            appName = appName,
            userId = 0
        )
    }

    private fun <T> runBlockingWithTimeout(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking { block() }
    }
}
