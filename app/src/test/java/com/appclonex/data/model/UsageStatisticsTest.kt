package com.appclonex.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * UsageStatistics 及相关数据模型单元测试
 */
class UsageStatisticsTest {

    // ==================== UsageStatistics ====================

    @Test
    fun `UsageStatistics 默认值全部为 0, null 或 empty`() {
        val stats = UsageStatistics()

        assertThat(stats.totalProfiles).isEqualTo(0)
        assertThat(stats.activeProfiles).isEqualTo(0)
        assertThat(stats.totalClonedApps).isEqualTo(0)
        assertThat(stats.totalLaunches).isEqualTo(0)
        assertThat(stats.totalFrozenApps).isEqualTo(0)
        assertThat(stats.mostUsedApp).isNull()
        assertThat(stats.mostUsedAppLaunches).isEqualTo(0)
        assertThat(stats.topUsedApps).isEmpty()
        assertThat(stats.recentActivity).isEmpty()
        assertThat(stats.storageUsedBytes).isEqualTo(0L)
    }

    @Test
    fun `UsageStatistics 非零值创建`() {
        val topApps = listOf(
            TopUsedApp(packageName = "com.app1", appName = "App1", launchCount = 10),
            TopUsedApp(packageName = "com.app2", appName = "App2", launchCount = 5)
        )
        val recentActivity = listOf(
            ActivityRecord(
                packageName = "com.app1",
                appName = "App1",
                action = "launch",
                timestamp = 1000L
            )
        )

        val stats = UsageStatistics(
            totalProfiles = 3,
            activeProfiles = 2,
            totalClonedApps = 10,
            totalLaunches = 50,
            totalFrozenApps = 3,
            mostUsedApp = "App1",
            mostUsedAppLaunches = 10,
            topUsedApps = topApps,
            recentActivity = recentActivity,
            storageUsedBytes = 1024L * 1024 * 500
        )

        assertThat(stats.totalProfiles).isEqualTo(3)
        assertThat(stats.activeProfiles).isEqualTo(2)
        assertThat(stats.totalClonedApps).isEqualTo(10)
        assertThat(stats.totalLaunches).isEqualTo(50)
        assertThat(stats.totalFrozenApps).isEqualTo(3)
        assertThat(stats.mostUsedApp).isEqualTo("App1")
        assertThat(stats.mostUsedAppLaunches).isEqualTo(10)
        assertThat(stats.topUsedApps).hasSize(2)
        assertThat(stats.recentActivity).hasSize(1)
        assertThat(stats.storageUsedBytes).isEqualTo(1024L * 1024 * 500)
    }

    @Test
    fun `UsageStatistics copy 保持不变值`() {
        val original = UsageStatistics(
            totalProfiles = 5,
            activeProfiles = 3,
            totalClonedApps = 20
        )
        val copied = original.copy(totalProfiles = 10)

        assertThat(copied.totalProfiles).isEqualTo(10)
        assertThat(copied.activeProfiles).isEqualTo(3)
        assertThat(copied.totalClonedApps).isEqualTo(20)
    }

    // ==================== ActivityRecord getActionLabel ====================

    @Test
    fun `getActionLabel 返回 launch 对应的中文标签`() {
        val record = ActivityRecord("com.app", "App", "launch", 0L)
        assertThat(record.getActionLabel()).isEqualTo("启动了")
    }

    @Test
    fun `getActionLabel 返回 clone 对应的中文标签`() {
        val record = ActivityRecord("com.app", "App", "clone", 0L)
        assertThat(record.getActionLabel()).isEqualTo("克隆了")
    }

    @Test
    fun `getActionLabel 返回 freeze 对应的中文标签`() {
        val record = ActivityRecord("com.app", "App", "freeze", 0L)
        assertThat(record.getActionLabel()).isEqualTo("冻结了")
    }

    @Test
    fun `getActionLabel 返回 unfreeze 对应的中文标签`() {
        val record = ActivityRecord("com.app", "App", "unfreeze", 0L)
        assertThat(record.getActionLabel()).isEqualTo("解冻了")
    }

    @Test
    fun `getActionLabel 返回 delete 对应的中文标签`() {
        val record = ActivityRecord("com.app", "App", "delete", 0L)
        assertThat(record.getActionLabel()).isEqualTo("删除了")
    }

    @Test
    fun `getActionLabel 对 unknown action 返回原始 action 字符串`() {
        val record = ActivityRecord("com.app", "App", "unknown", 0L)
        assertThat(record.getActionLabel()).isEqualTo("unknown")
    }

    @Test
    fun `getActionLabel 对任意未知字符串返回原始值`() {
        val record = ActivityRecord("com.app", "App", "custom_action", 0L)
        assertThat(record.getActionLabel()).isEqualTo("custom_action")
    }

    // ==================== ActivityRecord getActionIconName ====================

    @Test
    fun `getActionIconName 返回 launch 对应的图标名称`() {
        val record = ActivityRecord("com.app", "App", "launch", 0L)
        assertThat(record.getActionIconName()).isEqualTo("TouchApp")
    }

    @Test
    fun `getActionIconName 返回 clone 对应的图标名称`() {
        val record = ActivityRecord("com.app", "App", "clone", 0L)
        assertThat(record.getActionIconName()).isEqualTo("ContentCopy")
    }

    @Test
    fun `getActionIconName 返回 freeze 对应的图标名称`() {
        val record = ActivityRecord("com.app", "App", "freeze", 0L)
        assertThat(record.getActionIconName()).isEqualTo("AcUnit")
    }

    @Test
    fun `getActionIconName 返回 unfreeze 对应的图标名称`() {
        val record = ActivityRecord("com.app", "App", "unfreeze", 0L)
        assertThat(record.getActionIconName()).isEqualTo("PlayArrow")
    }

    @Test
    fun `getActionIconName 返回 delete 对应的图标名称`() {
        val record = ActivityRecord("com.app", "App", "delete", 0L)
        assertThat(record.getActionIconName()).isEqualTo("Delete")
    }

    @Test
    fun `getActionIconName 对 unknown action 返回 Info`() {
        val record = ActivityRecord("com.app", "App", "unknown", 0L)
        assertThat(record.getActionIconName()).isEqualTo("Info")
    }

    @Test
    fun `getActionIconName 对任意未知字符串返回 Info`() {
        val record = ActivityRecord("com.app", "App", "random", 0L)
        assertThat(record.getActionIconName()).isEqualTo("Info")
    }

    // ==================== TopUsedApp ====================

    @Test
    fun `TopUsedApp 数据类创建`() {
        val app = TopUsedApp(
            packageName = "com.example.app",
            appName = "ExampleApp",
            launchCount = 42
        )

        assertThat(app.packageName).isEqualTo("com.example.app")
        assertThat(app.appName).isEqualTo("ExampleApp")
        assertThat(app.launchCount).isEqualTo(42)
    }

    @Test
    fun `TopUsedApp copy 保持不变值`() {
        val original = TopUsedApp("com.app", "App", 10)
        val copied = original.copy(launchCount = 20)

        assertThat(copied.packageName).isEqualTo("com.app")
        assertThat(copied.appName).isEqualTo("App")
        assertThat(copied.launchCount).isEqualTo(20)
    }

    @Test
    fun `TopUsedApp data class equals 正确比较`() {
        val app1 = TopUsedApp("com.app", "App", 10)
        val app2 = TopUsedApp("com.app", "App", 10)
        val app3 = TopUsedApp("com.app", "App", 20)

        assertThat(app1).isEqualTo(app2)
        assertThat(app1).isNotEqualTo(app3)
    }
}
