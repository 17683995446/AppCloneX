package com.appclonex.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloneOperationNotificationStateTest {

    @Test
    fun `default state has correct initial values`() {
        val state = CloneOperationNotificationState()
        assertFalse(state.isRunning)
        assertFalse(state.isCompleted)
        assertFalse(state.isSuccess)
        assertEquals("", state.operationType)
        assertEquals("", state.packageName)
        assertEquals(0, state.currentProgress)
        assertEquals(0, state.totalProgress)
        assertEquals("", state.progressText)
        assertEquals("", state.statusMessage)
    }

    @Test
    fun `state copy preserves immutability`() {
        val state1 = CloneOperationNotificationState()
        val state2 = state1.copy(isRunning = true, operationType = "clone")

        assertFalse(state1.isRunning)
        assertTrue(state2.isRunning)
        assertEquals("clone", state2.operationType)
        assertEquals("", state1.operationType)
    }

    @Test
    fun `state copy with progress updates`() {
        val state = CloneOperationNotificationState(
            isRunning = true,
            operationType = "batch_freeze",
            currentProgress = 5,
            totalProgress = 10
        )
        assertEquals("5 / 10", state.progressText)
    }

    @Test
    fun `completed state has correct flags`() {
        val state = CloneOperationNotificationState(
            isCompleted = true,
            isSuccess = true,
            statusMessage = "操作完成"
        )
        assertTrue(state.isCompleted)
        assertTrue(state.isSuccess)
        assertEquals("操作完成", state.statusMessage)
    }
}
