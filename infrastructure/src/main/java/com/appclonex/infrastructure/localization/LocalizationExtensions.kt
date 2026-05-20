package com.appclonex.infrastructure.localization

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import java.util.Locale

/**
 * 本地化扩展函数
 *
 * 提供便捷的本地化相关操作
 */

/**
 * 获取本地化字符串，支持动态语言切换
 */
fun Context.getLocalizedString(resId: Int, locale: Locale = Locale.getDefault()): String {
    val config = android.content.res.Configuration(resources.configuration)
    config.setLocale(locale)
    val localizedContext = createConfigurationContext(config)
    return localizedContext.getString(resId)
}

/**
 * 获取本地化字符串，支持参数
 */
fun Context.getLocalizedString(resId: Int, vararg formatArgs: Any, locale: Locale = Locale.getDefault()): String {
    val config = android.content.res.Configuration(resources.configuration)
    config.setLocale(locale)
    val localizedContext = createConfigurationContext(config)
    return localizedContext.getString(resId, *formatArgs)
}

/**
 * 获取当前语言的文本方向
 */
fun Context.isRtlLanguage(): Boolean {
    val locale = resources.configuration.locale
    return when (locale.language) {
        "ar", "he", "iw", "fa", "ur" -> true
        else -> false
    }
}

/**
 * 格式化数字为本地化字符串
 */
fun Number.toLocalizedString(locale: Locale = Locale.getDefault()): String {
    return String.format(locale, "%,d", this)
}

/**
 * 格式化文件大小为本地化字符串
 */
fun Long.toLocalizedFileSize(context: Context): String {
    return when {
        this < 1024 -> "$this B"
        this < 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f KB", this / 1024.0)
        this < 1024 * 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f MB", this / (1024.0 * 1024.0))
        else -> String.format(Locale.getDefault(), "%.2f GB", this / (1024.0 * 1024.0 * 1024.0))
    }
}

/**
 * 格式化日期为本地化字符串
 */
fun java.util.Date.toLocalizedString(pattern: String = "yyyy-MM-dd", locale: Locale = Locale.getDefault()): String {
    val formatter = java.text.SimpleDateFormat(pattern, locale)
    return formatter.format(this)
}

/**
 * 格式化时间为本地化字符串
 */
fun java.util.Date.toLocalizedTimeString(locale: Locale = Locale.getDefault()): String {
    val formatter = java.text.SimpleDateFormat("HH:mm", locale)
    return formatter.format(this)
}

/**
 * 格式化日期时间为本地化字符串
 */
fun java.util.Date.toLocalizedDateTimeString(locale: Locale = Locale.getDefault()): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", locale)
    return formatter.format(this)
}

/**
 * 获取语言的本地化名称
 */
fun Locale.getDisplayNameLocalized(): String {
    return getDisplayName(this)
}

/**
 * 获取语言的本地化名称（简短）
 */
fun Locale.getDisplayLanguageLocalized(): String {
    return getDisplayLanguage(this)
}

/**
 * 获取语言的本地化名称（在指定语言中）
 */
fun Locale.getDisplayNameIn(targetLocale: Locale): String {
    return getDisplayName(targetLocale)
}

/**
 * 检查是否为中文语言环境
 */
fun Locale.isChinese(): Boolean {
    return language == Locale.CHINESE.language ||
            language == Locale.SIMPLIFIED_CHINESE.language ||
            language == Locale.TRADITIONAL_CHINESE.language
}

/**
 * 检查是否为简体中文
 */
fun Locale.isSimplifiedChinese(): Boolean {
    return this == Locale.SIMPLIFIED_CHINESE ||
            (language == "zh" && country == "CN")
}

/**
 * 检查是否为繁体中文
 */
fun Locale.isTraditionalChinese(): Boolean {
    return this == Locale.TRADITIONAL_CHINESE ||
            (language == "zh" && (country == "TW" || country == "HK"))
}

/**
 * 获取语言的Emoji国旗
 */
fun Locale.getFlagEmoji(): String {
    return when (this) {
        Locale.CHINA, Locale.SIMPLIFIED_CHINESE -> "🇨🇳"
        Locale.TAIWAN, Locale.TRADITIONAL_CHINESE ->