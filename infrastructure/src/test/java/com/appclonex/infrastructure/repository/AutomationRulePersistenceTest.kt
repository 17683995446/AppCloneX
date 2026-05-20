package com.appclonex.infrastructure.repository

import com.appclonex.domain.model.AutomationRule
import com.appclonex.domain.model.AutoFreezeConfig
import org.junit.Assert.*
import org.junit.Test

class AutomationRulePersistenceTest {

    @Test
    fun `AutomationRule is serializable`() {
        val rule = AutomationRule(
            id = "rule_1",
            name = "Auto Freeze Night",
            ruleType = AutomationRule.RuleType.AUTO_FREEZE,
            triggerType = AutomationRule.TriggerType.TIME_BASED,
            targetPackageNames = listOf("com.app1", "com.app2"),
            isEnabled = true,
            scheduleTime = "23:00"
        )
        assertEquals("rule_1", rule.id)
        assertEquals(AutomationRule.RuleType.AUTO_FREEZE, rule.ruleType)
        assertEquals(AutomationRule.TriggerType.TIME_BASED, rule.triggerType)
        assertEquals(2, rule.targetPackageNames.size)
        assertTrue(rule.isEnabled)
    }

    @Test
    fun `AutoFreezeConfig default values`() {
        val config = AutoFreezeConfig()
        assertFalse(config.isEnabled)
        assertEquals(15, config.freezeDelayMinutes)
        assertTrue(config.targetAllApps)
        assertTrue(config.excludeWhitelistedApps)
        assertTrue(config.whitelistedPackages.isEmpty())
    }

    @Test
    fun `AutoFreezeConfig custom values`() {
        val config = AutoFreezeConfig(
            isEnabled = true,
            freezeDelayMinutes = 30,
            targetAllApps = false,
            whitelistedPackages = listOf("com.system.app")
        )
        assertTrue(config.isEnabled)
        assertEquals(30, config.freezeDelayMinutes)
        assertFalse(config.targetAllApps)
        assertEquals(1, config.whitelistedPackages.size)
    }

    @Test
    fun `AutomationRule copy works correctly`() {
        val rule1 = AutomationRule(
            id = "1",
            name = "Test",
            ruleType = AutomationRule.RuleType.AUTO_FREEZE,
            triggerType = AutomationRule.TriggerType.SCREEN_OFF
        )
        val rule2 = rule1.copy(isEnabled = true, name = "Updated")
        assertFalse(rule1.isEnabled)
        assertEquals("Test", rule1.name)
        assertTrue(rule2.isEnabled)
        assertEquals("Updated", rule2.name)
    }

    @Test
    fun `AutomationRule ActionType enum values`() {
        assertEquals(3, AutomationRule.ActionType.entries.size)
        assertTrue(AutomationRule.ActionType.entries.contains(AutomationRule.ActionType.FREEZE))
        assertTrue(AutomationRule.ActionType.entries.contains(AutomationRule.ActionType.UNFREEZE))
        assertTrue(AutomationRule.ActionType.entries.contains(AutomationRule.ActionType.SWITCH_PROFILE))
    }
}
