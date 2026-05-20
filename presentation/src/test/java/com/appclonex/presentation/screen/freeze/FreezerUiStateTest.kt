package com.appclonex.presentation.screen.freeze

import com.appclonex.presentation.base.BaseUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreezerUiStateTest {

    @Test
    fun `default FreezerUiState values`() {
        val state = FreezerUiState()
        assertFalse(state.isLoading)
        assertEquals(null, state.message)
        assertEquals(null, state.error)
        assertTrue(state.clonedApps.isEmpty())
        assertTrue(state.frozenApps.isEmpty())
        assertEquals(0, state.frozenCount)
        assertEquals(0, state.totalCount)
        assertEquals(0, state.currentUserId)
        assertFalse(state.isProcessing)
    }

    @Test
    fun `FreezerUiState copy with frozen apps`() {
        val state = FreezerUiState(
            frozenApps = mapOf("com.app1" to true, "com.app2" to false),
            frozenCount = 1,
            totalCount = 2,
            isProcessing = false
        )
        assertEquals(1, state.frozenCount)
        assertEquals(2, state.totalCount)
        assertEquals(2, state.frozenApps.size)
    }

    @Test
    fun `FreezerUiState implements BaseUiState`() {
        val state: BaseUiState = FreezerUiState()
        assertFalse(state.isLoading)
        assertEquals(null, state.message)
        assertEquals(null, state.error)
    }
}
