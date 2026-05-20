package com.appclonex.infrastructure.repository

import com.appclonex.domain.model.ThemeMode
import com.appclonex.domain.repository.PreferencesRepository
import com.appclonex.infrastructure.persistence.datastore.ApplicationPreferencesDataStoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 偏好设置仓库实现
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStoreManager: ApplicationPreferencesDataStoreManager
) : PreferencesRepository {

    override fun getThemeMode(): Flow<ThemeMode> {
        return dataStoreManager.themeMode.map { mode ->
            when (mode) {
                1 -> ThemeMode.LIGHT
                2 -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        val modeValue = when (mode) {
            ThemeMode.LIGHT -> 1
            ThemeMode.DARK -> 2
            ThemeMode.SYSTEM -> 0
        }
        dataStoreManager.setThemeMode(modeValue)
    }

    override fun getExcludeSystemApps(): Flow<Boolean> {
        return dataStoreManager.excludeSystemApps
    }

    override suspend fun setExcludeSystemApps(exclude: Boolean) {
        dataStoreManager.setExcludeSystemApps(exclude)
    }

    override fun getShowInstalledApps(): Flow<Boolean> {
        return dataStoreManager.allSettings.map {
            it["show_installed_apps"] as? Boolean ?: true
        }
    }

    override suspend fun setShowInstalledApps(show: Boolean) {
        dataStoreManager.setShowInstalledApps(show)
    }

    override fun isOnboardingCompleted(): Flow<Boolean> {
        return dataStoreManager.isOnboardingCompleted
    }

    override suspend fun setOnboardingCompleted() {
        dataStoreManager.setOnboardingCompleted()
    }

    override fun getShowNotifications(): Flow<Boolean> {
        return dataStoreManager.allSettings.map {
            it["show_notifications"] as? Boolean ?: true
        }
    }

    override suspend fun setShowNotifications(show: Boolean) {
        dataStoreManager.setBooleanValue("show_notifications", show)
    }

    override fun getAutoStartProfile(): Flow<Boolean> {
        return dataStoreManager.allSettings.map {
            it["auto_start_profile"] as? Boolean ?: false
        }
    }

    override suspend fun setAutoStartProfile(autoStart: Boolean) {
        dataStoreManager.setBooleanValue("auto_start_profile", autoStart)
    }

    override fun getDarkTheme(): Flow<Boolean> {
        return dataStoreManager.allSettings.map {
            it["dark_theme"] as? Boolean ?: false
        }
    }

    override suspend fun setDarkTheme(dark: Boolean) {
        dataStoreManager.setBooleanValue("dark_theme", dark)
    }
}
