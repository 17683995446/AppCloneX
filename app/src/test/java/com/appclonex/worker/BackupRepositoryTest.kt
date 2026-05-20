package com.appclonex.worker

import com.appclonex.domain.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * 备份数据模型单元测试
 */
class BackupDataModelTest {

    @Test
    fun `BackupData default values are correct`() {
        val backup = BackupData()
        assertEquals(2, backup.version)
        assertTrue(backup.timestamp > 0)
        assertTrue(backup.profiles.isEmpty())
        assertTrue(backup.settings.isEmpty())
    }

    @Test
    fun `ProfileBackupData can be created with all fields`() {
        val profile = ProfileBackupData(
            userId = 10,
            profileName = "Work",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isActive = true,
            serialNumber = 1,
            clonedApps = listOf(
                ClonedAppBackupData(
                    packageName = "com.example.app",
                    appName = "Example App"
                )
            )
        )
        assertEquals(10, profile.userId)
        assertEquals("Work", profile.profileName)
        assertEquals(1, profile.clonedApps.size)
    }

    @Test
    fun `BackupMetadata fields are correct`() {
        val meta = BackupMetadata(
            id = "backup_001",
            fileName = "backup_001.json",
            fileSize = 1024,
            profileCount = 2,
            appCount = 10
        )
        assertEquals("backup_001", meta.id)
        assertEquals(1024, meta.fileSize)
    }
}

/**
 * 冻结策略模型单元测试
 */
class FreezeStrategyModelTest {

    @Test
    fun `FreezeStrategy default trigger is MANUAL`() {
        val strategy = FreezeStrategy(name = "Test")
        assertEquals(FreezeTriggerType.MANUAL, strategy.triggerType)
        assertTrue(strategy.isEnabled)
    }

    @Test
    fun `FreezeSchedule default covers full day`() {
        val schedule = FreezeSchedule()
        assertEquals(0, schedule.startTimeHour)
        assertEquals(23, schedule.endTimeHour)
        assertEquals(7, schedule.repeatDays.size)
    }

    @Test
    fun `FreezeExecutionResult tracks counts`() {
        val result = FreezeExecutionResult(
            strategyId = 1,
            strategyName = "Test",
            frozenCount = 5,
            unfrozenCount = 2,
            failedApps = listOf("com.failed.app")
        )
        assertEquals(5, result.frozenCount)
        assertEquals(1, result.failedApps.size)
    }
}

/**
 * 使用统计模型单元测试
 */
class UsageStatisticsModelTest {

    @Test
    fun `AppUsageStatistics default values`() {
        val stats = AppUsageStatistics(packageName = "com.test.app")
        assertEquals(0, stats.totalLaunchCount)
        assertEquals(0L, stats.totalUsageTimeMs)
        assertTrue(stats.dailyUsage.isEmpty())
    }

    @Test
    fun `UsageOverview default values`() {
        val overview = UsageOverview()
        assertEquals(0, overview.totalClonedApps)
        assertEquals(0, overview.totalLaunchesToday)
        assertTrue(overview.mostUsedApps.isEmpty())
    }

    @Test
    fun `StatisticsPeriod has correct values`() {
        assertEquals(5, StatisticsPeriod.values().size)
        assertNotNull(StatisticsPeriod.TODAY)
        assertNotNull(StatisticsPeriod.CUSTOM)
    }
}

/**
 * 批量克隆模型单元测试
 */
class BatchCloneModelTest {

    @Test
    fun `BatchCloneConfig default options`() {
        val config = BatchCloneConfig(targetProfileId = 1)
        assertTrue(config.options.skipExisting)
        assertFalse(config.options.autoFreeze)
        assertTrue(config.options.parallelClone)
        assertEquals(3, config.options.maxParallel)
    }

    @Test
    fun `BatchCloneProgress tracks state`() {
        val progress = BatchCloneProgress(
            totalApps = 10,
            completedApps = 5,
            failedApps = 1,
            currentStatus = CloneStatus.CLONING
        )
        assertEquals(10, progress.totalApps)
        assertEquals(CloneStatus.CLONING, progress.currentStatus)
    }

    @Test
    fun `CloneStatus has all states`() {
        assertEquals(8, CloneStatus.values().size)
    }

    @Test
    fun `BatchCloneSummary calculates correctly`() {
        val summary = BatchCloneSummary(
            totalApps = 10,
            successCount = 8,
            failedCount = 1,
            skippedCount = 1
        )
        assertEquals(10, summary.totalApps)
        assertEquals(8, summary.successCount)
    }
}
