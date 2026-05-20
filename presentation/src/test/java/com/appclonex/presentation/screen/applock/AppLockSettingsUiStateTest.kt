package com.appclonex.presentation.screen.applock

import com.appclonex.domain.model.AppLockConfiguration
import org.junit.Assert.*
import org.junit.Test

class AppLockSettingsUiStateTest {
    @Test
    fun `default state values`() {
        val state = AppLockSettingsUiState()
        assertFalse(state.isLoading)
        assertNull(state.message)
        assertNull(state.error)
        assertEquals(AppLockConfiguration.LockType.NONE, state.currentLockType)
        assertTrue(state.lockedApps.isEmpty())
        assertFalse(state.isSettingPin)
        assertFalse(state.isEnrollingBiometric)
    }

    @Test
    fun `state with PIN lock`() {
        val state = AppLockSettingsUiState(
            currentLockType = AppLockConfiguration.LockType.PIN,
            lockedApps = listOf("com.app1", "com.app2")
        )
        assertEquals(AppLockConfiguration.LockType.PIN, state.currentLockType)
        assertEquals(2, state.lockedApps.size)
    }

    @Test
    fun `state implements BaseUiState`() {
        val state: com.appclonex.presentation.base.BaseUiState = AppLockSettingsUiState()
        assertFalse(state.isLoading)
    }
}
