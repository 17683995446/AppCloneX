package com.appclonex.v23

import com.appclonex.domain.model.*
import com.appclonex.domain.usecase.statistics.ExportFormat
import org.junit.Assert.*
import org.junit.Test

class CloneTemplateTests {
    @Test fun `built-in templates count is 4`() = assertEquals(4, BuiltInTemplates.all.size)
    @Test fun `work profile has correct category`() = assertEquals(TemplateCategory.WORK, BuiltInTemplates.workProfile.category)
    @Test fun `social clone has packages`() = assertTrue(BuiltInTemplates.socialClone.appPackages.isNotEmpty())
    @Test fun `custom template default values`() {
        val t = CloneTemplate(name = "Custom")
        assertFalse(t.isBuiltIn)
        assertEquals(0, t.usageCount)
    }
    @Test fun `template categories count`() = assertEquals(6, TemplateCategory.values().size)
}

class CloudBackupTests {
    @Test fun `cloud providers count`() = assertEquals(5, CloudProvider.values().size)
    @Test fun `default config is local`() = assertEquals(CloudProvider.LOCAL, CloudBackupConfig().provider)
    @Test fun `sync results count`() = assertEquals(4, SyncResult.values().size)
    @Test fun `cloud backup info defaults`() {
        val info = CloudBackupInfo()
        assertFalse(info.isAvailable)
        assertEquals(0L, info.fileSize)
    }
}

class PerformanceBenchmarkTests {
    @Test fun `benchmark ratings count`() = assertEquals(6, BenchmarkRating.values().size)
    @Test fun `benchmark result defaults`() {
        val r = BenchmarkResult(testName = "test")
        assertEquals(BenchmarkRating.UNKNOWN, r.rating)
        assertEquals(0f, r.score, 0.001f)
    }
    @Test fun `benchmark suite defaults`() {
        val s = BenchmarkSuite(id = "suite1")
        assertTrue(s.results.isEmpty())
        assertEquals(0f, s.overallScore, 0.001f)
    }
    @Test fun `performance metrics defaults`() {
        val m = PerformanceMetrics()
        assertEquals(0L, m.coldStartMs)
        assertEquals(0f, m.memoryUsageMb, 0.001f)
    }
}

class AccessibilityTests {
    @Test fun `font size scales count`() = assertEquals(4, FontSizeScale.values().size)
    @Test fun `default font scale`() = assertEquals(1f, FontSizeScale.DEFAULT.scale, 0.001f)
    @Test fun `accessibility settings defaults`() {
        val s = AccessibilitySettings()
        assertEquals(1f, s.fontSizeScale, 0.001f)
        assertFalse(s.highContrast)
        assertFalse(s.reduceMotion)
        assertEquals(48, s.touchTargetSize)
    }
    @Test fun `font scale labels`() {
        assertEquals("小", FontSizeScale.SMALL.label)
        assertEquals("特大", FontSizeScale.EXTRA_LARGE.label)
    }
}

class ExportStatisticsTests {
    @Test fun `export formats count`() = assertEquals(2, ExportFormat.values().size)
    @Test fun `export format values`() {
        assertNotNull(ExportFormat.CSV)
        assertNotNull(ExportFormat.JSON)
    }
}
