/*
 * Copyright (c) 2024 AppCloneX Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appclonex.domain.model

import kotlinx.serialization.Serializable

// ==================== WebDAV 认证类型 ====================

/**
 * WebDAV 认证类型枚举
 * 定义 WebDAV 服务器支持的认证方式
 */
@Serializable
enum class WebDavAuthType {
    /** HTTP Basic 认证（用户名 + 密码 Base64 编码） */
    BASIC,
    /** HTTP Digest 认证（挑战-响应模式） */
    DIGEST,
    /** OAuth2 Bearer Token 认证 */
    OAUTH2,
    /** 无认证（匿名访问） */
    NONE
}

// ==================== WebDAV 资源模型 ====================

/**
 * WebDAV 资源模型
 * 表示 WebDAV 服务器上的一个文件或目录，对应 WebDAV PROPFIND 响应中的 resource 节点
 *
 * @property href 资源的完整路径（URI 编码）
 * @property displayName 资源的显示名称
 * @property contentLength 文件大小（字节），目录时为 0
 * @property lastModified 最后修改时间戳（毫秒）
 * @property mimeType 资源的 MIME 类型，目录时为 "httpd/unix-directory"
 * @property isCollection 是否为目录（WebDAV 中称为 collection）
 * @property etag 资源的 ETag 标识，用于缓存和冲突检测
 */
@Serializable
data class WebDavResource(
    val href: String = "",
    val displayName: String = "",
    val contentLength: Long = 0L,
    val lastModified: Long = 0L,
    val mimeType: String = "",
    val isCollection: Boolean = false,
    val etag: String = ""
) {
    /**
     * 获取资源的文件名（从 href 中提取最后一段路径）
     *
     * @return 文件名字符串
     */
    fun getFileName(): String {
        if (displayName.isNotEmpty()) return displayName
        val path = href.trimEnd('/')
        val lastSlash = path.lastIndexOf('/')
        return if (lastSlash >= 0) path.substring(lastSlash + 1) else path
    }
}

// ==================== WebDAV 配置模型 ====================

/**
 * WebDAV 连接配置
 * 定义连接 WebDAV 服务器所需的全部参数
 *
 * @property serverUrl WebDAV 服务器基础 URL（如 https://dav.example.com/remote.php/dav/files/user/）
 * @property username 用户名
 * @property password 密码
 * @property basePath 服务器上的基础路径（相对于 serverUrl）
 * @property connectTimeout 连接超时时间（毫秒），默认 30 秒
 * @property readTimeout 读取超时时间（毫秒），默认 60 秒
 */
@Serializable
data class WebDavConfig(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val basePath: String = "/",
    val connectTimeout: Int = 30_000,
    val readTimeout: Int = 60_000
) {
    /**
     * 获取完整的远程路径 URL
     *
     * @param relativePath 相对于 basePath 的路径
     * @return 完整的远程 URL
     */
    fun buildFullUrl(relativePath: String): String {
        val base = serverUrl.trimEnd('/')
        val path = if (basePath.startsWith("/")) basePath else "/$basePath"
        val rel = if (relativePath.startsWith("/")) relativePath else "/$relativePath"
        return "$base$path$rel"
    }
}

// ==================== WebDAV 同步结果模型 ====================

/**
 * WebDAV 同步操作结果
 * 记录一次增量同步操作的详细统计信息
 *
 * @property uploadedFiles 成功上传的文件数量
 * @property downloadedFiles 成功下载的文件数量
 * @property skippedFiles 跳过的文件数量（无需同步）
 * @property failedFiles 失败的文件数量
 * @property totalBytes 总传输字节数
 * @property durationMs 同步操作耗时（毫秒）
 */
