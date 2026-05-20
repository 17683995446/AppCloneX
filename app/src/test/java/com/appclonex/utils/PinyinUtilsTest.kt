package com.appclonex.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * PinyinUtils 单元测试
 */
class PinyinUtilsTest {

    @Test
    fun `toPinyin converts Chinese characters to pinyin`() {
        val result = PinyinUtils.toPinyin("你好")
        assertThat(result).isEqualTo("nihao")
    }

    @Test
    fun `toPinyin converts English letters to lowercase`() {
        val result = PinyinUtils.toPinyin("Hello")
        assertThat(result).isEqualTo("hello")
    }

    @Test
    fun `toPinyin returns empty string for empty input`() {
        val result = PinyinUtils.toPinyin("")
        assertThat(result).isEmpty()
    }

    @Test
    fun `toPinyin returns empty string for null input`() {
        val result = PinyinUtils.toPinyin(null)
        assertThat(result).isEmpty()
    }

    @Test
    fun `getFirstLetters returns correct first letters for Chinese text`() {
        val result = PinyinUtils.getFirstLetters("你好世界")
        assertThat(result).isEqualTo("nhsj")
    }

    @Test
    fun `matches returns true for pinyin abbreviation`() {
        assertThat(PinyinUtils.matches("微信", "wx")).isTrue()
    }

    @Test
    fun `matches returns true for full pinyin`() {
        assertThat(PinyinUtils.matches("微信", "weixin")).isTrue()
    }

    @Test
    fun `matches returns true for partial pinyin`() {
        assertThat(PinyinUtils.matches("微信", "wei")).isTrue()
    }

    @Test
    fun `matches returns false for non-matching query`() {
        assertThat(PinyinUtils.matches("微信", "abc")).isFalse()
    }

    @Test
    fun `matches returns false for empty text`() {
        assertThat(PinyinUtils.matches("", "abc")).isFalse()
    }

    @Test
    fun `matches returns false for null text`() {
        assertThat(PinyinUtils.matches(null, "abc")).isFalse()
    }
}
