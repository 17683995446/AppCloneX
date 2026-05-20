package com.appclonex.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationTest {

    @Test
    fun `NavTarget routes are unique`() {
        val routes = setOf(
            NavTarget.Onboarding.route,
            NavTarget.Home.route,
            NavTarget.Apps.route,
            NavTarget.Profile.route,
            NavTarget.Settings.route,
            NavTarget.Freeze.route,
            NavTarget.Sandbox.route,
            NavTarget.Dashboard.route
        )
        assertEquals(8, routes.size)
    }

    @Test
    fun `AppDetail createRoute generates correct URL`() {
        val route = NavTarget.AppDetail.createRoute("com.example.app", 10)
        assertEquals("app_detail/com.example.app/10", route)
    }

    @Test
    fun `AppDetail createRoute uses default userId`() {
        val route = NavTarget.AppDetail.createRoute("com.example.app")
        assertEquals("app_detail/com.example.app/0", route)
    }

    @Test
    fun `AppDetail route pattern contains placeholders`() {
        val route = NavTarget.AppDetail.route
        assert(route.contains("{packageName}"))
        assert(route.contains("{userId}"))
    }

    @Test
    fun `Onboarding route is correct`() {
        assertEquals("onboarding", NavTarget.Onboarding.route)
    }

    @Test
    fun `Freeze route is correct`() {
        assertEquals("freeze", NavTarget.Freeze.route)
    }

    @Test
    fun `Sandbox route is correct`() {
        assertEquals("sandbox", NavTarget.Sandbox.route)
    }

    @Test
    fun `Dashboard route is correct`() {
        assertEquals("dashboard", NavTarget.Dashboard.route)
    }
}
