package com.appclonex.presentation.screen.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardUiStateTest {

    @Test
    fun `default DashboardUiState has isLoading true`() {
        val state = DashboardUiState()
        assertTrue(state.isLoading) // Dashboard defaults to true
        assertNull(state.message)
        assertNull(state.error)
    }

    @Test
    fun `DashboardUiState with data`() {
        val state = DashboardUiState(
            isLoading = false,
            totalProfiles = 3,
            activeProfiles = 2,
            totalClonedApps = 10,
            totalLaunches = 50,
            totalFrozenApps = 3,
            mostUsedApp = "com.example.app",
            mostUsedAppLaunches = 15,
            storageUsedBytes = 1024 * 1024 * 100L
        )
        assertEquals(3, state.totalProfiles)
        assertEquals(2, state.activeProfiles)
        assertEquals(10, state.totalClonedApps)
        assertEquals(50, state.totalLaunches)
        assertEquals(3, state.totalFrozenApps)
        assertEquals("com.example.app", state.mostUsedApp)
        assertEquals(15, state.mostUsedAppLaunches)
        assertEquals(104857600L, state.storageUsedBytes)
    }

    @Test
    fun `TopUsedApp data class`() {
        val app = TopUsedApp(
            packageName = "com.test",
            appName = "TestApp",
            launchCount = 20
        )
        assertEquals("com.test", app.packageName)
        assertEquals("TestApp", app.appName)
        assertEquals(20, app.launchCount)
    }

    @Test
    fun `ActivityRecord data class`() {
        val record = ActivityRecord(
            packageName = "com.test",
            appName = "TestApp",
            activityType = "LAUNCH",
            timestamp = System.currentTimeMillis()
        )
        assertEquals("LAUNCH", record.activityType)
    }
}
