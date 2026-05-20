package com.appclonex.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 克隆应用数据模型。
 *
 * 该数据类代表一个已克隆的应用实例，存储在本地数据库中。
 * 每个克隆应用都关联到一个特定的用户配置文件（UserProfileConfigurationModel），
 * 支持同一应用的多个分身实例。
 *
 * ## 数据库关系
 * - 通过外键关联到 [UserProfileConfigurationModel]
 * - 当用户配置文件被删除时，关联的克隆应用会级联删除
 * - 包含唯一索引约束：同一配置文件下同一包名只能有一个克隆实例
 *
 * ## 使用示例
 * ```kotlin
 * // 创建一个新的克隆应用实例
 * val clonedApp = ClonedApplicationInformationModel(
 *     packageName = "com.example.app",
 *     appName = "示例应用",
 *     profileUserId = 10,
 *     cloneIndex = 1,
 *     versionName = "1.0.0",
 *     versionCode = 1
 * )
 *
 * // 获取显示名称（优先使用自定义名称）
 * val displayName = clonedApp.getDisplayName()
 *
 * // 获取格式化的应用大小
 * val sizeDisplay = clonedApp.getFormattedSize()
 * ```
 *
 * @property id 主键ID，自动生成，默认值为0（新记录）
 * @property packageName 原始应用的包名，如 "com.example.app"
 * @property appName 应用显示名称，从原始应用的标签获取
 * @property appIcon 应用图标路径，存储图标文件的本地路径，可为null
 * @property profileUserId 所属用户配置文件的Android系统用户ID
 * @property cloneIndex 克隆序号，表示同一应用的第几个分身，从1开始
 * @property versionName 应用版本名称，如 "1.0.0"
 * @property versionCode 应用版本号，整数值
 * @property sourceDir APK源文件路径，指向原始APK的位置
 * @property dataDir 应用数据目录路径，存储应用的用户数据
 * @property createdAt 创建时间戳，毫秒级Unix时间戳
 * @property updatedAt 最后更新时间戳，毫秒级Unix时间戳
 * @property isEnabled 是否启用，false表示应用被冻结
 * @property isRunning 是否正在运行，表示应用当前是否在前台运行
 * @property lastLaunchTime 最后一次启动时间，毫秒级Unix时间戳
 * @property launchCount 累计启动次数
 * @property customName 用户自定义的应用名称，优先于appName显示
 * @property customIcon 用户自定义图标路径，优先于appIcon显示
 * @property size 应用占用存储空间大小，单位为字节
 * @property notes 用户备注信息
 *
 * @see UserProfileConfigurationModel
 * @see ClonedAppInfo
 * @see ClonedAppState
 */
@Entity(
    tableName = "cloned_apps",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileConfigurationModel::class,
            parentColumns = ["userId"],
            childColumns = ["profileUserId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileUserId"]),
        Index(value = ["packageName", "profileUserId"], unique = true)
    ]
)
data class ClonedApplicationInformationModel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val packageName: String,
    val appName: String,
    val appIcon: String? = null,

    val profileUserId: Int,
    val cloneIndex: Int = 1,

    val versionName: String? = null,
    val versionCode: Long = 0,

    val sourceDir: String? = null,
    val dataDir: String? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val isEnabled: Boolean = true,
    val isRunning: Boolean = false,

    val lastLaunchTime: Long = 0,
    val launchCount: Int = 0,

    val customName: String? = null,
    val customIcon: String? = null,

    val size: Long = 0L,
    val notes: String? = null
) {
    /**
     * 获取应用的显示名称。
     *
     * 优先返回用户自定义名称（[customName]），如果未设置则返回原始应用名称（[appName]）。
     *
     * @return 应用的显示名称，永不为null
     */
    fun getDisplayName(): String {
        return customName ?: appName
    }

    /**
     * 将应用大小格式化为人类可读的字符串。
     *
     * 自动选择合适的单位（MB或GB）进行显示。
     * - 小于1GB时显示为 "X.X MB"
     * - 大于等于1GB时显示为 "X.X GB"
     *
     * @return 格式化后的大小字符串，如 "256.5 MB" 或 "1.5 GB"
     */
    fun getFormattedSize(): String {
        val mb = size / (1024 * 1024)
        return if (mb > 1024) {
            String.format("%.1f GB", mb / 1024.0)
        } else {
            String.format("%.1f MB", mb.toFloat())
        }
    }
}

