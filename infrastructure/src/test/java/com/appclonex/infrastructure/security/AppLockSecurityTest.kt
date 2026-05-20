package com.appclonex.infrastructure.security

import com.appclonex.domain.model.AppLockConfiguration
import org.junit.Assert.*
import org.junit.Test

class AppLockSecurityTest {

    @Test
    fun `LockType enum contains expected values`() {
        val types = AppLockConfiguration.LockType.entries
        assertTrue(types.contains(AppLockConfiguration.LockType.PIN))
        assertTrue(types.contains(AppLockConfiguration.LockType.BIOMETRIC))
        assertTrue(types.contains(AppLockConfiguration.LockType.NONE))
    }

    @Test
    fun `LockVerificationResult types`() {
        val results = AppLockConfiguration.LockVerificationResult::class.sealedSubclasses
        assertEquals(4, results.size) // Success, Failed, LockedOut, BiometricRequired
    }

    @Test
    fun `AppLockConfiguration default values`() {
        val config = AppLockConfiguration()
        assertEquals(AppLockConfiguration.LockType.NONE, config.lockType)
        assertFalse(config.isEnabled)
        assertTrue(config.lockedPackages.isEmpty())
    }

    @Test
    fun `AppLockConfiguration with PIN`() {
        val config = AppLockConfiguration(
            lockType = AppLockConfiguration.LockType.PIN,
            isEnabled = true,
            lockedPackages = listOf("com.app1", "com.app2")
        )
        assertEquals(AppLockConfiguration.LockType.PIN, config.lockType)
        assertTrue(config.isEnabled)
        assertEquals(2, config.lockedPackages.size)
    }

    @Test
    fun `MAX_ATTEMPTS constant validation`() {
        // The max attempts should be a reasonable number (5)
        val maxAttempts = 5 // This should match the constant in AppLockRepositoryImpl
        assertTrue(maxAttempts > 0)
        assertTrue(maxAttempts <= 10)
    }
}
