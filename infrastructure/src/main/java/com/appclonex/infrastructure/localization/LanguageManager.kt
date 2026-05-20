package com.appclonex.infrastructure.localization

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import com.appclonex.infrastructure.persistence.datastore.AppDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语言管理器
 *
 * 管理应用语言设置，支持多语言切换
 * 支持简体中文(zh)、繁体中文(zh-rTW)、英文(en)
 */
@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: AppDataStore
) {

    companion object {
        const val KEY_LANGUAGE = "app_language"
        const val LANGUAGE_SYSTEM = "system"
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_CHINESE_SIMPLIFIED = "zh"
        const val LANGUAGE_CHINESE_TRADITIONAL = "zh-rTW"
    }

    /**
     * 支持的语言列表
     */
    enum class SupportedLanguage(
        val code: String,
        val displayName: String,
        val locale: Locale
    ) {
        SYSTEM(LANGUAGE_SYSTEM, "跟随系统", Locale.getDefault()),
        ENGLISH(LANGUAGE_ENGLISH, "English", Locale.ENGLISH),
        CHINESE_SIMPLIFIED(LANGUAGE_CHINESE_SIMPLIFIED, "简体中文", Locale.SIMPLIFIED_CHINESE),
        CHINESE_TRADITIONAL(LANGUAGE_CHINESE_TRADITIONAL, "繁體中文", Locale.TRADITIONAL_CHINESE);

        companion object {
            fun fromCode(code: String): SupportedLanguage {
                return values().find { it.code == code } ?: SYSTEM
            }
        }
    }

    /**
     * 当前语言设置流
     */
    val currentLanguage: Flow<SupportedLanguage> = dataStore.getStringFlow(KEY_LANGUAGE, LANGUAGE_SYSTEM)
        .map { SupportedLanguage.fromCode(it) }

    /**
     * 获取当前语言代码
     */
    suspend fun getCurrentLanguageCode(): String {
        return dataStore.getString(KEY_LANGUAGE, LANGUAGE_SYSTEM)
    }

    /**
     * 设置应用语言
     *
     * @param languageCode 语言代码
     */
    suspend fun setLanguage(languageCode: String) {
        dataStore.putString(KEY_LANGUAGE, languageCode)
        updateLocale(languageCode)
    }

    /**
     * 设置应用语言
     *
     * @param language 语言枚举
     */
    suspend fun setLanguage(language: SupportedLanguage) {
        setLanguage(language.code)
    }

    /**
     * 获取系统语言
     */
    fun getSystemLanguage(): Locale {
        return ConfigurationCompat.getLocales(context.resources.configuration)[0]
            ?: Locale.getDefault()
    }

    /**
     * 更新应用Locale
     *
     * @param languageCode 语言代码
     */
    fun updateLocale(languageCode: String): Context {
        val locale = when (languageCode) {
            LANGUAGE_ENGLISH -> Locale.ENGLISH
            LANGUAGE_CHINESE_SIMPLIFIED -> Locale.SIMPLIFIED_CHINESE
            LANGUAGE_CHINESE_TRADITIONAL -> Locale.TRADITIONAL_CHINESE
            else -> getSystemLanguage()
        }

        return updateContextLocale(context, locale)
    }

    /**
     * 更新Context的Locale
     */
    private fun updateContextLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
            context.createConfigurationContext(config)
        } else {
            config.locale = locale
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    /**
     * 获取应用支持的Locale列表
     */
    fun getSupportedLocales(): List<Locale> {
        return listOf(
            Locale.ENGLISH,
            Locale.SIMPLIFIED_CHINESE,
            Locale.TRADITIONAL_CHINESE
        )
    }

    /**
     * 检查是否支持该语言
     */
    fun isLanguageSupported(languageCode: String): Boolean {
        return SupportedLanguage.values().any { it.code == languageCode }
    }

    /**
     * 获取语言的显示名称
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return SupportedLanguage.fromCode(languageCode).displayName
    }

    /**
     * 应用语言设置到Context
     */
    fun applyLanguage(context: Context): Context {
        val languageCode = dataStore.getStringBlocking(KEY_LANGUAGE, LANGUAGE_SYSTEM)
        return updateLocale(languageCode)
    }

    /**
     * 获取语言选择列表（用于UI显示）
     */
    fun getLanguageOptions(): List<LanguageOption> {
        return SupportedLanguage.values().map {
            LanguageOption(
                code = it.code,
                displayName = it.displayName,
                isSelected = false
            )
        }
    }

    /**
     * 语言选项数据类
     */
    data class LanguageOption(
        val code: String,
        val displayName: String,
        var isSelected: Boolean
    )
}
