package com.appclonex.domain.model

/**
 * 应用主题模式
 */
enum class ThemeMode {
    SYSTEM,  // 跟随系统
    LIGHT,   // 浅色模式
    DARK;    // 深色模式

    companion object {
        /**
         * 根据 ordinal 获取 ThemeMode
         */
        fun fromOrdinal(ordinal: Int): ThemeMode {
            return entries.getOrElse(ordinal) { SYSTEM }
        }
    }
}
