package com.appclonex.infrastructure.utility

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType

/**
 * 拼音搜索转换器
 * 用于将中文转换为拼音，支持拼音搜索
 */
object ChinesePinyinSearchQueryConverter {

    private val format = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.LOWERCASE
        toneType = HanyuPinyinToneType.WITHOUT_TONE
        vCharType = HanyuPinyinVCharType.WITH_V
    }

    /**
     * 将字符串转换为拼音
     * @param input 输入字符串
     * @param separator 分隔符
     * @return 拼音字符串
     */
    fun toPinyin(input: String?, separator: String = ""): String {
        if (input.isNullOrEmpty()) return ""
        
        val result = StringBuilder()
        
        for (char in input) {
            if (char.code in 0x4E00..0x9FA5) {
                // 中文字符
                try {
                    val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(char, format)
                    if (pinyinArray != null && pinyinArray.isNotEmpty()) {
                        result.append(pinyinArray[0])
                        result.append(separator)
                    }
                } catch (e: Exception) {
                    result.append(char)
                }
            } else {
                // 非中文字符
                result.append(char.lowercaseChar())
            }
        }
        
        return result.toString()
    }

    /**
     * 获取拼音首字母
     * @param input 输入字符串
     * @return 首字母字符串
     */
    fun getFirstLetters(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        
        val result = StringBuilder()
        
        for (char in input) {
            if (char.code in 0x4E00..0x9FA5) {
                // 中文字符
                try {
                    val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(char, format)
                    if (pinyinArray != null && pinyinArray.isNotEmpty()) {
                        result.append(pinyinArray[0].first())
                    }
                } catch (e: Exception) {
                    // 忽略错误
                }
            } else if (char.isLetter()) {
                result.append(char.uppercaseChar())
            }
        }
        
        return result.toString()
    }

    /**
     * 检查字符串是否匹配搜索词
     * 支持中文、拼音全拼、拼音首字母搜索
     * @param text 待搜索文本
     * @param query 搜索词
     * @return 是否匹配
     */
    fun match(text: String?, query: String?): Boolean {
        if (text.isNullOrEmpty() || query.isNullOrEmpty()) return false
        
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        
        // 1. 直接匹配
        if (lowerText.contains(lowerQuery)) return true
        
        // 2. 拼音全拼匹配
        val pinyin = toPinyin(text)
        if (pinyin.contains(lowerQuery)) return true
        
        // 3. 拼音首字母匹配
        val firstLetters = getFirstLetters(text).lowercase()
        if (firstLetters.contains(lowerQuery)) return true
        
        return false
    }
}

/**
 * 搜索工具类
 * 扩展搜索功能，支持拼音搜索
 */
object SearchUtils {

    /**
     * 搜索应用列表
     * @param apps 应用列表
     * @param query 搜索词
     * @return 过滤后的应用列表
     */
    inline fun <T> search(
        items: List<T>,
        query: String,
        crossinline getText: (T) -> String
    ): List<T> {
        if (query.isBlank()) return items
        
        return items.filter { item ->
            val text = getText(item)
            ChinesePinyinSearchQueryConverter.match(text, query)
        }
    }
}
