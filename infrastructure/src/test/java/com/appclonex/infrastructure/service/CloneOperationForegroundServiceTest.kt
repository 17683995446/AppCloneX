package com.appclonex.infrastructure.service

import org.junit.Assert.assertEquals
import org.junit.Test

class CloneOperationForegroundServiceTest {

    @Test
    fun `operation type constants are correct`() {
        assertEquals("clone", CloneOperationForegroundService.OP_CLONE)
        assertEquals("freeze", CloneOperationForegroundService.OP_FREEZE)
        assertEquals("unfreeze", CloneOperationForegroundService.OP_UNFREEZE)
        assertEquals("delete", CloneOperationForegroundService.OP_DELETE)
        assertEquals("batch_freeze", CloneOperationForegroundService.OP_BATCH_FREEZE)
        assertEquals("batch_unfreeze", CloneOperationForegroundService.OP_BATCH_UNFREEZE)
    }

    @Test
    fun `action constants are correct`() {
        assertEquals("com.appclonex.action.START_CLONE_SERVICE",
            CloneOperationForegroundService.ACTION_START)
        assertEquals("com.appclonex.action.STOP_CLONE_SERVICE",
            CloneOperationForegroundService.ACTION_STOP)
    }

    @Test
    fun `CloneOperationUiState default values`() {
        val state = CloneOperationUiState()
        assertEquals(false, state.isRunning)
        assertEquals(false, state.isCompleted)
        assertEquals(false, state.isSuccess)
        assertEquals("", state.operationType)
        assertEquals("", state.packageName)
        assertEquals(0, state.currentProgress)
        assertEquals(0, state.totalProgress)
        assertEquals("", state.progressText)
        assertEquals("", state.statusMessage)
    }

    @Test
    fun `CloneOperationUiState copy works correctly`() {
        val state = CloneOperationUiState(
            isRunning = true,
            operationType = "clone",
            packageName = "com.test.app",
            currentProgress = 3,
            totalProgress = 5
        )
        assertEquals("3 / 5", state.progressText)

        val updated = state.copy(
            currentProgress = 5,
            isCompleted = true,
            isSuccess = true
        )
        assertEquals("5 / 5", updated.progressText)
        assertEquals(true, updated.isCompleted)
    }
}
