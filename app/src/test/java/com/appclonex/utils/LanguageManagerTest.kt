package com.appclonex.utils

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

/**
 * LanguageManager 单元测试
 */
class LanguageManagerTest {

    // ==================== 语言数据模型测试 ====================

    @Test
    fun `Language data class should have correct properties`() {
        val language = LanguageManager.Language(
            code = "en",
            displayName = "English",
            locale = Locale.ENGLISH
        )
        
        assertEquals("en", language.code)
        assertEquals("English", language.displayName)
        assertEquals(Locale.ENGLISH, language.locale)
    }

    @Test
    fun `Language with null locale should represent system default`() {
        val systemLanguage = LanguageManager.Language(
            code = "system",
            displayName = "System Default",
            locale = null
        )
        
        assertEquals("system", systemLanguage.code)
        assertNull(systemLanguage.locale)
    }

    // ==================== 支持语言列表测试 ====================

    @Test
    fun `SUPPORTED_LANGUAGES should contain required languages`() {
        val codes = LanguageManager.SUPPORTED_LANGUAGES.map { it.code }
        
        assertTrue("system" in codes)
        assertTrue("en" in codes)
        assertTrue("zh" in codes)
    }

    @Test
    fun `SUPPORTED_LANGUAGES should have correct count`() {
        assertEquals(3, LanguageManager.SUPPORTED_LANGUAGES.size)
    }

    @Test
    fun `system language should be first in list`() {
        assertEquals("system", LanguageManager.SUPPORTED_LANGUAGES[0].code)
    }

    // ==================== Locale 映射测试 ====================

    @Test
    fun `English locale should be correct`() {
        assertEquals(Locale.ENGLISH.language, "en")
    }

    @Test
    fun `Chinese locale should be correct`() {
        assertEquals(Locale.CHINESE.language, "zh")
    }

    // ==================== 语言代码验证测试 ====================

    @Test
    fun `language codes should be valid`() {
        LanguageManager.SUPPORTED_LANGUAGES.forEach { language ->
            assertTrue(
                "Language code '${language.code}' should not be empty",
                language.code.isNotEmpty()
            )
            assertTrue(
                "Language code '${language.code}' should be lowercase",
                language.code == language.code.lowercase()
            )
        }
    }

    @Test
    fun `language display names should not be empty`() {
        LanguageManager.SUPPORTED_LANGUAGES.forEach { language ->
            assertTrue(
                "Display name for '${language.code}' should not be empty",
                language.displayName.isNotEmpty()
            )
        }
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `Language copy should work correctly`() {
        val original = LanguageManager.Language(
            code = "en",
            displayName = "English",
            locale = Locale.ENGLISH
        )
        
        val copied = original.copy(displayName = "English (US)")
        
        assertEquals("en", copied.code)
        assertEquals("English (US)", copied.displayName)
        assertEquals(Locale.ENGLISH, copied.locale)
    }

    @Test
    fun `Language equality should work correctly`() {
        val lang1 = LanguageManager.Language(
            code = "en",
            displayName = "English",
            locale = Locale.ENGLISH
        )
        
        val lang2 = LanguageManager.Language(
            code = "en",
            displayName = "English",
            locale = Locale.ENGLISH
        )
        
        assertEquals(lang1, lang2)
    }

    @Test
    fun `different languages should not be equal`() {
        val english = LanguageManager.Language(
            code = "en",
            displayName = "English",
            locale = Locale.ENGLISH
        )
        
        val chinese = LanguageManager.Language(
            code = "zh",
            displayName = "中文",
            locale = Locale.CHINESE
        )
        
        assertNotEquals(english, chinese)
    }
}