@Serializable
data class WebDavSyncResult(
    val uploadedFiles: Int = 0,
    val downloadedFiles: Int = 0,
    val skippedFiles: Int = 0,
    val failedFiles: Int = 0,
    val totalBytes: Long = 0L,
    val durationMs: Long = 0L
) {
    /**
     * 判断同步操作是否完全成功（无失败文件）
     *
     * @return true 表示所有文件同步成功
     */
    fun isSuccessful(): Boolean = failedFiles == 0

    /**
     * 获取同步操作的总文件数
     *
     * @return 参与同步的文件总数
     */
    fun totalFiles(): Int = uploadedFiles + downloadedFiles + skippedFiles + failedFiles
}

// ==================== Nextcloud 配置模型 ====================

/**
 * Nextcloud 连接配置
 * 继承 WebDAV 配置概念，增加 Nextcloud 特有的参数
 *
 * @property serverUrl Nextcloud 服务器基础 URL（如 https://nextcloud.example.com）
 * @property username 用户名
 * @property password 用户密码
 * @property appPassword 应用专用密码（Nextcloud 生成的 App Password）
 * @property basePath WebDAV 基础路径，默认为 Nextcloud 标准 WebDAV 路径
 */
@Serializable
data class NextcloudConfig(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val appPassword: String = "",
    val basePath: String = "/remote.php/dav/files/$username/"
) {
    /**
     * 获取用于 WebDAV 操作的有效密码
     * 优先使用应用专用密码，为空时回退到用户密码
     *
     * @return 有效密码
     */
    fun getEffectivePassword(): String = if (appPassword.isNotEmpty()) appPassword else password

    /**
     * 获取 WebDAV 基础 URL
     *
     * @return WebDAV 服务器的完整基础 URL
     */
    fun getWebDavBaseUrl(): String {
        val base = serverUrl.trimEnd('/')
        val path = if (basePath.startsWith("/")) basePath else "/$basePath"
        return "$base$path"
    }

    /**
     * 转换为 WebDavConfig 对象，用于 WebDAV 基础操作
     *
     * @return 等效的 WebDavConfig
     */
    fun toWebDavConfig(): WebDavConfig {
        return WebDavConfig(
            serverUrl = getWebDavBaseUrl(),
            username = username,
            password = getEffectivePassword(),
            basePath = "/"
        )
    }
}

// ==================== Nextcloud 分享模型 ====================

/**
 * Nextcloud 分享信息
 * 对应 Nextcloud OCS Share API 返回的分享数据
 *
 * @property shareId 分享的唯一标识符
 * @property shareUrl 分享链接 URL（公开链接分享时可用）
 * @property shareWith 分享目标（用户、组或邮箱）
 * @property permissions 分享权限（位掩码：1=读取, 2=更新, 4=创建, 8=删除, 16=共享, 31=全部）
 * @property expireDate 分享过期日期（ISO 8601 格式），null 表示永不过期
 */
@Serializable
data class NextcloudShareInfo(
    val shareId: String = "",
    val shareUrl: String = "",
    val shareWith: String = "",
    val permissions: Int = 1,
    val expireDate: String? = null
) {
    companion object {
        /** 只读权限 */
        const val PERMISSION_READ = 1
        /** 读写权限 */
        const val PERMISSION_UPDATE = 2
        /** 创建权限 */
        const val PERMISSION_CREATE = 4
        /** 删除权限 */
        const val PERMISSION_DELETE = 8
        /** 重新分享权限 */
        const val PERMISSION_SHARE = 16
        /** 全部权限 */
        const val PERMISSION_ALL = 31
    }

    /**
     * 判断分享是否包含读取权限
     *
     * @return true 表示包含读取权限
     */
    fun canRead(): Boolean = (permissions and PERMISSION_READ) != 0

    /**
     * 判断分享是否包含写入权限
     *
     * @return true 表示包含写入权限
     */
    fun canWrite(): Boolean = (permissions and PERMISSION_UPDATE) != 0

    /**
     * 判断分享是否包含删除权限
     *
     * @return true 表示包含删除权限
     */
    fun canDelete(): Boolean = (permissions and PERMISSION_DELETE) != 0
}
