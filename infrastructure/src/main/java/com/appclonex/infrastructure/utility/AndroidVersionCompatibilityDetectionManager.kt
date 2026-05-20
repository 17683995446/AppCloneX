package com.appclonex.infrastructure.utility

import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android 版本兼容性检测管理器

 * 解决 P1 缺陷：Android 版本兼容性问题
 * 提供版本检测、功能可用性检查和降级方案
 */
@Singleton
class AndroidVersionCompatibilityDetectionManager @Inject constructor() {

    /**
     * 支持的最低 Android 版本
     */
    val minimumSupportedVersion: AndroidVersion = AndroidVersion.N

    /**
     * 推荐的最低 Android 版本（完整功能）
     */
    val recommendedVersion: AndroidVersion = AndroidVersion.R

    /**
     * 当前设备 Android 版本
     */
    val currentVersion: AndroidVersion
        get() = AndroidVersion.fromSdkInt(Build.VERSION.SDK_INT)

    /**
     * 检查当前设备是否满足最低版本要求
     */
    fun isMinimumVersionSupported(): Boolean {
        return Build.VERSION.SDK_INT >= minimumSupportedVersion.sdkInt
    }

    /**
     * 检查当前设备是否满足推荐版本
     */
    fun isRecommendedVersion(): Boolean {
        return Build.VERSION.SDK_INT >= recommendedVersion.sdkInt
    }

    /**
     * 获取版本兼容性报告
     */
    fun getCompatibilityReport(): VersionCompatibilityReportModel {
        val features = mutableListOf<FeatureCompatibilityModel>()

        // Shizuku 无线调试（Android 11+）
        features.add(FeatureCompatibilityModel(
            featureName = "Shizuku 无线调试启动",
            minVersion = AndroidVersion.R,
            currentVersion = currentVersion,
            fallbackDescription = "可通过 USB 连接电脑启动 Shizuku"
        ))

        // 应用冻结功能（Android 6+）
        features.add(FeatureCompatibilityModel(
            featureName = "应用冻结/暂停",
            minVersion = AndroidVersion.M,
            currentVersion = currentVersion,
            fallbackDescription = null
        ))

        // 沙盒隔离（Android 9+）
        features.add(FeatureCompatibilityModel(
            featureName = "完整沙盒数据隔离",
            minVersion = AndroidVersion.P,
            currentVersion = currentVersion,
            fallbackDescription = "使用基础文件隔离替代"
        ))

        // 生物识别（Android 6+）
        features.add(FeatureCompatibilityModel(
            featureName = "生物识别认证",
            minVersion = AndroidVersion.M,
            currentVersion = currentVersion,
            fallbackDescription = "使用 PIN/图案锁替代"
        ))

        // 通知渠道（Android 8+）
        features.add(FeatureCompatibilityModel(
            featureName = "克隆进度通知",
            minVersion = AndroidVersion.O,
            currentVersion = currentVersion,
            fallbackDescription = "使用基础通知"
        ))

        // 桌面小组件（Android 5+）
        features.add(FeatureCompatibilityModel(
            featureName = "桌面快捷操作小组件",
            minVersion = AndroidVersion.LOLLIPOP,
            currentVersion = currentVersion,
            fallbackDescription = null
        ))

        val supportedFeatures = features.count { it.isSupported }
        val degradedFeatures = features.count { !it.isSupported && it.fallbackDescription != null }
        val unsupportedFeatures = features.count { !it.isSupported && it.fallbackDescription == null }

        return VersionCompatibilityReportModel(
            currentVersion = currentVersion,
            isMinimumSupported = isMinimumVersionSupported(),
            isRecommended = isRecommendedVersion(),
            totalFeatures = features.size,
            supportedFeatures = supportedFeatures,
            degradedFeatures = degradedFeatures,
            unsupportedFeatures = unsupportedFeatures,
            featureDetails = features
        )
    }

