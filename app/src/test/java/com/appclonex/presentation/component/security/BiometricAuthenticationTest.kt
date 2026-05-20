package com.appclonex.presentation.component.security

import android.content.Context
import androidx.biometric.BiometricManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 生物识别认证单元测试
 * 
 * 测试覆盖：
 * - 生物识别可用性检查
 * - 生物识别状态获取
 * - 状态枚举验证
 */
class BiometricAuthenticationTest {

    private lateinit var context: Context
    private lateinit var biometricManager: BiometricManager
    private lateinit var helper: BiometricAuthenticationHelper

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        biometricManager = mockk(relaxed = true)
        
        mockkStatic(BiometricManager::class)
        every { BiometricManager.from(any()) } returns biometricManager
        
        helper = BiometricAuthenticationHelper(context)
    }

    // ==================== 生物识别可用性测试 ====================

    @Test
    fun `isBiometricAvailable should return true when biometric is available`() {
        every { 
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) 
        } returns BiometricManager.BIOMETRIC_SUCCESS

        assertTrue(helper.isBiometricAvailable())
    }

    @Test
    fun `isBiometricAvailable should return false when no hardware`() {
        every { 
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) 
        } returns BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE

        assertFalse(helper.isBiometricAvailable())
    }

    @Test
    fun `isBiometricAvailable should return false when hardware unavailable`() {
        every { 
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) 
        } returns BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE

        assertFalse(helper.isBiometricAvailable())
    }

    @Test
    fun `isBiometricAvailable should return false when not enrolled`() {
        every { 
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) 
        } returns BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED

        assertFalse(helper.isBiometricAvailable())
    }

    // ==================== 生物识别状态测试 ====================

    @Test
    fun `getBiometricStatus should return AVAILABLE when biometric is available`() {
        every { 
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) 
        } returns BiometricManager.BIOMETRIC_SUCCESS

        assertEquals(BiometricStatus.AVAILABLE, helper.getBiometricStatus())
    }

    @Test
    fun `getBiometricStatus should return NO_HARDWARE when no hardware`() {
        every { 
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) 
        } returns BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE

        assertEquals(BiometricStatus.NO_HARDWARE, helper.getBiometricStatus())
    }

    @Test
    fun `getBiometricStatus should return HARDWARE_UNAVAILABLE when hardware unavailable`() {
        every { 
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) 
        } returns BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE

        assertEquals(BiometricStatus.HARDWARE_UNAVAILABLE, helper.getBiometricStatus())
    }

    @Test
    fun `getBiometricStatus should return NOT_ENROLLED when not enrolled`() {
        every { 
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) 
        } returns BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED

        assertEquals(BiometricStatus.NOT_ENROLLED, helper.getBiometricStatus())
    }

    @Test
    fun `getBiometricStatus should return UNKNOWN for unknown error`() {
        every { 
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) 
        } returns BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED

        assertEquals(BiometricStatus.UNKNOWN, helper.getBiometricStatus())
    }

    // ==================== 状态枚举测试 ====================

    @Test
    fun `BiometricStatus enum should have all expected values`() {
        val expectedValues = listOf(
            BiometricStatus.AVAILABLE,
            BiometricStatus.NO_HARDWARE,
            BiometricStatus.HARDWARE_UNAVAILABLE,
            BiometricStatus.NOT_ENROLLED,
            BiometricStatus.UNKNOWN
        )

        assertEquals(5, BiometricStatus.values().size)
        expectedValues.forEach { status ->
            assertTrue(BiometricStatus.values().contains(status))
        }
    }

    @Test
    fun `BiometricStatus enum values should be accessible by name`() {
        assertEquals(BiometricStatus.AVAILABLE, BiometricStatus.valueOf("AVAILABLE"))
        assertEquals(BiometricStatus.NO_HARDWARE, BiometricStatus.valueOf("NO_HARDWARE"))
        assertEquals(BiometricStatus.HARDWARE_UNAVAILABLE, BiometricStatus.valueOf("HARDWARE_UNAVAILABLE"))
        assertEquals(BiometricStatus.NOT_ENROLLED, BiometricStatus.valueOf("NOT_ENROLLED"))
        assertEquals(BiometricStatus.UNKNOWN, BiometricStatus.valueOf("UNKNOWN"))
    }
}
