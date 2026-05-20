package com.appclonex.domain.model

import kotlinx.serialization.Serializable

/**
 * 克隆模板模型
 * 预设批量克隆配置，支持内置模板和用户自定义模板
 *
 * @property id 模板唯一标识
 * @property name 模板名称
 * @property description 模板描述
 * @property icon 模板图标（emoji）
 * @property category 模板分类
 * @property appPackages 包含的应用包名列表
 * @property options 批量克隆选项
 * @property isBuiltIn 是否为内置模板
 * @property createdAt 创建时间戳
 * @property updatedAt 更新时间戳
 * @property usageCount 使用次数
 */
@Serializable
data class CloneTemplate(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val icon: String = "\uD83D\uDCE6",
    val category: TemplateCategory = TemplateCategory.GENERAL,
    val appPackages: List<String> = emptyList(),
    val options: BatchCloneOptions = BatchCloneOptions(),
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0
)

/**
 * 模板分类枚举
 */
@Serializable
enum class TemplateCategory {
    GENERAL,      // 通用
    SOCIAL,       // 社交
    WORK,         // 工作
    GAMING,       // 游戏
    PRIVACY,      // 隐私
    PRODUCTIVITY  // 生产力
}

/**
 * 内置模板集合
 * 提供预置的批量克隆配置模板
 */
object BuiltInTemplates {

    /**
     * 工作空间模板 - 常用办公应用
     */
    val workProfile = CloneTemplate(
        id = 1,
        name = "工作空间",
        description = "常用办公应用",
        icon = "\uD83D\uDCBC",
        category = TemplateCategory.WORK,
        isBuiltIn = true,
        appPackages = listOf(
            "com.tencent.mm",
            "com.alibaba.android.rimet",
            "com.tencent.wework"
        )
    )

    /**
     * 社交分身模板 - 社交应用双开
     */
    val socialClone = CloneTemplate(
        id = 2,
        name = "社交分身",
        description = "社交应用双开",
        icon = "\uD83D\uDCAC",
        category = TemplateCategory.SOCIAL,
        isBuiltIn = true,
        appPackages = listOf(
            "com.tencent.mm",
            "com.tencent.mobileqq",
            "com.sina.weibo"
        )
    )

    /**
     * 游戏空间模板 - 游戏应用隔离
     */
    val gamingProfile = CloneTemplate(
        id = 3,
        name = "游戏空间",
        description = "游戏应用隔离",
        icon = "\uD83C\uDFAE",
        category = TemplateCategory.GAMING,
        isBuiltIn = true,
        appPackages = listOf(
            "com.tencent.tmgp.sgame",
            "com.netease.my"
        )
    )

    /**
     * 隐私保险箱模板 - 私密应用保护
     */
    val privacyVault = CloneTemplate(
        id = 4,
        name = "隐私保险箱",
        description = "私密应用保护",
        icon = "\uD83D\uDD12",
        category = TemplateCategory.PRIVACY,
        isBuiltIn = true,
        appPackages = emptyList()
    )

    /**
     * 所有内置模板列表
     */
    val all: List<CloneTemplate> = listOf(
        workProfile,
        socialClone,
        gamingProfile,
        privacyVault
    )
}
