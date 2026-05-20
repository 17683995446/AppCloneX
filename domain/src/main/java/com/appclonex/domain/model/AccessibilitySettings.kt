package com.appclonex.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AccessibilitySettings(
    val fontSizeScale: Float = 1f,        // 0.85f, 1f, 1.15f, 1.3f
    val highContrast: Boolean = false,
    val boldText: Boolean = false,
    val reduceMotion: Boolean = false,
    val touchTargetSize: Int = 48,        // dp
    val colorBlindFriendly: Boolean = false,
    val screenReaderDescriptions: Boolean = true
)

enum class FontSizeScale(val scale: Float, val label: String) {
    SMALL(0.85f, "小"),
    DEFAULT(1f, "默认"),
    LARGE(1.15f, "大"),
    EXTRA_LARGE(1.3f, "特大")
}