    /**
     * 获取 Shizuku 推荐启动方式
     */
    fun getRecommendedShizukuLaunchMethod(): ShizukuLaunchMethodModel {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ShizukuLaunchMethodModel(
                method = ShizukuLaunchMethod.WIRELESS_DEBUGGING,
                description = "通过无线调试启动（推荐）",
                requiresComputer = false,
                minVersion = AndroidVersion.R
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShizukuLaunchMethodModel(
                method = ShizukuLaunchMethod.ADB_OVER_TCP,
                description = "通过 ADB 连接启动",
                requiresComputer = true,
                minVersion = AndroidVersion.O
            )
        } else {
            ShizukuLaunchMethodModel(
                method = ShizukuLaunchMethod.USB_CONNECTED,
                description = "通过 USB 连接电脑启动",
                requiresComputer = true,
                minVersion = AndroidVersion.LOLLIPOP
            )
        }
    }
}

/**
 * Android 版本枚举
 */
enum class AndroidVersion(val sdkInt: Int, val name: String, val codename: String) {
    LOLLIPOP(21, "5.0", "Lollipop"),
    M(23, "6.0", "Marshmallow"),
    N(24, "7.0", "Nougat"),
    O(26, "8.0", "Oreo"),
    P(28, "9.0", "Pie"),
    Q(29, "10", "Q"),
    R(30, "11", "R"),
    S(31, "12", "S"),
    TIRAMISU(33, "13", "Tiramisu"),
    UPSIDE_DOWN_CAKE(34, "14", "Upside Down Cake");

    companion object {
        fun fromSdkInt(sdkInt: Int): AndroidVersion {
            return entries.find { it.sdkInt <= sdkInt } ?: LOLLIPOP
        }
    }
}

/**
 * Shizuku 启动方式
 */
enum class ShizukuLaunchMethod {
    WIRELESS_DEBUGGING,
    ADB_OVER_TCP,
    USB_CONNECTED
}

/**
 * Shizuku 启动方式模型
 */
data class ShizukuLaunchMethodModel(
    val method: ShizukuLaunchMethod,
    val description: String,
    val requiresComputer: Boolean,
    val minVersion: AndroidVersion
)

/**
 * 功能兼容性模型
 */
data class FeatureCompatibilityModel(
    val featureName: String,
    val minVersion: AndroidVersion,
    val currentVersion: AndroidVersion,
    val fallbackDescription: String?
) {
    val isSupported: Boolean
        get() = currentVersion.sdkInt >= minVersion.sdkInt

    val supportStatus: FeatureSupportStatus
        get() = when {
            isSupported -> FeatureSupportStatus.FULL
            fallbackDescription != null -> FeatureSupportStatus.DEGRADED
            else -> FeatureSupportStatus.UNSUPPORTED
        }
}

/**
 * 功能支持状态
 */
enum class FeatureSupportStatus {
    FULL,
    DEGRADED,
    UNSUPPORTED
}

/**
 * 版本兼容性报告模型
 */
data class VersionCompatibilityReportModel(
    val currentVersion: AndroidVersion,
    val isMinimumSupported: Boolean,
    val isRecommended: Boolean,
    val totalFeatures: Int,
    val supportedFeatures: Int,
    val degradedFeatures: Int,
    val unsupportedFeatures: Int,
    val featureDetails: List<FeatureCompatibilityModel>
) {
    val compatibilityScore: Int
        get() = if (totalFeatures == 0) 0 else (supportedFeatures * 100 + degradedFeatures * 50) / totalFeatures

    val compatibilityLevel: CompatibilityLevel
        get() = when {
            compatibilityScore >= 90 -> CompatibilityLevel.EXCELLENT
            compatibilityScore >= 70 -> CompatibilityLevel.GOOD
            compatibilityScore >= 50 -> CompatibilityLevel.FAIR
            else -> CompatibilityLevel.POOR
        }
}

/**
 * 兼容性等级
 */
enum class CompatibilityLevel {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR
}
