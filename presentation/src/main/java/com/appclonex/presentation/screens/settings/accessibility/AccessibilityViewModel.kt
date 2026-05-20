package com.appclonex.presentation.screens.settings.accessibility

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appclonex.domain.model.AccessibilitySettings
import com.appclonex.domain.model.FontSizeScale
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AccessibilityViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val settingsFile: File
        get() = File(context.filesDir, "accessibility_settings.json")

    private val _state = MutableStateFlow(AccessibilitySettings())
    val state: StateFlow<AccessibilitySettings> = _state.asStateFlow()

    init {
        loadSettings()
    }

    // ── Font size ─────────────────────────────────────────────────

    fun setFontSizeScale(scale: Float) {
        _state.value = _state.value.copy(fontSizeScale = scale)
        persistSettings()
    }

    fun setFontSizeByEnum(fontSizeScale: FontSizeScale) {
        _state.value = _state.value.copy(fontSizeScale = fontSizeScale.scale)
        persistSettings()
    }

    // ── Toggle helpers ────────────────────────────────────────────

    fun toggleHighContrast() {
        _state.value = _state.value.copy(highContrast = !_state.value.highContrast)
        persistSettings()
    }

    fun toggleBoldText() {
        _state.value = _state.value.copy(boldText = !_state.value.boldText)
        persistSettings()
    }

    fun toggleReduceMotion() {
        _state.value = _state.value.copy(reduceMotion = !_state.value.reduceMotion)
        persistSettings()
    }

    fun toggleColorBlindFriendly() {
        _state.value = _state.value.copy(colorBlindFriendly = !_state.value.colorBlindFriendly)
        persistSettings()
    }

    fun toggleScreenReaderDescriptions() {
        _state.value = _state.value.copy(
            screenReaderDescriptions = !_state.value.screenReaderDescriptions
        )
        persistSettings()
    }

    // ── Touch target size ─────────────────────────────────────────

    fun setTouchTargetSize(dp: Int) {
        _state.value = _state.value.copy(touchTargetSize = dp)
        persistSettings()
    }

    // ── Reset ─────────────────────────────────────────────────────

    fun resetToDefaults() {
        _state.value = AccessibilitySettings()
        persistSettings()
    }

    // ── Persistence ───────────────────────────────────────────────

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                if (settingsFile.exists()) {
                    val content = settingsFile.readText()
                    if (content.isNotBlank()) {
                        val settings = json.decodeFromString<AccessibilitySettings>(content)
                        _state.value = settings
                    }
                }
            } catch (_: Exception) {
                // If deserialization fails, keep defaults
            }
        }
    }

    private fun persistSettings() {
        viewModelScope.launch {
            try {
                val content = json.encodeToString(_state.value)
                settingsFile.writeText(content)
            } catch (_: Exception) {
                // Silently fail – settings persistence should never crash
            }
        }
    }
}
