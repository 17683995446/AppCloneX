package com.appclonex.infrastructure.monitoring

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 远程配置管理器
 *
 * 使用 Firebase Remote Config 实现动态配置
 * 支持功能开关、配置参数远程更新
 *
 * 站在巨人的肩膀上：基于 Firebase Remote Config 最佳实践
 * 高度模块化：功能开关 + 配置项 + 默认值 三层抽象
 */
@Singleton
class RemoteConfigManager @Inject constructor() {

    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600 // 1小时
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(DEFAULT_VALUES)
    }

    // ==================== 基础读取 ====================

    fun getString(key: String): String = remoteConfig.getString(key)
    fun getBoolean(key: String): Boolean = remoteConfig.getBoolean(key)
    fun getLong(key: String): Long = remoteConfig.getLong(key)
    fun getDouble(key: String): Double = remoteConfig.getDouble(key)

    fun getString(key: String, defaultValue: String): String =
        remoteConfig.getString(key).ifEmpty { defaultValue }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean = try {
        remoteConfig.getBoolean(key)
    } catch (e: Exception) {
        defaultValue
    }

    fun getLong(key: String, defaultValue: Long): Long = try {
        remoteConfig.getLong(key)
    } catch (e: Exception) {
        defaultValue
    }

    fun getInt(key: String, defaultValue: Int): Int =
        getLong(key, defaultValue.toLong()).toInt()

    fun getFloat(key: String, defaultValue: Float): Float =
        getDouble(key, defaultValue.toDouble()).toFloat()

    // ==================== 功能开关 ====================

    fun isFeatureEnabled(feature: FeatureFlag): Boolean =
        isFeatureEnabled(feature, feature.defaultEnabled)

    fun isFeatureEnabled(feature: FeatureFlag, defaultValue: Boolean): Boolean =
        getBoolean(feature.configKey, defaultValue)

    // ==================== 集合/列表类型 ====================

    fun getStringList(key: String, delimiter: String = ","): List<String> =
        getString(key).split(delimiter).map { it.trim() }.filter { it.isNotEmpty() }

    fun getStringList(key: String, defaultValue: List<String>, delimiter: String = ","): List<String> =
        getStringList(key, delimiter).ifEmpty { defaultValue }

    fun getStringSet(key: String, delimiter: String = ","): Set<String> =
        getStringList(key, delimiter).toSet()

    fun getIntList(key: String, delimiter: String = ","): List<Int> = try {
        getStringList(key, delimiter).map { it.toInt() }
    } catch (e: Exception) {
        emptyList()
    }

    fun getBooleanList(key: String, delimiter: String = ","): List<Boolean> = try {
        getStringList(key, delimiter).map { it.toBoolean() }
    } catch (e: Exception) {
        emptyList()
    }

    // ==================== Map 类型 ====================

    fun getStringMap(
        key: String,
        entryDelimiter: String = ";",
        kvDelimiter: String = "="
    ): Map<String, String> = try {
        getString(key).split(entryDelimiter)
            .mapNotNull { entry ->
                val parts = entry.split(kvDelimiter, limit = 2)
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
            }
            .toMap()
    } catch (e: Exception) {
        emptyMap()
    }

    // ==================== 特殊类型 ====================

    fun getPercentage(key: String, defaultValue: Float): Float = try {
        getString(key).replace("%", "").toFloatOrNull()?.coerceIn(0f, 100f) ?: defaultValue
    } catch (e: Exception) {
        defaultValue
    }

    fun getVersion(key: String, defaultValue: String): String {
        val v = getString(key)
        return if (v.matches(Regex("""^\d+(\.\d+)*$"""))) v else defaultValue
    }

    fun getUrl(key: String, defaultValue: String): String {
        val url = getString(key)
        return if (url.startsWith("http://") || url.startsWith("https://")) url else defaultValue
    }

    fun getColor(key: String, defaultValue: Int): Int = try {
        val c = getString(key)
        if (c.startsWith("#")) android.graphics.Color.parseColor(c) else defaultValue
    } catch (e: Exception) {
        defaultValue
    }

    // ==================== 远程刷新 ====================

    suspend fun fetchAndActivate(): Boolean = try {
        remoteConfig.fetchAndActivate().await()
    } catch (e: Exception) {
        Timber.e(e, "RemoteConfig fetch failed")
        false
    }

    fun getLastFetchTime(): Long = remoteConfig.info.fetchTimeMillis

    // ==================== 功能开关枚举 ====================

    enum class FeatureFlag(val configKey: String, val defaultEnabled: Boolean) {
        // 克隆功能
        CLONE_BATCH("feature_clone_batch", true),
        CLONE_PROGRESS("feature_clone_progress", true),

        // 冻结功能
        FREEZE_AUTO("feature_freeze_auto", false),
        FREEZE_SCHEDULE("feature_freeze_schedule", false),

        // 备份功能
        BACKUP_ENABLED("feature_backup", false),
        BACKUP_AUTO("feature_backup_auto", false),

        // 统计功能
        STATS_USAGE("feature_stats_usage", true),

        // 监控功能
        MONITOR_PERFORMANCE("feature_monitor_performance", true),
        MONITOR_HEALTH("feature_monitor_health", true),

        // UI功能
        UI_DARK_MODE("feature_ui_dark_mode", true),
        UI_ANIMATION("feature_ui_animation", true),

        // 反馈功能
        FEEDBACK_ENABLED("feature_feedback", true),
    }

    enum class AppConfig(val configKey: String) {
        SUPPORT_EMAIL("config_support_email"),
        FAQ_URL("config_faq_url"),
        UPDATE_URL("config_update_url"),
        MIN_VERSION("config_min_version"),
    }

    enum class NumericConfig(val configKey: String) {
        MAX_CLONE_PROFILES("numeric_max_profiles"),
        CLONE_TIMEOUT_MS("numeric_clone_timeout"),
        AUTO_FREEZE_DELAY_MS("numeric_auto_freeze_delay"),
        CACHE_MAX_SIZE_MB("numeric_cache_max_size"),
        HEALTH_CHECK_INTERVAL_MS("numeric_health_check_interval"),
    }

    enum class StringConfig(val configKey: String) {
        DEFAULT_PROFILE_NAME("string_default_profile_name"),
        MAINTENANCE_MESSAGE("string_maintenance_message"),
    }

    companion object {
        private val DEFAULT_VALUES = mapOf(
            // 功能开关默认值
            "feature_clone_batch" to true,
            "feature_clone_progress" to true,
            "feature_freeze_auto" to false,
            "feature_freeze_schedule" to false,
            "feature_backup" to false,
            "feature_backup_auto" to false,
            "feature_stats_usage" to true,
            "feature_monitor_performance" to true,
            "feature_monitor_health" to true,
            "feature_ui_dark_mode" to true,
            "feature_ui_animation" to true,
            "feature_feedback" to true,

            // 数值配置默认值
            "numeric_max_profiles" to 10L,
            "numeric_clone_timeout" to 60000L,
            "numeric_auto_freeze_delay" to 300000L,
            "numeric_cache_max_size" to 50L,
            "numeric_health_check_interval" to 60000L,

            // 字符串配置默认值
            "string_default_profile_name" to "Profile",
            "config_support_email" to "support@appclonex.dev",
            "config_faq_url" to "https://github.com/your-repo/AppCloneX/wiki",
        )
    }
}
