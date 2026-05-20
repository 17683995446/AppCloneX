package com.appclonex.presentation.util

import org.junit.Assert.*
import org.junit.Test

class PermissionNameMapperTest {

    @Test
    fun `getDisplayName returns Chinese name for known permissions`() {
        assertEquals("相机", PermissionNameMapper.getDisplayName("android.permission.CAMERA"))
        assertEquals("麦克风", PermissionNameMapper.getDisplayName("android.permission.RECORD_AUDIO"))
        assertEquals("精确位置", PermissionNameMapper.getDisplayName("android.permission.ACCESS_FINE_LOCATION"))
        assertEquals("网络访问", PermissionNameMapper.getDisplayName("android.permission.INTERNET"))
    }

    @Test
    fun `getDisplayName returns last segment for unknown permissions`() {
        assertEquals("UNKNOWN_PERMISSION", PermissionNameMapper.getDisplayName("android.permission.UNKNOWN_PERMISSION"))
        assertEquals("CUSTOM", PermissionNameMapper.getDisplayName("com.example.permission.CUSTOM"))
    }

    @Test
    fun `getDisplayName handles edge cases`() {
        assertEquals("", PermissionNameMapper.getDisplayName(""))
        assertEquals("permission", PermissionNameMapper.getDisplayName("permission"))
    }

    @Test
    fun `getDescription returns description for known permissions`() {
        assertTrue(PermissionNameMapper.getDescription("android.permission.CAMERA").contains("拍摄"))
        assertTrue(PermissionNameMapper.getDescription("android.permission.RECORD_AUDIO").contains("录制"))
        assertTrue(PermissionNameMapper.getDescription("android.permission.INTERNET").contains("网络"))
    }

    @Test
    fun `getDescription returns default for unknown permissions`() {
        val desc = PermissionNameMapper.getDescription("android.permission.UNKNOWN")
        assertEquals("允许应用使用此权限", desc)
    }

    @Test
    fun `all mapped permissions have non-empty display names`() {
        val testPermissions = listOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.INTERNET"
        )
        
        for (permission in testPermissions) {
            val displayName = PermissionNameMapper.getDisplayName(permission)
            assertTrue("Permission $permission should have non-empty display name", displayName.isNotEmpty())
            assertNotEquals(permission, displayName) // Should be translated, not the same
        }
    }
}
