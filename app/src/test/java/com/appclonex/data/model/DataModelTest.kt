package com.appclonex.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * 数据模型单元测试
 */
class DataModelTest {

    // ==================== UserProfile 测试 ====================

    @Test
    fun `UserProfile creation with required fields`() {
        val profile = UserProfileModel(
            userId = 100,
            profileName = "工作空间"
        )

        assertThat(profile.userId).isEqualTo(100)
        assertThat(profile.profileName).isEqualTo("工作空间")
    }

    @Test
    fun `UserProfile default values are correct`() {
        val profile = UserProfileModel(
            userId = 100,
            profileName = "测试"
        )

        assertThat(profile.profileIcon).isNull()
        assertThat(profile.isActive).isFalse()
        assertThat(profile.isRunning).isFalse()
        assertThat(profile.serialNumber).isEqualTo(0)
        assertThat(profile.clonedAppCount).isEqualTo(0)
        assertThat(profile.storageUsed).isEqualTo(0L)
        assertThat(profile.notes).isNull()
    }

    @Test
    fun `UserProfile toString contains key information`() {
        val profile = UserProfileModel(
            userId = 100,
            profileName = "工作空间"
        )

        val str = profile.toString()
        assertThat(str).contains("100")
        assertThat(str).contains("工作空间")
    }

    @Test
    fun `UserProfile custom values override defaults`() {
        val profile = UserProfileModel(
            userId = 200,
            profileName = "游戏空间",
            profileIcon = "/path/icon.png",
            isActive = true,
            isRunning = true,
            serialNumber = 5,
            clonedAppCount = 10,
            storageUsed = 1024L * 1024 * 500,
            notes = "测试备注"
        )

        assertThat(profile.profileIcon).isEqualTo("/path/icon.png")
        assertThat(profile.isActive).isTrue()
        assertThat(profile.isRunning).isTrue()
        assertThat(profile.serialNumber).isEqualTo(5)
        assertThat(profile.clonedAppCount).isEqualTo(10)
        assertThat(profile.storageUsed).isEqualTo(1024L * 1024 * 500)
        assertThat(profile.notes).isEqualTo("测试备注")
    }

    @Test
    fun `UserProfile getFormattedStorage returns correct format`() {
        val profileMB = UserProfileModel(
            userId = 1,
            profileName = "Test",
            storageUsed = 500L * 1024 * 1024
        )
        assertThat(profileMB.getFormattedStorage()).isEqualTo("500 MB")

        val profileGB = UserProfileModel(
            userId = 1,
            profileName = "Test",
            storageUsed = 2L * 1024 * 1024 * 1024
        )
        assertThat(profileGB.getFormattedStorage()).isEqualTo("2.0 GB")
    }

    // ==================== ClonedApplicationInformationModel 测试 ====================

    @Test
    fun `ClonedApplicationInformationModel creation with required fields`() {
        val app = ClonedApplicationInformationModel(
            packageName = "com.example.app",
            appName = "示例应用",
            profileUserId = 100
        )

        assertThat(app.packageName).isEqualTo("com.example.app")
        assertThat(app.appName).isEqualTo("示例应用")
        assertThat(app.profileUserId).isEqualTo(100)
    }

    @Test
    fun `ClonedApplicationInformationModel default values are correct`() {
        val app = ClonedApplicationInformationModel(
            packageName = "com.example.app",
            appName = "示例应用",
            profileUserId = 100
        )

        assertThat(app.id).isEqualTo(0)
        assertThat(app.appIcon).isNull()
        assertThat(app.cloneIndex).isEqualTo(1)
        assertThat(app.versionName).isNull()
        assertThat(app.versionCode).isEqualTo(0)
        assertThat(app.sourceDir).isNull()
        assertThat(app.dataDir).isNull()
        assertThat(app.isEnabled).isTrue()
        assertThat(app.isRunning).isFalse()
        assertThat(app.lastLaunchTime).isEqualTo(0)
        assertThat(app.launchCount).isEqualTo(0)
        assertThat(app.customName).isNull()
        assertThat(app.customIcon).isNull()
        assertThat(app.size).isEqualTo(0L)
        assertThat(app.notes).isNull()
    }

    @Test
    fun `ClonedApplicationInformationModel getDisplayName returns appName when customName is null`() {
        val app = ClonedApplicationInformationModel(
            packageName = "com.example.app",
            appName = "示例应用",
            profileUserId = 100
        )

        assertThat(app.getDisplayName()).isEqualTo("示例应用")
    }

    @Test
    fun `ClonedApplicationInformationModel getDisplayName returns customName when set`() {
        val app = ClonedApplicationInformationModel(
            packageName = "com.example.app",
            appName = "示例应用",
            profileUserId = 100,
            customName = "自定义名称"
        )

        assertThat(app.getDisplayName()).isEqualTo("自定义名称")
    }

    @Test
    fun `ClonedApplicationInformationModel getFormattedSize returns correct format`() {
        val appMB = ClonedApplicationInformationModel(
            packageName = "com.example.app",
            appName = "Test",
            profileUserId = 100,
            size = 50L * 1024 * 1024
        )
        assertThat(appMB.getFormattedSize()).isEqualTo("50.0 MB")

        val appGB = ClonedApplicationInformationModel(
            packageName = "com.example.app",
            appName = "Test",
            profileUserId = 100,
            size = 2L * 1024 * 1024 * 1024
        )
        assertThat(appGB.getFormattedSize()).isEqualTo("2.0 GB")
    }
}
