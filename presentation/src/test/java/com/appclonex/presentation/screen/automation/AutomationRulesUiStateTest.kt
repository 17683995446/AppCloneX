package com.appclonex.presentation.screen.automation

import com.appclonex.domain.model.AutomationRule
import com.appclonex.domain.model.AutoFreezeConfig
import org.junit.Assert.*
import org.junit.Test

class AutomationRulesUiStateTest {
    @Test
    fun `default state values`() {
        val state = AutomationRulesUiState()
        assertFalse(state.isLoading)
        assertTrue(state.rules.isEmpty())
        assertFalse(state.autoFreezeConfig.isEnabled)
        assertTrue(state.availableApps.isEmpty())
        assertFalse(state.isCreatingRule)
        assertFalse(state.showCreateDialog)
    }

    @Test
    fun `state with rules`() {
        val rule = AutomationRule(
            id = "1", name = "Test",
            ruleType = AutomationRule.RuleType.AUTO_FREEZE,
            triggerType = AutomationRule.TriggerType.SCREEN_OFF
        )
        val state = AutomationRulesUiState(
            rules = listOf(rule),
            autoFreezeConfig = AutoFreezeConfig(isEnabled = true)
        )
        assertEquals(1, state.rules.size)
        assertTrue(state.autoFreezeConfig.isEnabled)
    }

    @Test
    fun `state copy works`() {
        val state1 = AutomationRulesUiState()
        val state2 = state1.copy(showCreateDialog = true)
        assertFalse(state1.showCreateDialog)
        assertTrue(state2.showCreateDialog)
    }
}
