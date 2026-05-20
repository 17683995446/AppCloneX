package com.appclonex.presentation.screen.appdetail

import com.appclonex.domain.model.ClonedApplicationInformationModel
import org.junit.Assert.*
import org.junit.Test

class AppDetailUiStateTest {

    @Test
    fun `default state has isLoading true`() {
        val state = AppDetailUiState()
        assertTrue(state.isLoading)
        assertNull(state.app)
        assertFalse(state.isFrozen)
        assertEquals("--", state.cacheSize)
        assertEquals("--", state.dataSize)
        assertEquals("--", state.totalSize)
        assertTrue(state.permissions.isEmpty())
        assertFalse(state.isProcessing)
    }

    @Test
    fun `state copy preserves immutability`() {
        val state1 = AppDetailUiState()
        val state2 = state1.copy(isLoading = false, isFrozen = true)
        assertTrue(state1.isLoading)
        assertFalse(state1.isFrozen)
        assertFalse(state2.isLoading)
        assertTrue(state2.isFrozen)
    }

    @Test
    fun `state with app data`() {
        val app = ClonedApplicationInformationModel(
            packageName = "com.test.app",
            appName = "TestApp",
            userId = 10
        )
        val state = AppDetailUiState(
            isLoading = false,
            app = app,
            isFrozen = true,
            cacheSize = "10.5 MB",
            dataSize = "25.3 MB",
            totalSize = "35.8 MB",
            permissions = listOf("INTERNET", "CAMERA"),
            isProcessing = false
        )
        assertEquals("com.test.app", state.app?.packageName)
        assertEquals("TestApp", state.app?.appName)
        assertTrue(state.isFrozen)
        assertEquals(2, state.permissions.size)
    }

    @Test
    fun `state implements BaseUiState`() {
        val state: com.appclonex.presentation.base.BaseUiState = AppDetailUiState()
        assertTrue(state.isLoading)
        assertNull(state.message)
        assertNull(state.error)
    }
}
