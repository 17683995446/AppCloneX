package com.appclonex.presentation.screens.settings.language

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appclonex.infrastructure.localization.LanguageManager
import com.appclonex.presentation.R

/**
 * 语言选择屏幕
 *
 * 允许用户切换应用语言
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(
    onNavigateBack: () -> Unit,
    viewModel: LanguageSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_language)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.languageOptions) { option ->
                LanguageOptionItem(
                    option = option,
                    isSelected = option.code == uiState.currentLanguageCode,
                    onClick = { viewModel.selectLanguage(option.code) }
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionItem(
    option: LanguageManager.LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = option.displayName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = when (option.code) {
                        "system" -> stringResource(R.string.lang_system)
                        "en" -> "English"
                        "zh" -> "简体中文"
                        "zh-rTW" -> "繁體中文"
                        else -> option.code
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * UI状态
 */
data class LanguageSelectionUiState(
    val currentLanguageCode: String = LanguageManager.LANGUAGE_SYSTEM,
    val languageOptions: List<LanguageManager.LanguageOption> = emptyList()
)
