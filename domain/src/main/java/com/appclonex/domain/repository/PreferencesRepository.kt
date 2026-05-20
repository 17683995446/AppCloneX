package com.appclonex.domain.repository

import com.appclonex.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * 偏好设置仓库接口
 *
 * 提供应用偏好设置的读写操作
 */
interface PreferencesRepository {
    /**
     * 获取主题模式
     */
    fun getThemeMode(): Flow<ThemeMode>

    /**
     * 设置主题模式
     */
    suspend fun setThemeMode(mode: ThemeMode)

    /**
     * 获取是否排除系统应用
     */
    fun getExcludeSystemApps(): Flow<Boolean>

    /**
     * 设置是否排除系统应用
     */
    suspend fun setExcludeSystemApps(exclude: Boolean)

    /**
     * 获取是否显示已安装应用
     */
    fun getShowInstalledApps(): Flow<Boolean>

    /**
     * 设置是否显示已安装应用
     */
    suspend fun setShowInstalledApps(show: Boolean)

    /**
     * 获取是否已完成引导页
     */
    fun isOnboardingCompleted(): Flow<Boolean>

    /**
     * 标记引导页已完成
     */
    suspend fun setOnboardingCompleted()

    /**
     * 获取是否显示通知
     */
    fun getShowNotifications(): Flow<Boolean>

    /**
     * 设置是否显示通知
     */
    suspend fun setShowNotifications(show: Boolean)

    /**
     * 获取是否自动启动配置文件
     */
    fun getAutoStartProfile(): Flow<Boolean>

    /**
     * 设置是否自动启动配置文件
     */
    suspend fun setAutoStartProfile(autoStart: Boolean)

    /**
     * 获取深色模式设置
     */
    fun getDarkTheme(): Flow<Boolean>

    /**
     * 设置深色模式
     */
    suspend fun setDarkTheme(dark: Boolean)
}