/**
 * 克隆应用状态枚举。
 *
 * 定义克隆应用可能处于的各种状态，用于UI展示和状态管理。
 *
 * @property INSTALLING 安装中，应用正在被克隆安装
 * @property INSTALLED 已安装，应用已成功克隆但未运行
 * @property RUNNING 运行中，应用当前正在前台运行
 * @property FROZEN 已冻结，应用被禁用，无法启动
 * @property ERROR 错误状态，安装或运行过程中发生错误
 * @property UNINSTALLING 卸载中，应用正在被删除
 */
enum class ClonedAppState {
    INSTALLING,
    INSTALLED,
    RUNNING,
    FROZEN,
    ERROR,
    UNINSTALLING
}

/**
 * 克隆应用扩展信息数据类。
 *
 * 包装 [ClonedApplicationInformationModel]，添加运行时状态信息，
 * 用于在UI层展示完整的应用状态。
 *
 * ## 使用示例
 * ```kotlin
 * val appInfo = ClonedAppInfo(
 *     clonedApp = clonedApp,
 *     state = ClonedAppState.INSTALLED,
 *     progress = 100
 * )
 *
 * when (appInfo.state) {
 *     ClonedAppState.INSTALLING -> showProgress(appInfo.progress)
 *     ClonedAppState.ERROR -> showError(appInfo.errorMessage)
 *     else -> showApp(appInfo.clonedApp)
 * }
 * ```
 *
 * @property clonedApp 克隆应用的基础信息模型
 * @property state 当前状态，默认为 [ClonedAppState.INSTALLED]
 * @property progress 安装/卸载进度，范围0-100，用于显示进度条
 * @property errorMessage 错误信息，当状态为 [ClonedAppState.ERROR] 时包含具体错误描述
 *
 * @see ClonedApplicationInformationModel
 * @see ClonedAppState
 */
data class ClonedAppInfo(
    val clonedApp: ClonedApplicationInformationModel,
    val state: ClonedAppState = ClonedAppState.INSTALLED,
    val progress: Int = 0,
    val errorMessage: String? = null
)

/**
 * 可安装应用数据类。
 *
 * 代表设备上已安装但尚未克隆的应用，用于在应用列表中展示可供克隆的应用。
 * 通常从系统的 PackageManager 获取已安装应用列表后转换为该模型。
 *
 * ## 使用示例
 * ```kotlin
 * // 在应用选择列表中展示
 * installableApps.forEach { app ->
 *     if (app.isAlreadyCloned) {
 *         showClonedBadge(app.cloneCount)
 *     }
 *     showAppIcon(app.appIcon)
 *     showAppName(app.appName)
 * }
 * ```
 *
 * @property packageName 应用包名，唯一标识符
 * @property appName 应用显示名称
 * @property appIcon 应用图标Drawable对象，可直接用于UI显示，可能为null
 * @property versionName 应用版本名称字符串
 * @property versionCode 应用版本号
 * @property isSystemApp 是否为系统预装应用
 * @property size 应用占用存储空间大小，单位为字节
 * @property isAlreadyCloned 是否已在当前配置文件中克隆
 * @property cloneCount 已克隆的数量，用于显示"已克隆X个"标签
 *
 * @see ClonedApplicationInformationModel
 */
data class InstallableApp(
    val packageName: String,
    val appName: String,
    val appIcon: android.graphics.drawable.Drawable?,
    val versionName: String?,
    val versionCode: Long,
    val isSystemApp: Boolean = false,
    val size: Long = 0L,
    val isAlreadyCloned: Boolean = false,
    val cloneCount: Int = 0
)
