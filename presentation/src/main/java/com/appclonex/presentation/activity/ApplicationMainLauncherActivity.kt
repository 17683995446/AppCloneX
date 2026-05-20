package com.appclonex.presentation.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.appclonex.presentation.navigation.AppCloneXNavHost
import com.appclonex.presentation.theme.AppCloneXTheme
import com.appclonex.presentation.theme.LocalAppThemeOverrides
import com.appclonex.presentation.theme.ThemeOverrides
import dagger.hilt.android.AndroidEntryPoint

/**
 * 应用主入口 Activity
 */
@AndroidEntryPoint
class ApplicationMainLauncherActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val context = this
            // We'll use a simple remember with mutableState for dark theme
            // that gets updated when the user changes the setting
            var isDarkTheme by remember { mutableStateOf(false) }

            // Observe the setting (simplified - in production use a ViewModel)
            LaunchedEffect(Unit) {
                // This is a simplified approach - the actual implementation
                // would use a shared ViewModel or DataStore observation
            }

            CompositionLocalProvider(
                LocalAppThemeOverrides provides ThemeOverrides(darkTheme = isDarkTheme)
            ) {
                AppCloneXTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppCloneXNavHost()
                    }
                }
            }
        }
    }
}
