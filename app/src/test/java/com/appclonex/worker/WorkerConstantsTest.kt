package com.appclonex.worker

import org.junit.Assert.*
import org.junit.Test

class WorkerConstantsTest {
    @Test
    fun `AppStateSyncWorker has correct work name`() {
        assertEquals("app_state_sync", ApplicationStateSynchronizationWorker.WORK_NAME)
    }

    @Test
    fun `ConfigAutoBackupWorker has correct work name`() {
        assertEquals("config_auto_backup", ConfigurationAutoBackupWorker.WORK_NAME)
    }

    @Test
    fun `AutoFreezeWorker has correct work name`() {
        assertEquals("auto_freeze", AutoFreezeWorker.WORK_NAME)
    }

    @Test
    fun `StorageCleanupWorker has correct work name`() {
        assertEquals("storage_cleanup", StorageCleanupWorker.WORK_NAME)
    }
}
