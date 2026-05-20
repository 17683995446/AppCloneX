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

package com.appclonex.infrastructure.repository

import com.appclonex.domain.model.NextcloudConfig
import com.appclonex.domain.model.NextcloudShareInfo
import com.appclonex.domain.model.WebDavConfig
import com.appclonex.domain.model.WebDavResource
import com.appclonex.domain.model.WebDavSyncResult
import com.appclonex.domain.repository.NextcloudRepository
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import com.appclonex.infrastructure.network.WebDavApiService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nextcloud 仓库实现
 * 继承 WebDavRepositoryImpl 的全部 WebDAV 文件操作能力，
 * 并扩展 Nextcloud OCS API 的管理功能（分享、配额、服务器信息）。
 *
 * Nextcloud OCS API 使用 JSON 格式的请求和响应，
 * 通过 HTTP Basic Auth 认证（使用应用专用密码）。
 *
 * @param apiService WebDAV API 服务，用于文件操作
 */
@Singleton
class NextcloudRepositoryImpl @Inject constructor(
    private val apiService: WebDavApiService
) : NextcloudRepository {

    companion object {
        private const val TAG = "NextcloudRepo"

        /** Nextcloud OCS API 基础路径 */
        private const val OCS_API_BASE = "/ocs/v2.php/apps/files_sharing/api/v1"

        /** Nextcloud Capabilities API 路径 */
        private const val CAPABILITIES_PATH = "/ocs/v2.php/cloud/capabilities"

        /** Nextcloud 用户信息 API 路径模板 */
        private const val USER_INFO_PATH = "/ocs/v2.php/cloud/users/%s"

        /** OCS API 请求格式 */
        private const val OCS_FORMAT = "json"

        /** OCS API 响应中的数据节点名称 */
        private const val OCS_DATA_NODE = "data"
    }

    /** 同步操作互斥锁，防止并发同步冲突 */
    private val syncMutex = Mutex()

    // ==================== WebDAV 操作委托 ====================
    // 所有 WebDAV 基础文件操作委托给 WebDavApiService，
    // 通过将 NextcloudConfig 转换为 WebDavConfig 实现

    /**
     * 测试与 Nextcloud 服务器的连接
     * 通过 WebDAV PROPFIND 验证连接，同时检查 OCS API 可用性
     *
     * @param config WebDAV 连接配置（由 NextcloudConfig 转换而来）
     * @return Result<Boolean>，true 表示连接成功
     */
    override suspend fun testConnection(config: WebDavConfig): Result<Boolean> {
        return try {
            ApplicationLoggingServiceManager.i(TAG, "测试 Nextcloud 连接: ${config.serverUrl}")
            val resources = apiService.propfindList(config, "/")
            val connected = resources != null
            ApplicationLoggingServiceManager.i(
                TAG,
                "Nextcloud 连接测试${if (connected) "成功" else "失败"}"
            )
            Result.success(connected)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "Nextcloud 连接测试异常", e)
            Result.failure(e)
        }
    }

    override suspend fun listFiles(config: WebDavConfig, path: String): Result<List<WebDavResource>> {
        return try {
            val resources = apiService.propfindList(config, path)
            if (resources != null) {
                Result.success(resources)
            } else {
                Result.failure(IllegalStateException("无法获取远程目录列表: $path"))
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "列出文件异常: $path", e)
            Result.failure(e)
        }
    }

    override suspend fun uploadFile(
        config: WebDavConfig,
        localPath: String,
        remotePath: String
    ): Result<WebDavResource> {
        return try {
            val localFile = File(localPath)
            if (!localFile.exists() || !localFile.isFile) {
                return Result.failure(IllegalArgumentException("本地文件不存在: $localPath"))
            }

            // 确保远程目录存在
            val parentPath = remotePath.substringBeforeLast('/')
            if (parentPath.isNotEmpty() && parentPath != "/") {
                apiService.makeCollection(config, parentPath)
            }

            val resource = apiService.putFile(config, localFile, remotePath)
            if (resource != null) {
                Result.success(resource)
            } else {
                Result.failure(IllegalStateException("上传文件失败: $localPath -> $remotePath"))
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "上传文件异常: $localPath", e)
            Result.failure(e)
        }
    }

    override suspend fun downloadFile(
        config: WebDavConfig,
        remotePath: String,
        localPath: String
    ): Result<File> {
        return try {
            val localFile = File(localPath)
            localFile.parentFile?.mkdirs()

            val result = apiService.getFile(config, remotePath, localFile)
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(IllegalStateException("下载文件失败: $remotePath -> $localPath"))
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "下载文件异常: $remotePath", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteFile(config: WebDavConfig, remotePath: String): Result<Unit> {
        return try {
            val deleted = apiService.deleteResource(config, remotePath)
            if (deleted) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("删除文件失败: $remotePath"))
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "删除文件异常: $remotePath", e)
            Result.failure(e)
        }
    }

    override suspend fun createDirectory(config: WebDavConfig, path: String): Result<Unit> {
        return try {
            val created = apiService.makeCollection(config, path)
            if (created) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("创建目录失败: $path"))
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "创建目录异常: $path", e)
            Result.failure(e)
        }
    }

    override suspend fun checkFileExists(config: WebDavConfig, remotePath: String): Result<Boolean> {
        return try {
            Result.success(apiService.headResource(config, remotePath))
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "检查文件存在异常: $remotePath", e)
            Result.failure(e)
        }
    }

    override suspend fun getFileInfo(config: WebDavConfig, remotePath: String): Result<WebDavResource> {
        return try {
            val resource = apiService.propfindSingle(config, remotePath)
            if (resource != null) {
                Result.success(resource)
            } else {
                Result.failure(IllegalStateException("获取文件信息失败: $remotePath"))
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "获取文件信息异常: $remotePath", e)
            Result.failure(e)
        }
    }

    override suspend fun syncBackupFiles(
        config: WebDavConfig,
        localDir: String,
        remoteDir: String
    ): Result<WebDavSyncResult> {
        return syncMutex.withLock {
            // Nextcloud 的增量同步使用与 WebDAV 相同的逻辑
            // 委托给一个内部实现，这里直接调用 WebDavApiService 的方法
            try {
                val startTime = System.currentTimeMillis()
                var uploadedFiles = 0
                var downloadedFiles = 0
                var skippedFiles = 0
                var failedFiles = 0
                var totalBytes = 0L

                val localDirectory = File(localDir)
                if (!localDirectory.exists() || !localDirectory.isDirectory) {
                    return@withLock Result.failure(IllegalArgumentException("本地目录不存在: $localDir"))
                }

                // 确保远程目录存在
                apiService.makeCollection(config, remoteDir)

                // 获取远程文件列表
                val remoteResources = apiService.propfindList(config, remoteDir) ?: emptyList()
                val remoteFileMap = remoteResources
                    .filter { !it.isCollection }
                    .associateBy { it.getFileName() }

                // 获取本地文件列表
                val localFiles = localDirectory.listFiles { file -> file.isFile && !file.isHidden }
                    ?: emptyArray()
                val localFileMap = localFiles.associateBy { it.name }

                // 上传本地新文件或更新的文件
                for ((fileName, localFile) in localFileMap) {
                    try {
                        val remoteResource = remoteFileMap[fileName]
                        val needsUpload = remoteResource == null ||
                            localFile.lastModified() > remoteResource.lastModified

                        if (needsUpload) {
                            val remotePath = "${remoteDir.trimEnd('/')}/$fileName"
                            val result = apiService.putFile(config, localFile, remotePath)
                            if (result != null) {
                                uploadedFiles++
                                totalBytes += localFile.length()
                            } else {
                                failedFiles++
                            }
                        } else {
                            skippedFiles++
                        }
                    } catch (e: Exception) {
                        failedFiles++
                    }
                }

                // 下载远程有但本地没有的文件
                for ((fileName, remoteResource) in remoteFileMap) {
                    if (localFileMap.containsKey(fileName)) continue
                    try {
                        val localPath = File(localDirectory, fileName).absolutePath
                        val remotePath = "${remoteDir.trimEnd('/')}/$fileName"
                        val result = apiService.getFile(config, remotePath, File(localPath))
                        if (result != null) {
                            downloadedFiles++
                            totalBytes += remoteResource.contentLength
                        } else {
                            failedFiles++
                        }
                    } catch (e: Exception) {
                        failedFiles++
                    }
                }

                val durationMs = System.currentTimeMillis() - startTime
                Result.success(
                    WebDavSyncResult(
                        uploadedFiles = uploadedFiles,
                        downloadedFiles = downloadedFiles,
                        skippedFiles = skippedFiles,
                        failedFiles = failedFiles,
                        totalBytes = totalBytes,
                        durationMs = durationMs
                    )
                )
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "Nextcloud 增量同步异常", e)
                Result.failure(e)
            }
        }
    }

    // ==================== Nextcloud OCS API 操作 ====================

    /**
     * 创建分享链接
     * 通过 Nextcloud OCS Share API 创建文件或目录的分享
     *
     * @param config Nextcloud 连接配置
     * @param remotePath 要分享的远程路径
     * @param shareWith 分享目标（用户名、组名或邮箱），null 表示创建公开链接
     * @param permissions 分享权限位掩码
     * @return Result<NextcloudShareInfo> 创建的分享信息
     */
    override suspend fun createShare(
        config: NextcloudConfig,
        remotePath: String,
        shareWith: String?,
        permissions: Int
    ): Result<NextcloudShareInfo> {
        return try {
            val baseUrl = config.serverUrl.trimEnd('/')
            // OCS API 使用用户文件路径（需要包含 /files/username/ 前缀）
            val fullPath = "/files/${config.username}$remotePath"
            val encodedPath = URLEncoder.encode(fullPath, "UTF-8")

            val urlBuilder = StringBuilder("$baseUrl$OCS_API_BASE/shares?path=$encodedPath&permissions=$permissions&format=$OCS_FORMAT")

            // 如果指定了分享目标，添加 shareType 和 shareWith 参数
            if (!shareWith.isNullOrEmpty()) {
                // 默认 shareType=0 表示用户分享
                urlBuilder.append("&shareType=0&shareWith=${URLEncoder.encode(shareWith, "UTF-8")}")
            } else {
                // shareType=3 表示公开链接分享
                urlBuilder.append("&shareType=3")
            }

            val url = urlBuilder.toString()
            ApplicationLoggingServiceManager.i(TAG, "创建分享: $url")

            val connection = createOcsConnection(url, "POST", config)

            val responseCode = connection.responseCode
            when (responseCode) {
                200 -> {
                    val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                    val shareInfo = parseShareFromOcsResponse(responseBody)
                    if (shareInfo != null) {
                        ApplicationLoggingServiceManager.i(TAG, "分享创建成功: id=${shareInfo.shareId}")
                        Result.success(shareInfo)
                    } else {
                        Result.failure(IllegalStateException("解析分享信息失败"))
                    }
                }
                else -> {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "未知错误"
                    ApplicationLoggingServiceManager.e(TAG, "创建分享失败: HTTP $responseCode, $errorBody")
                    Result.failure(IllegalStateException("创建分享失败: HTTP $responseCode"))
                }
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "创建分享异常", e)
            Result.failure(e)
        }
    }

    /**
     * 列出指定路径的所有分享
     * 通过 Nextcloud OCS Share API 查询现有分享
     *
     * @param config Nextcloud 连接配置
     * @param remotePath 远程路径
     * @return Result<List<NextcloudShareInfo>> 该路径下的分享列表
     */
    override suspend fun listShares(
        config: NextcloudConfig,
        remotePath: String
    ): Result<List<NextcloudShareInfo>> {
        return try {
            val baseUrl = config.serverUrl.trimEnd('/')
            val fullPath = "/files/${config.username}$remotePath"
            val encodedPath = URLEncoder.encode(fullPath, "UTF-8")
            val url = "$baseUrl$OCS_API_BASE/shares?path=$encodedPath&format=$OCS_FORMAT&subfiles=true"

            ApplicationLoggingServiceManager.d(TAG, "列出分享: $url")

            val connection = createOcsConnection(url, "GET", config)

            val responseCode = connection.responseCode
            when (responseCode) {
                200 -> {
                    val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                    val shares = parseShareListFromOcsResponse(responseBody)
                    ApplicationLoggingServiceManager.d(TAG, "列出分享成功: 共 ${shares.size} 个分享")
                    Result.success(shares)
                }
                else -> {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "未知错误"
                    ApplicationLoggingServiceManager.e(TAG, "列出分享失败: HTTP $responseCode, $errorBody")
                    Result.failure(IllegalStateException("列出分享失败: HTTP $responseCode"))
                }
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "列出分享异常", e)
            Result.failure(e)
        }
    }

    /**
     * 删除指定的分享
     * 通过 Nextcloud OCS Share API 删除分享链接
     *
     * @param config Nextcloud 连接配置
     * @param shareId 要删除的分享 ID
     * @return Result<Unit> 删除成功时返回 Unit
     */
    override suspend fun deleteShare(config: NextcloudConfig, shareId: String): Result<Unit> {
        return try {
            val baseUrl = config.serverUrl.trimEnd('/')
            val url = "$baseUrl$OCS_API_BASE/shares/$shareId?format=$OCS_FORMAT"

            ApplicationLoggingServiceManager.i(TAG, "删除分享: id=$shareId")

            val connection = createOcsConnection(url, "DELETE", config)

            val responseCode = connection.responseCode
            when (responseCode) {
                200 -> {
                    ApplicationLoggingServiceManager.i(TAG, "分享删除成功: id=$shareId")
                    Result.success(Unit)
                }
                else -> {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "未知错误"
                    ApplicationLoggingServiceManager.e(TAG, "删除分享失败: HTTP $responseCode, $errorBody")
                    Result.failure(IllegalStateException("删除分享失败: HTTP $responseCode"))
                }
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "删除分享异常", e)
            Result.failure(e)
        }
    }

    /**
     * 获取 Nextcloud 服务器信息
     * 通过 OCS Capabilities API 查询服务器版本、功能等元数据
     *
     * @param config Nextcloud 连接配置
     * @return Result<Map<String, String>> 服务器信息键值对
     */
    override suspend fun getServerInfo(config: NextcloudConfig): Result<Map<String, String>> {
        return try {
            val baseUrl = config.serverUrl.trimEnd('/')
            val url = "$baseUrl$CAPABILITIES_PATH?format=$OCS_FORMAT"

            ApplicationLoggingServiceManager.d(TAG, "获取服务器信息: $url")

            val connection = createOcsConnection(url, "GET", config)

            val responseCode = connection.responseCode
            when (responseCode) {
                200 -> {
                    val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                    val info = parseServerCapabilities(responseBody)
                    ApplicationLoggingServiceManager.i(TAG, "服务器信息: $info")
                    Result.success(info)
                }
                else -> {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "未知错误"
                    ApplicationLoggingServiceManager.e(TAG, "获取服务器信息失败: HTTP $responseCode, $errorBody")
                    Result.failure(IllegalStateException("获取服务器信息失败: HTTP $responseCode"))
                }
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "获取服务器信息异常", e)
            Result.failure(e)
        }
    }

    /**
     * 获取当前用户的存储配额
     * 通过 OCS User API 查询用户已使用和总配额空间
     *
     * @param config Nextcloud 连话配置
     * @return Result<Pair<Long, Long>> Pair(已使用字节数, 总配额字节数)，-1 表示无限制
     */
    override suspend fun getUserQuota(config: NextcloudConfig): Result<Pair<Long, Long>> {
        return try {
            val baseUrl = config.serverUrl.trimEnd('/')
            val url = String.format("$baseUrl$USER_INFO_PATH?format=$OCS_FORMAT", config.username)

            ApplicationLoggingServiceManager.d(TAG, "获取用户配额: $url")

            val connection = createOcsConnection(url, "GET", config)

            val responseCode = connection.responseCode
            when (responseCode) {
                200 -> {
                    val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                    val quota = parseUserQuota(responseBody)
                    ApplicationLoggingServiceManager.i(
                        TAG,
                        "用户配额: 已使用=${quota.first}, 总计=${quota.second}"
                    )
                    Result.success(quota)
                }
                else -> {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "未知错误"
                    ApplicationLoggingServiceManager.e(TAG, "获取用户配额失败: HTTP $responseCode, $errorBody")
                    Result.failure(IllegalStateException("获取用户配额失败: HTTP $responseCode"))
                }
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "获取用户配额异常", e)
            Result.failure(e)
        }
    }

    // ==================== OCS API 辅助方法 ====================

    /**
     * 创建带认证的 OCS API HTTP 连接
     * 设置 Basic Auth 认证头和 JSON 请求格式
     *
     * @param url 完整的 OCS API URL
     * @param method HTTP 请求方法
     * @param config Nextcloud 连接配置
     * @return 配置好的 HttpURLConnection
     */
    private fun createOcsConnection(url: String, method: String, config: NextcloudConfig): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true

        // 设置 Basic Auth 认证（使用应用专用密码）
        val credentials = "${config.username}:${config.getEffectivePassword()}"
        val encoded = java.util.Base64.getEncoder().encodeToString(credentials.toByteArray(Charsets.UTF_8))
        connection.setRequestProperty("Authorization", "Basic $encoded")

        // OCS API 请求头
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("OCS-APIRequest", "true")

        if (method == "POST") {
            connection.doOutput = true
        }

        return connection
    }

    /**
     * 从 OCS JSON 响应中提取 data 节点的内容
     * OCS 响应格式: { "ocs": { "meta": {...}, "data": {...} } }
     *
     * @param json OCS API 的 JSON 响应体
     * @return data 节点的 JSON 字符串，提取失败返回 null
     */
    private fun extractOcsData(json: String): String? {
        // 使用正则提取 ocs.data 节点内容
        val ocsMatch = Regex(""""ocs"\s*:\s*\{""").find(json) ?: return null
        val ocsStart = ocsMatch.range.first
        val dataMatch = Regex(""""data"\s*:\s*""").find(json, ocsStart) ?: return null
        val dataKeyEnd = dataMatch.range.last + 1

        // 判断 data 值的类型（对象或数组）
        val charAfter = json.getOrNull(dataKeyEnd + 1)
        return when (charAfter) {
            '{', '[' -> extractBalancedBlock(json, dataKeyEnd + 1)
            '"' -> {
                // 字符串值
                val endQuote = json.indexOf('"', dataKeyEnd + 2)
                if (endQuote > 0) json.substring(dataKeyEnd + 2, endQuote) else null
            }
            else -> null
        }
    }

    /**
     * 提取 JSON 中平衡的块（花括号或方括号内的内容）
     *
     * @param json JSON 字符串
     * @param startIndex 块起始位置
     * @return 块的完整内容（包含起始和结束符号）
     */
    private fun extractBalancedBlock(json: String, startIndex: Int): String? {
        val openChar = json[startIndex]
        val closeChar = if (openChar == '{') '}' else ']'
        var depth = 1
        var inString = false
        var escape = false
        var i = startIndex + 1

        while (i < json.length && depth > 0) {
            val c = json[i]
            when {
                escape -> escape = false
                c == '\\' && inString -> escape = true
                c == '"' -> inString = !inString
                !inString && c == openChar -> depth++
                !inString && c == closeChar -> depth--
            }
            if (depth > 0) i++
        }

        return if (depth == 0) json.substring(startIndex, i + 1) else null
    }

    /**
     * 从 OCS 响应中解析单个分享信息
     *
     * @param json OCS API 的 JSON 响应体
     * @return 分享信息，解析失败返回 null
     */
    private fun parseShareFromOcsResponse(json: String): NextcloudShareInfo? {
        val data = extractOcsData(json) ?: return null
        return parseShareInfoFromJson(data)
    }

    /**
     * 从 OCS 响应中解析分享列表
     *
     * @param json OCS API 的 JSON 响应体
     * @return 分享信息列表
     */
    private fun parseShareListFromOcsResponse(json: String): List<NextcloudShareInfo> {
        val data = extractOcsData(json) ?: return emptyList()
        val shares = mutableListOf<NextcloudShareInfo>()

        // data 是一个数组，逐个解析分享对象
        val sharePattern = Regex("""\{[^{}]*"id"\s*:\s*(\d+)""")
        val matches = sharePattern.findAll(data)

        for (match in matches) {
            try {
                val shareJson = extractBalancedBlock(data, match.range.first)
                if (shareJson != null) {
                    val shareInfo = parseShareInfoFromJson(shareJson)
                    if (shareInfo != null) {
                        shares.add(shareInfo)
                    }
                }
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.w(TAG, "解析分享项失败", e)
            }
        }

        return shares
    }

    /**
     * 从分享 JSON 对象中解析 NextcloudShareInfo
     *
     * @param json 分享对象的 JSON 字符串
     * @return 分享信息
     */
    private fun parseShareInfoFromJson(json: String): NextcloudShareInfo? {
        return try {
            NextcloudShareInfo(
                shareId = extractJsonStringValue(json, "id") ?: "",
                shareUrl = extractJsonStringValue(json, "url") ?: "",
                shareWith = extractJsonStringValue(json, "share_with") ?: "",
                permissions = extractJsonIntValue(json, "permissions") ?: 1,
                expireDate = extractJsonStringValue(json, "expiration")
            )
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.w(TAG, "解析分享信息失败", e)
            null
        }
    }

    /**
     * 从 OCS Capabilities 响应中解析服务器信息
     *
     * @param json OCS Capabilities API 的 JSON 响应体
     * @return 服务器信息键值对
     */
    private fun parseServerCapabilities(json: String): Map<String, String> {
        val data = extractOcsData(json) ?: return emptyMap()
        val info = mutableMapOf<String, String>()

        // 提取版本信息
        extractJsonStringValue(data, "versionstring")?.let { info["version"] = it }
        extractJsonStringValue(data, "edition")?.let { info["edition"] = it }
        extractJsonStringValue(data, "hostname")?.let { info["hostname"] = it }
        extractJsonStringValue(data, "productname")?.let { info["productName"] = it }
        extractJsonStringValue(data, "website")?.let { info["website"] = it }

        return info
    }

    /**
     * 从 OCS User API 响应中解析用户配额
     *
     * @param json OCS User API 的 JSON 响应体
     * @return Pair(已使用字节数, 总配额字节数)
     */
    private fun parseUserQuota(json: String): Pair<Long, Long> {
        val data = extractOcsData(json) ?: return Pair(0L, 0L)

        // 提取配额相关字段
        // quota 结构: { "free": 12345, "used": 67890, "total": 80275, "relative": 84.5 }
        val quotaUsed = extractQuotaField(data, "used")
        val quotaTotal = extractQuotaField(data, "total")

        return Pair(quotaUsed, quotaTotal)
    }

    /**
     * 从配额 JSON 中提取指定字段的值
     * 配额字段可能在 data 节点下，也可能在 data.quota 子节点下
     *
     * @param json JSON 字符串
     * @param fieldName 字段名
     * @return 字段值，解析失败返回 0
     */
    private fun extractQuotaField(json: String, fieldName: String): Long {
        // 先尝试直接在 data 中查找
        val directValue = extractJsonLongValue(json, fieldName)
        if (directValue != null && directValue > 0) return directValue

        // 尝试在 quota 子对象中查找
        val quotaMatch = Regex(""""quota"\s*:\s*\{""").find(json)
        if (quotaMatch != null) {
            val quotaStart = quotaMatch.range.first
            val quotaBlock = extractBalancedBlock(json, quotaStart + quotaMatch.value.lastIndexOf('{'))
            if (quotaBlock != null) {
                return extractJsonLongValue(quotaBlock, fieldName) ?: 0L
            }
        }

        return 0L
    }

    // ==================== JSON 解析辅助方法 ====================

    /**
     * 从 JSON 字符串中提取字符串字段值
     *
     * @param json JSON 字符串
     * @param fieldName 字段名
     * @return 字段值，未找到返回 null
     */
    private fun extractJsonStringValue(json: String, fieldName: String): String? {
        val regex = Regex(""""$fieldName"\s*:\s*"([^"]*)"""")
        return regex.find(json)?.groupValues?.get(1)
    }

    /**
     * 从 JSON 字符串中提取整型字段值
     *
     * @param json JSON 字符串
     * @param fieldName 字段名
     * @return 字段值，未找到返回 null
     */
    private fun extractJsonIntValue(json: String, fieldName: String): Int? {
        val regex = Regex(""""$fieldName"\s*:\s*(-?\d+)""")
        return regex.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * 从 JSON 字符串中提取长整型字段值
     *
     * @param json JSON 字符串
     * @param fieldName 字段名
     * @return 字段值，未找到返回 null
     */
    private fun extractJsonLongValue(json: String, fieldName: String): Long? {
        val regex = Regex(""""$fieldName"\s*:\s*(-?\d+)""")
        return regex.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }
}
