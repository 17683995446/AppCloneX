package com.appclonex.ui.viewmodels

import com.appclonex.ui.theme.ThemeMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * SettingsUiState 单元测试
 */
class SettingsUiStateTest {

    @Test
    fun `getFormattedLastBackupTime returns correct string when lastBackupTime is zero`() {
        val state = SettingsUiState(lastBackupTime = 0L)
        assertThat(state.getFormattedLastBackupTime()).isEqualTo("从未备份")
    }

    @Test
    fun `getFormattedLastBackupTime returns formatted string for valid timestamp`() {
        // 2024-06-15 14:30:00 UTC+8 = 1718430600000L
        val timestamp = 1718430600000L
        val state = SettingsUiState(lastBackupTime = timestamp)
        val formatted = state.getFormattedLastBackupTime()

        // Should contain date and time in yyyy-MM-dd HH:mm format
        assertThat(formatted).contains("2024-06-15")
        assertThat(formatted).contains("14:30")
    }

    @Test
    fun `SettingsUiState default values are correct`() {
        val state = SettingsUiState()

        assertThat(state.excludeSystemApps).isTrue()
        assertThat(state.showNotifications).isTrue()
        assertThat(state.autoStartProfile).isFalse()
        assertThat(state.darkTheme).isFalse()
        assertThat(state.themeMode).isEqualTo(ThemeMode.SYSTEM)
        assertThat(state.totalProfiles).isEqualTo(0)
        assertThat(state.totalClonedApps).isEqualTo(0)
        assertThat(state.lastBackupTime).isEqualTo(0L)
        assertThat(state.isBackupLoading).isFalse()
        assertThat(state.isRestoreLoading).isFalse()
        assertThat(state.message).isNull()
        assertThat(state.error).isNull()
    }

    @Test
    fun `SettingsUiState copy preserves unchanged values`() {
        val state = SettingsUiState(
            excludeSystemApps = false,
            totalProfiles = 5
        )
        val copied = state.copy(excludeSystemApps = true)

        assertThat(copied.excludeSystemApps).isTrue()
        assertThat(copied.totalProfiles).isEqualTo(5)
        assertThat(copied.showNotifications).isTrue() // default preserved
    }

    @Test
    fun `getFormattedLastBackupTime returns different strings for different timestamps`() {
        val state1 = SettingsUiState(lastBackupTime = 0L)
        val state2 = SettingsUiState(lastBackupTime = System.currentTimeMillis())

        assertThat(state1.getFormattedLastBackupTime()).isNotEqualTo(state2.getFormattedLastBackupTime())
    }
}
