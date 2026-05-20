package com.appclonex.infrastructure.utility

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.util.Locale

/**
 * 语言配置管理器

 * 支持动态切换应用语言，无需重启应用

 * 使用方式：
 * 1. 在 Application.onCreate() 中调用 applyLanguage()
 * 2. 在设置页面调用 setLanguage() 切换语言
 * 3. 在 Activity.attachBaseContext() 中调用 attachLanguage()
 */
object ApplicationLanguageConfigurationManager {
    
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    
    // 支持的语言列表
    val SUPPORTED_LANGUAGES = listOf(
        Language("system", "System Default", null),
        Language("en", "English", Locale.ENGLISH),
        Language("zh", "中文", Locale.CHINESE)
    )
    
    data class Language(
        val code: String,
        val displayName: String,
        val locale: Locale?
    )
    
    /**
     * 获取当前选择的语言代码
     */
    fun getSelectedLanguageCode(context: Context): String {
        val prefs = getPrefs(context)
        return prefs.getString(KEY_LANGUAGE, "system") ?: "system"
    }
    
    /**
     * 获取当前语言对象
     */
    fun getSelectedLanguage(context: Context): Language {
        val code = getSelectedLanguageCode(context)
        return SUPPORTED_LANGUAGES.find { it.code == code } ?: SUPPORTED_LANGUAGES[0]
    }
    
    /**
     * 设置语言
     * @return 是否需要重启 Activity
     */
    fun setLanguage(context: Context, languageCode: String): Boolean {
        val currentCode = getSelectedLanguageCode(context)
        
        if (currentCode == languageCode) {
            return false
        }
        
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
        
        return true
    }
    
    /**
     * 应用语言设置
     * 在 Application.onCreate() 和 Activity.onCreate() 中调用
     */
    fun applyLanguage(context: Context): Context {
        val languageCode = getSelectedLanguageCode(context)
        val locale = getLocaleForCode(languageCode)
        
        return updateResources(context, locale)
    }
    
    /**
     * 附加语言上下文
     * 在 Activity.attachBaseContext() 中调用
     */
    fun attachLanguage(context: Context): Context {
        return applyLanguage(context)
    }
    
    /**
     * 获取语言代码对应的 Locale
     */
    private fun getLocaleForCode(code: String): Locale? {
        return when (code) {
            "system" -> null // 使用系统语言
            "en" -> Locale.ENGLISH
            "zh" -> Locale.CHINESE
            else -> null
        }
    }
    
    /**
     * 更新资源配置
     */
    private fun updateResources(context: Context, locale: Locale?): Context {
        if (locale == null) {
            // 使用系统默认语言
            return context
        }
        
        val configuration = context.resources.configuration
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * 获取当前实际使用的 Locale
     */
    fun getCurrentLocale(context: Context): Locale {
        val languageCode = getSelectedLanguageCode(context)
        val locale = getLocaleForCode(languageCode)
        
        return locale ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }
    
    /**
     * 获取语言显示名称
     */
    fun getLanguageDisplayName(context: Context): String {
        val language = getSelectedLanguage(context)
        return language.displayName
    }
    
    /**
     * 检查是否是 RTL 语言
     */
    fun isRtlLanguage(context: Context): Boolean {
        val locale = getCurrentLocale(context)
        val direction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            android.text.TextUtils.getLayoutDirectionFromLocale(locale)
        } else {
            // 简单判断阿拉伯语、希伯来语等 RTL 语言
            locale.language in listOf("ar", "he", "fa", "ur")
        }
        return direction == android.view.View.LAYOUT_DIRECTION_RTL
    }
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
