package com.appclonex.presentation.screen.sandbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SandboxUiStateTest {

    @Test
    fun `default SandboxUiState values`() {
        val state = SandboxUiState()
        assertFalse(state.isLoading)
        assertNull(state.message)
        assertNull(state.error)
        assertTrue(state.sandboxes.isEmpty())
        assertNull(state.selectedSandbox)
        assertTrue(state.installedApps.isEmpty())
    }

    @Test
    fun `SandboxInfo data class works correctly`() {
        val info = SandboxInfo(
            userId = 10,
            name = "Test Sandbox",
            appCount = 5,
            isActive = true
        )
        assertEquals(10, info.userId)
        assertEquals("Test Sandbox", info.name)
        assertEquals(5, info.appCount)
        assertTrue(info.isActive)
    }

    @Test
    fun `SandboxUiState copy works`() {
        val info = SandboxInfo(userId = 1, name = "S1", appCount = 3)
        val state = SandboxUiState(
            sandboxes = listOf(info),
            selectedSandbox = info,
            installedApps = listOf("com.app1", "com.app2")
        )
        assertEquals(1, state.sandboxes.size)
        assertEquals(info, state.selectedSandbox)
        assertEquals(2, state.installedApps.size)
    }
}
