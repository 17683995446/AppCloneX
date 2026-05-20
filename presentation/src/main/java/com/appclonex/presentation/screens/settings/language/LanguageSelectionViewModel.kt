package com.appclonex.presentation.screens.settings.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appclonex.infrastructure.localization.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 语言选择ViewModel
 */
@HiltViewModel
class LanguageSelectionViewModel @Inject constructor(
    private val languageManager: LanguageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LanguageSelectionUiState())
    val uiState: StateFlow<LanguageSelectionUiState> = _uiState.asStateFlow()

    init {
        loadLanguageOptions()
    }

    private fun loadLanguageOptions() {
        viewModelScope.launch {
            val currentLanguage = languageManager.getCurrentLanguageCode()
            val options = languageManager.getLanguageOptions()

            _uiState.update { state ->
                state.copy(
                    currentLanguageCode = currentLanguage,
                    languageOptions = options
                )
            }
        }
    }

    /**
     * 选择语言
     */
    fun selectLanguage(languageCode: String) {
        viewModelScope.launch {
            languageManager.setLanguage(languageCode)
            _uiState.update { state ->
                state.copy(currentLanguageCode = languageCode)
            }
        }
    }
}
