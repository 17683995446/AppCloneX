package com.appclonex.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户配置文件数据模型。
 *
 * 该数据类代表一个独立的多用户空间，用于运行分身应用。
 * 每个用户配置文件对应Android系统中的一个用户（UserHandle），
 * 实现应用分身的核心隔离机制。
 *
 * ## 核心概念
 * - **用户配置文件**：Android系统支持多用户功能，每个用户有独立的应用空间
 * - **分身隔离**：不同用户配置文件中的应用数据完全隔离，互不影响
 * - **克隆应用关联**：通过外键关联 [ClonedApplicationInformationModel]
 *
 * ## 数据库设计
 * - 使用Room持久化存储
 * - [userId] 作为主键，对应Android系统的用户ID
 *
 * ## 使用示例
 * ```kotlin
 * // 创建新的用户配置文件
 * val profile = UserProfileConfigurationModel(
 *     userId = 10,
 *     profileName = "工作空间",
 *     serialNumber = 1
 * )
 *
 * // 获取格式化的存储空间显示
 * val storageDisplay = profile.getFormattedStorage()
 *
 * // 检查配置文件状态
 * if (profile.isActive && profile.isRunning) {
 *     // 配置文件正在运行
 * }
 * ```
 *
 * @property userId Android系统用户ID，作为主键，由系统分配
 * @property profileName 配置文件显示名称，展示给用户看的友好名称
 * @property profileIcon 配置文件图标路径，存储图标文件的本地路径，可为null
 * @property createdAt 创建时间戳，毫秒级Unix时间戳
 * @property updatedAt 最后更新时间戳，毫秒级Unix时间戳
 * @property isActive 是否为当前激活的配置文件
 * @property isRunning 配置文件是否正在运行（用户空间是否已启动）
 * @property serialNumber 用户序列号，用于排序和标识
 * @property clonedAppCount 该配置文件下已克隆的应用数量
 * @property storageUsed 已使用的存储空间，单位为字节
 * @property notes 用户备注信息
 *
 * @see ClonedApplicationInformationModel
 * @see UserProfileInfo
 * @see UserProfileState
 */
@Entity(tableName = "user_profiles")
data class UserProfileConfigurationModel(
    @PrimaryKey
    val userId: Int,

    val profileName: String,
    val profileIcon: String? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val isActive: Boolean = false,
    val isRunning: Boolean = false,

    val serialNumber: Int = 0,

    val clonedAppCount: Int = 0,
    val storageUsed: Long = 0L,

    val notes: String? = null
) {
    /**
     * 将存储空间格式化为人类可读的字符串。
     *
     * 自动选择合适的单位（MB或GB）进行显示。
     * - 小于1GB时显示为 "XXX MB"
     * - 大于等于1GB时显示为 "X.X GB"
     *
     * @return 格式化后的存储空间字符串，如 "256 MB" 或 "1.5 GB"
     */
    fun getFormattedStorage(): String {
        val mb = storageUsed / (1024 * 1024)
        val gb = mb / 1024
        return if (gb > 0) {
            String.format("%.1f GB", gb.toFloat())
        } else {
            String.format("%d MB", mb)
        }
    }
}

/**
 * 用户配置文件状态枚举。
 *
 * 定义用户配置文件可能处于的各种生命周期状态，用于UI展示和状态管理。
 *
 * ## 状态转换流程
 * ```
 * CREATING -> READY -> STARTING -> RUNNING -> STOPPING -> STOPPED
 *                |                                  |
 *                +---------- ERROR <---------------+
 * ```
 *
 * @property CREATING 创建中，正在创建新的用户空间
 * @property READY 就绪状态，用户空间已创建但未启动
 * @property STARTING 启动中，正在启动用户空间
 * @property RUNNING 运行中，用户空间已启动，可以运行应用
 * @property STOPPING 停止中，正在停止用户空间
 * @property STOPPED 已停止，用户空间已停止
 * @property ERROR 错误状态，操作过程中发生错误
 */
enum class UserProfileState {
    CREATING,
    READY,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    ERROR
}

/**
 * 用户配置文件扩展信息数据类。
 *
 * 包装 [UserProfileConfigurationModel]，添加运行时状态信息，
 * 用于在UI层展示完整的配置文件状态。
 *
 * ## 使用示例
 * ```kotlin
 * val profileInfo = UserProfileInfo(
 *     profile = profile,
 *     state = UserProfileState.RUNNING,
 *     runningApps = listOf("com.example.app1", "com.example.app2")
 * )
 *
 * when (profileInfo.state) {
 *     UserProfileState.CREATING -> showCreatingProgress()
 *     UserProfileState.RUNNING -> showRunningApps(profileInfo.runningApps)
 *     UserProfileState.ERROR -> showError(profileInfo.errorMessage)
 *     else -> showProfile(profileInfo.profile)
 * }
 * ```
 *
 * @property profile 用户配置文件的基础信息模型
 * @property state 当前状态，默认为 [UserProfileState.READY]
 * @property errorMessage 错误信息，当状态为 [UserProfileState.ERROR] 时包含具体错误描述
 * @property runningApps 正在运行的应用包名列表，用于显示运行中的应用数量
 *
 * @see UserProfileConfigurationModel
 * @see UserProfileState
 */
data class UserProfileInfo(
    val profile: UserProfileConfigurationModel,
    val state: UserProfileState = UserProfileState.READY,
    val errorMessage: String? = null,
    val runningApps: List<String> = emptyList()
)
