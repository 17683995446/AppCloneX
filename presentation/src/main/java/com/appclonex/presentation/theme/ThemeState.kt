package com.appclonex.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration

val LocalAppThemeOverrides = staticCompositionLocalOf { ThemeOverrides() }

data class ThemeOverrides(
    val darkTheme: Boolean? = null // null = follow system
)

@Composable
fun rememberAppThemeState(): ThemeOverrides {
    // This will be overridden by the ViewModel when the user changes the setting
    return ThemeOverrides()
}
