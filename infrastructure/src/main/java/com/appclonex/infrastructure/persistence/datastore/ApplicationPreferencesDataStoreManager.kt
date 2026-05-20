package com.appclonex.infrastructure.persistence.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 偏好设置 DataStore
 * 使用 DataStore 持久化用户偏好设置
 */
@Singleton
class ApplicationPreferencesDataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    companion object {
        // 设置项键名
        private val KEY_EXCLUDE_SYSTEM_APPS = booleanPreferencesKey("exclude_system_apps")
        private val KEY_SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        private val KEY_AUTO_START_PROFILE = booleanPreferencesKey("auto_start_profile")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_THEME_MODE = intPreferencesKey("theme_mode")
        private val KEY_LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        private val KEY_TOTAL_CLONES = longPreferencesKey("total_clones")
        private val KEY_TOTAL_PROFILES = longPreferencesKey("total_profiles")
        private val KEY_SHOW_INSTALLED_APPS = booleanPreferencesKey("show_installed_apps")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    // 是否排除系统应用
    val excludeSystemApps: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[KEY_EXCLUDE_SYSTEM_APPS] ?: true }

    // 是否显示通知
    val showNotifications: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[KEY_SHOW_NOTIFICATIONS] ?: true }

    // 是否自动启动配置文件
    val autoStartProfile: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[KEY_AUTO_START_PROFILE] ?: false }

    // 主题模式
    val themeMode: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[KEY_THEME_MODE] ?: 0 }

    // 上次备份时间
    val lastBackupTime: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[KEY_LAST_BACKUP_TIME] ?: 0L }

    // 总克隆数
    val totalClones: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[KEY_TOTAL_CLONES] ?: 0L }

    // 总配置文件数
    val totalProfiles: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[KEY_TOTAL_PROFILES] ?: 0L }

    // 是否已完成引导页
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[KEY_ONBOARDING_COMPLETED] ?: false }

    // 设置排除系统应用
    suspend fun setExcludeSystemApps(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_EXCLUDE_SYSTEM_APPS] = value
        }
    }

    // 设置显示通知
    suspend fun setShowNotifications(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_NOTIFICATIONS] = value
        }
    }

    // 设置自动启动配置文件
    suspend fun setAutoStartProfile(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_START_PROFILE] = value
        }
    }

    // 设置显示已安装应用
    suspend fun setShowInstalledApps(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_INSTALLED_APPS] = value
        }
    }

    // 设置主题模式
    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    // 更新上次备份时间
    suspend fun updateLastBackupTime() {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_BACKUP_TIME] = System.currentTimeMillis()
        }
    }

    // 更新统计信息
    suspend fun updateStatistics(totalClones: Long, totalProfiles: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TOTAL_CLONES] = totalClones
            preferences[KEY_TOTAL_PROFILES] = totalProfiles
        }
    }

    // 标记引导页已完成
    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = true
        }
    }

    // 获取所有设置（用于备份）
    val allSettings: Flow<Map<String, Any>> = context.dataStore.data
        .map { preferences ->
            mapOf(
                "exclude_system_apps" to (preferences[KEY_EXCLUDE_SYSTEM_APPS] ?: true),
                "show_notifications" to (preferences[KEY_SHOW_NOTIFICATIONS] ?: true),
                "auto_start_profile" to (preferences[KEY_AUTO_START_PROFILE] ?: false)
            )
        }

    // 通用字符串读写方法
    suspend fun setStringValue(key: String, value: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }

    suspend fun setBooleanValue(key: String, value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(key)] = value
        }
    }

    suspend fun setIntValue(key: String, value: Int) {
        context.dataStore.edit { preferences ->
            preferences[intPreferencesKey(key)] = value
        }
    }

    suspend fun setStringListValue(key: String, value: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value.joinToString("\u0000")
        }
    }

    fun getStringValue(key: String, defaultValue: String = ""): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key)] ?: defaultValue
        }
    }

    fun getBooleanValue(key: String, defaultValue: Boolean = false): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[booleanPreferencesKey(key)] ?: defaultValue
        }
    }

    fun getIntValue(key: String, defaultValue: Int = 0): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[intPreferencesKey(key)] ?: defaultValue
        }
    }

    fun getStringListValue(key: String, defaultValue: List<String> = emptyList()): Flow<List<String>> {
        return context.dataStore.data.map { preferences ->
            val raw = preferences[stringPreferencesKey(key)] ?: return@map defaultValue
            if (raw.isEmpty()) defaultValue else raw.split("\u0000")
        }
    }
}
