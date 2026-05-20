package com.appclonex.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.appclonex.domain.model.ThemeMode

// ============================================================
// 绿色系配色方案 - 适用于应用克隆工具
// ============================================================

// ---- Light Theme Colors ----
val md_theme_light_primary = Color(0xFF1B6B52)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFA4F2CE)
val md_theme_light_onPrimaryContainer = Color(0xFF002117)
val md_theme_light_secondary = Color(0xFF4B635C)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFCDE8DF)
val md_theme_light_onSecondaryContainer = Color(0xFF07201A)
val md_theme_light_tertiary = Color(0xFF3F6374)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFC3E8FB)
val md_theme_light_onTertiaryContainer = Color(0xFF001F29)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFFBFDF9)
val md_theme_light_onBackground = Color(0xFF191C1A)
val md_theme_light_surface = Color(0xFFFBFDF9)
val md_theme_light_onSurface = Color(0xFF191C1A)
val md_theme_light_surfaceVariant = Color(0xFFDBE5DF)
val md_theme_light_onSurfaceVariant = Color(0xFF3F4944)
val md_theme_light_outline = Color(0xFF6F7974)
val md_theme_light_outlineVariant = Color(0xFFBFC9C3)
val md_theme_light_inverseSurface = Color(0xFF2E3130)
val md_theme_light_inverseOnSurface = Color(0xFFF0F1EE)
val md_theme_light_inversePrimary = Color(0xFF89D6B2)
val md_theme_light_surfaceTint = Color(0xFF1B6B52)

// ---- Dark Theme Colors ----
val md_theme_dark_primary = Color(0xFF7EDBB5)
val md_theme_dark_onPrimary = Color(0xFF003827)
val md_theme_dark_primaryContainer = Color(0xFF005139)
val md_theme_dark_onPrimaryContainer = Color(0xFFA4F2CE)
val md_theme_dark_secondary = Color(0xFFB1CCC3)
val md_theme_dark_onSecondary = Color(0xFF1C352F)
val md_theme_dark_secondaryContainer = Color(0xFF334B45)
val md_theme_dark_onSecondaryContainer = Color(0xFFCDE8DF)
val md_theme_dark_tertiary = Color(0xFFA7CCE0)
val md_theme_dark_onTertiary = Color(0xFF0C3444)
val md_theme_dark_tertiaryContainer = Color(0xFF274A5C)
val md_theme_dark_onTertiaryContainer = Color(0xFFC3E8FB)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Color(0xFF191C1A)
val md_theme_dark_onBackground = Color(0xFFE1E3DF)
val md_theme_dark_surface = Color(0xFF191C1A)
val md_theme_dark_onSurface = Color(0xFFE1E3DF)
val md_theme_dark_surfaceVariant = Color(0xFF3F4944)
val md_theme_dark_onSurfaceVariant = Color(0xFFBFC9C3)
val md_theme_dark_outline = Color(0xFF89938E)
val md_theme_dark_outlineVariant = Color(0xFF3F4944)
val md_theme_dark_inverseSurface = Color(0xFFE1E3DF)
val md_theme_dark_inverseOnSurface = Color(0xFF2E3130)
val md_theme_dark_inversePrimary = Color(0xFF1B6B52)
val md_theme_dark_surfaceTint = Color(0xFF7EDBB5)

/**
 * 动态主题系统
 * 支持 System/Light/Dark 三种模式 + Material You 动态取色
 */
object DynamicTheme {
    // 亮色方案
    val LightColorScheme = lightColorScheme(
        primary = md_theme_light_primary,
        onPrimary = md_theme_light_onPrimary,
        primaryContainer = md_theme_light_primaryContainer,
        onPrimaryContainer = md_theme_light_onPrimaryContainer,
        secondary = md_theme_light_secondary,
        onSecondary = md_theme_light_onSecondary,
        secondaryContainer = md_theme_light_secondaryContainer,
        onSecondaryContainer = md_theme_light_onSecondaryContainer,
        tertiary = md_theme_light_tertiary,
        onTertiary = md_theme_light_onTertiary,
        tertiaryContainer = md_theme_light_tertiaryContainer,
        onTertiaryContainer = md_theme_light_onTertiaryContainer,
        error = md_theme_light_error,
        onError = md_theme_light_onError,
        errorContainer = md_theme_light_errorContainer,
        onErrorContainer = md_theme_light_onErrorContainer,
        background = md_theme_light_background,
        onBackground = md_theme_light_onBackground,
        surface = md_theme_light_surface,
        onSurface = md_theme_light_onSurface,
        surfaceVariant = md_theme_light_surfaceVariant,
        onSurfaceVariant = md_theme_light_onSurfaceVariant,
        outline = md_theme_light_outline,
        outlineVariant = md_theme_light_outlineVariant,
        inverseSurface = md_theme_light_inverseSurface,
        inverseOnSurface = md_theme_light_inverseOnSurface,
        inversePrimary = md_theme_light_inversePrimary,
        surfaceTint = md_theme_light_surfaceTint,
    )

    // 暗色方案
    val DarkColorScheme = darkColorScheme(
        primary = md_theme_dark_primary,
        onPrimary = md_theme_dark_onPrimary,
        primaryContainer = md_theme_dark_primaryContainer,
        onPrimaryContainer = md_theme_dark_onPrimaryContainer,
        secondary = md_theme_dark_secondary,
        onSecondary = md_theme_dark_onSecondary,
        secondaryContainer = md_theme_dark_secondaryContainer,
        onSecondaryContainer = md_theme_dark_onSecondaryContainer,
        tertiary = md_theme_dark_tertiary,
        onTertiary = md_theme_dark_onTertiary,
        tertiaryContainer = md_theme_dark_tertiaryContainer,
        onTertiaryContainer = md_theme_dark_onTertiaryContainer,
        error = md_theme_dark_error,
        onError = md_theme_dark_onError,
        errorContainer = md_theme_dark_errorContainer,
        onErrorContainer = md_theme_dark_onErrorContainer,
        background = md_theme_dark_background,
        onBackground = md_theme_dark_onBackground,
        surface = md_theme_dark_surface,
        onSurface = md_theme_dark_onSurface,
        surfaceVariant = md_theme_dark_surfaceVariant,
        onSurfaceVariant = md_theme_dark_onSurfaceVariant,
        outline = md_theme_dark_outline,
        outlineVariant = md_theme_dark_outlineVariant,
        inverseSurface = md_theme_dark_inverseSurface,
        inverseOnSurface = md_theme_dark_inverseOnSurface,
        inversePrimary = md_theme_dark_inversePrimary,
        surfaceTint = md_theme_dark_surfaceTint,
    )

    /**
     * 根据主题模式获取颜色方案
     * Android 12+ 支持动态取色（Material You）
     */
    @Composable
    fun getColorScheme(themeMode: ThemeMode, dynamicColor: Boolean = true): ColorScheme {
        val darkTheme = when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }

        return when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    }

    /**
     * 更新状态栏颜色
     */
    @Composable
    fun UpdateStatusBarColor(themeMode: ThemeMode, dynamicColor: Boolean = true) {
        val colorScheme = getColorScheme(themeMode, dynamicColor)
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme(themeMode)
            }
        }
    }

    private fun darkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> false // Will be determined by isSystemInDarkTheme
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
}
