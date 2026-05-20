package com.appclonex.presentation.navigation

import org.junit.Assert.*
import org.junit.Test

class NavTargetTest {

    @Test
    fun `all nav targets have unique routes`() {
        val routes = listOf(
            NavTarget.Onboarding.route,
            NavTarget.Home.route,
            NavTarget.Apps.route,
            NavTarget.Profile.route,
            NavTarget.Settings.route,
            NavTarget.Freeze.route,
            NavTarget.Sandbox.route,
            NavTarget.Dashboard.route,
            NavTarget.AppDetail.route,
            NavTarget.AppLock.route,
            NavTarget.Automation.route
        )
        
        // AppDetail route contains placeholders, so we check the base pattern
        val uniqueRoutes = routes.filter { !it.contains("{") }.toSet()
        assertEquals(10, uniqueRoutes.size)
    }

    @Test
    fun `new routes added in V20`() {
        assertEquals("app_lock", NavTarget.AppLock.route)
        assertEquals("automation", NavTarget.Automation.route)
    }

    @Test
    fun `AppDetail route pattern is correct`() {
        val route = NavTarget.AppDetail.route
        assertTrue(route.contains("{packageName}"))
        assertTrue(route.contains("{userId}"))
    }

    @Test
    fun `AppDetail createRoute generates correct URL`() {
        val route = NavTarget.AppDetail.createRoute("com.test.app", 10)
        assertEquals("app_detail/com.test.app/10", route)
    }
}
