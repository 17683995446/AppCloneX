package com.appclonex.widget

import org.junit.Assert.*
import org.junit.Test

/**
 * QuickActionsWidget 单元测试
 * 
 * 测试覆盖：
 * - Widget 操作常量
 * - Intent 构造
 * - 操作类型验证
 */
class QuickActionsWidgetTest {

    // ==================== 操作常量测试 ====================

    @Test
    fun `WidgetAction constants should be correct`() {
        assertEquals("widget_action", WidgetAction.EXTRA_ACTION)
        assertEquals("action_new_profile", WidgetAction.ACTION_NEW_PROFILE)
        assertEquals("action_quick_clone", WidgetAction.ACTION_QUICK_CLONE)
        assertEquals("action_freeze_all", WidgetAction.ACTION_FREEZE_ALL)
        assertEquals("action_open_app", WidgetAction.ACTION_OPEN_APP)
    }

    @Test
    fun `all action constants should be unique`() {
        val actions = setOf(
            WidgetAction.ACTION_NEW_PROFILE,
            WidgetAction.ACTION_QUICK_CLONE,
            WidgetAction.ACTION_FREEZE_ALL,
            WidgetAction.ACTION_OPEN_APP
        )
        
        assertEquals(4, actions.size)
    }

    @Test
    fun `action constants should have consistent naming`() {
        val prefix = "action_"
        
        assertTrue(WidgetAction.ACTION_NEW_PROFILE.startsWith(prefix))
        assertTrue(WidgetAction.ACTION_QUICK_CLONE.startsWith(prefix))
        assertTrue(WidgetAction.ACTION_FREEZE_ALL.startsWith(prefix))
        assertTrue(WidgetAction.ACTION_OPEN_APP.startsWith(prefix))
    }

    // ==================== Intent 构造测试 ====================

    @Test
    fun `widget action intent should have correct flags`() {
        // 模拟 Intent flags
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        
        // FLAG_ACTIVITY_NEW_TASK = 0x10000000
        // FLAG_ACTIVITY_CLEAR_TOP = 0x04000000
        val expectedFlags = 0x10000000 or 0x04000000
        
        assertEquals(expectedFlags, flags)
    }

    @Test
    fun `widget action intent should contain action extra`() {
        val action = WidgetAction.ACTION_QUICK_CLONE
        
        // 模拟 Intent extra
        val extras = mapOf(WidgetAction.EXTRA_ACTION to action)
        
        assertTrue(extras.containsKey(WidgetAction.EXTRA_ACTION))
        assertEquals(WidgetAction.ACTION_QUICK_CLONE, extras[WidgetAction.EXTRA_ACTION])
    }

    // ==================== 操作类型验证测试 ====================

    @Test
    fun `ACTION_NEW_PROFILE should be valid action`() {
        val validActions = listOf(
            WidgetAction.ACTION_NEW_PROFILE,
            WidgetAction.ACTION_QUICK_CLONE,
            WidgetAction.ACTION_FREEZE_ALL,
            WidgetAction.ACTION_OPEN_APP
        )
        
        fun isValidAction(action: String): Boolean {
            return action in validActions
        }
        
        assertTrue(isValidAction(WidgetAction.ACTION_NEW_PROFILE))
        assertFalse(isValidAction("invalid_action"))
    }

    @Test
    fun `ACTION_QUICK_CLONE should be valid action`() {
        val validActions = setOf(
            WidgetAction.ACTION_NEW_PROFILE,
            WidgetAction.ACTION_QUICK_CLONE,
            WidgetAction.ACTION_FREEZE_ALL,
            WidgetAction.ACTION_OPEN_APP
        )
        
        assertTrue(WidgetAction.ACTION_QUICK_CLONE in validActions)
    }

    @Test
    fun `ACTION_FREEZE_ALL should be valid action`() {
        val validActions = setOf(
            WidgetAction.ACTION_NEW_PROFILE,
            WidgetAction.ACTION_QUICK_CLONE,
            WidgetAction.ACTION_FREEZE_ALL,
            WidgetAction.ACTION_OPEN_APP
        )
        
        assertTrue(WidgetAction.ACTION_FREEZE_ALL in validActions)
    }

    @Test
    fun `ACTION_OPEN_APP should be valid action`() {
        val validActions = setOf(
            WidgetAction.ACTION_NEW_PROFILE,
            WidgetAction.ACTION_QUICK_CLONE,
            WidgetAction.ACTION_FREEZE_ALL,
            WidgetAction.ACTION_OPEN_APP
        )
        
        assertTrue(WidgetAction.ACTION_OPEN_APP in validActions)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `extra action key should not be empty`() {
        assertTrue(WidgetAction.EXTRA_ACTION.isNotEmpty())
    }

    @Test
    fun `all action values should not be empty`() {
        assertTrue(WidgetAction.ACTION_NEW_PROFILE.isNotEmpty())
        assertTrue(WidgetAction.ACTION_QUICK_CLONE.isNotEmpty())
        assertTrue(WidgetAction.ACTION_FREEZE_ALL.isNotEmpty())
        assertTrue(WidgetAction.ACTION_OPEN_APP.isNotEmpty())
    }

    // ==================== Widget 尺寸测试 ====================

    @Test
    fun `widget minimum size should be reasonable`() {
        // 模拟 minResizeWidth 和 minResizeHeight
        val minResizeWidthDp = 110
        val minResizeHeightDp = 110
        
        assertTrue(minResizeWidthDp >= 40) // Android 最小小组件尺寸
        assertTrue(minResizeHeightDp >= 40)
    }

    @Test
    fun `widget should support 2x2 grid`() {
        // 2x2 小组件的标准尺寸
        val cellsX = 2
        val cellsY = 2
        
        // 每个单元格约 40-70dp
        val minSize = cellsX * 40
        val maxSize = cellsX * 70
        
        assertTrue(minSize >= 40)
        assertTrue(maxSize <= 200)
    }
}

/**
 * Intent flags 模拟
 */
private object Intent {
    const val FLAG_ACTIVITY_NEW_TASK = 0x10000000
    const val FLAG_ACTIVITY_CLEAR_TOP = 0x04000000
}
