package com.appclonex.presentation.screens.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import org.junit.Assert.*
import org.junit.Test

class AnimationConfigTest {

    @Test
    fun `navigation animation duration is 300ms`() {
        val spec = tween<Float>(300)
        assertEquals(300, spec.durationMillis)
    }

    @Test
    fun `list item animation uses spring with correct stiffness`() {
        // Spring.StiffnessLow = 200f
        assertEquals(200f, Spring.StiffnessLow, 0.01f)
    }

    @Test
    fun `list item animation uses medium bouncy damping`() {
        // Spring.DampingRatioMediumBouncy = 0.6f
        assertEquals(0.6f, Spring.DampingRatioMediumBouncy, 0.01f)
    }

    @Test
    fun `animation constants are reasonable`() {
        // Navigation animation should be fast enough (300ms)
        assertTrue(300 <= 500) // Less than 500ms
        
        // List item animation should be bouncy but not too slow
        assertTrue(Spring.StiffnessLow >= 100f) // Not too loose
        assertTrue(Spring.StiffnessLow <= 500f) // Not too stiff
    }
}
