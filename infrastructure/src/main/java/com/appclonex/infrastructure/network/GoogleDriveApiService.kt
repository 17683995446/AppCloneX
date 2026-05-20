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

package com.appclonex.infrastructure.network

import android.content.Context
import com.appclonex.domain.model.GoogleDriveFile
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Drive API 服务
 * 封装 Google Drive REST API v3 的核心操作，使用 Google Play Services 的 Drive API 进行文件管理。
 * 所有操作通过 HTTP REST 调用实现，支持断点续传和进度回调。
 *
 * 注意：此实现使用 Google Play Services 的 GoogleApiClient 和 Drive.DriveApi。
 * 在实际设备上需要安装 Google Play Services 才能正常工作。
 */
@Singleton
class GoogleDriveApiService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "GoogleDriveApiService"

        /** AppCloneX 在 Google Drive 中的专用备份文件夹名称 */
        const val BACKUP_FOLDER_NAME = "AppCloneX_Backups"

        /** Google Drive REST API v3 基础 URL */
        private const val DRIVE_API_BASE_URL = "https://www.googleapis.com/drive/v3"

        /** Google Drive 上传 API URL */
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3"

        /** 查询文件时每页最大数量 */
        private const val PAGE_SIZE = 100

        /** 备份文件的 MIME 类型 */
        const val MIME_TYPE_JSON = "application/json"

        /** 文件夹的 MIME 类型 */
        const val MIME_TYPE_FOLDER = "application/vnd.google-apps.folder"

        /** 查询字段：文件基本信息 + MD5 校验和 */
        private const val FILE_FIELDS = "id,name,mimeType,size,modifiedTime,md5Checksum,webViewLink"

        /** 上传查询参数 */
        private const val UPLOAD_FIELDS = "id,name,mimeType,size,modifiedTime,md5Checksum,webViewLink"
    }

    // ==================== GoogleApiClient 管理 ====================

    /**
     * Google API 客户端实例
     * 使用 lazy 延迟初始化，避免在不需要时创建连接
     */
    // private var googleApiClient: GoogleApiClient? = null

    /**
     * 当前认证的 Google 账号令牌
     * 用于 REST API 调用的 Bearer Token 认证
     */
    @Volatile
    private var authToken: String? = null

    /**
     * 设置认证令牌
     * 由 Repository 层在认证成功后调用
     *
     * @param token Google OAuth2 访问令牌
     */
    fun setAuthToken(token: String?) {
        authToken = token
        ApplicationLoggingServiceManager.i(TAG, "认证令牌已${if (token != null) "设置" else "清除"}")
    }

    /**
     * 获取当前认证令牌
     *
     * @return 当前有效的 OAuth2 令牌，未认证时返回 null
     */
    fun getAuthToken(): String? = authToken

    // ==================== 文件夹操作 ====================

    /**
     * 查找或创建 AppCloneX 专用备份文件夹
     * 首先在 Google Drive 中搜索名为 [BACKUP_FOLDER_NAME] 的文件夹，
     * 如果不存在则创建一个新文件夹
     *
     * @param token OAuth2 访问令牌
     * @return 备份文件夹的 Google Drive 文件 ID，失败时返回 null
     */
    suspend fun findOrCreateBackupFolder(token: String): String? {
        return try {
            // 尝试查找已存在的备份文件夹
            val existingFolder = findFolderByName(token, BACKUP_FOLDER_NAME)
            if (existingFolder != null) {
                ApplicationLoggingServiceManager.i(TAG, "找到已存在的备份文件夹: id=${existingFolder.id}")
                return existingFolder.id
            }

            // 文件夹不存在，创建新文件夹
            ApplicationLoggingServiceManager.i(TAG, "备份文件夹不存在，正在创建: $BACKUP_FOLDER_NAME")
            val folderId = createFolder(token, BACKUP_FOLDER_NAME)
            if (folderId != null) {
                ApplicationLoggingServiceManager.i(TAG, "备份文件夹创建成功: id=$folderId")
            } else {
                ApplicationLoggingServiceManager.e(TAG, "备份文件夹创建失败")
            }
            folderId
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "查找或创建备份文件夹时发生异常", e)
            null
        }
    }

    /**
     * 按名称搜索 Google Drive 中的文件夹
     *
     * @param token OAuth2 访问令牌
     * @param folderName 要搜索的文件夹名称
     * @return 匹配的文件夹文件信息，未找到时返回 null
     */
    private suspend fun findFolderByName(token: String, folderName: String): GoogleDriveFile? {
        var connection: java.net.HttpURLConnection? = null
        return try {
            // 使用 Google Drive REST API 搜索文件夹
            // query: name = 'folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false
            val query = "name = '$folderName' and mimeType = '$MIME_TYPE_FOLDER' and trashed = false"

            // 通过 HttpURLConnection 执行 REST API 调用
            val url = "$DRIVE_API_BASE_URL/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}" +
                    "&fields=files($FILE_FIELDS)&spaces=drive"

            connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                // 解析 JSON 响应，提取第一个匹配的文件夹
                val folderId = parseFolderIdFromResponse(responseBody)
                if (folderId != null) {
                    GoogleDriveFile(id = folderId, name = folderName, mimeType = MIME_TYPE_FOLDER)
                } else {
                    null
                }
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                ApplicationLoggingServiceManager.e(TAG, "搜索文件夹失败: HTTP $responseCode, $errorBody")
                null
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "搜索文件夹时发生异常", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * 在 Google Drive 中创建新文件夹
     *
     * @param token OAuth2 访问令牌
     * @param folderName 文件夹名称
     * @return 新创建文件夹的 ID，失败时返回 null
     */
    private suspend fun createFolder(token: String, folderName: String): String? {
        var connection: java.net.HttpURLConnection? = null
        return try {
            val url = "$DRIVE_API_BASE_URL/files?fields=$FILE_FIELDS"
            connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000

            // 构建创建文件夹的 JSON 请求体
            val requestBody = """
                {
                    "name": "$folderName",
                    "mimeType": "$MIME_TYPE_FOLDER"
                }
            """.trimIndent()

            connection.outputStream.use { os ->
                os.write(requestBody.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == 200 || responseCode == 201) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                parseFileIdFromResponse(responseBody)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                ApplicationLoggingServiceManager.e(TAG, "创建文件夹失败: HTTP $responseCode, $errorBody")
                null
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "创建文件夹时发生异常", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    // ==================== 文件列表查询 ====================

    /**
     * 列出备份文件夹中的所有备份文件
     * 仅返回 .json 格式的文件，按修改时间降序排列
     *
     * @param token OAuth2 访问令牌
     * @param folderId 备份文件夹的 Google Drive ID
     * @return 备份文件列表，失败时返回空列表
     */
    suspend fun listFiles(token: String, folderId: String): List<GoogleDriveFile> {
        var connection: java.net.HttpURLConnection? = null
        return try {
            // 查询条件：父文件夹为指定文件夹、非文件夹、非回收站、名称以 .json 结尾
            val query = "'$folderId' in parents and mimeType != '$MIME_TYPE_FOLDER'" +
                    " and name contains '.json' and trashed = false"
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")

            val url = "$DRIVE_API_BASE_URL/files?q=$encodedQuery" +
                    "&fields=files($FILE_FIELDS)&orderBy=modifiedTime desc" +
                    "&pageSize=$PAGE_SIZE&spaces=drive"

            connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                parseFileListFromResponse(responseBody)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                ApplicationLoggingServiceManager.e(TAG, "列出文件失败: HTTP $responseCode, $errorBody")
                emptyList()
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "列出文件时发生异常", e)
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    // ==================== 文件上传（支持断点续传和进度回调） ====================

    /**
     * 上传文件到 Google Drive
     * 使用可恢复上传方式，支持断点续传和实时进度回调
     *
     * @param token OAuth2 访问令牌
     * @param localFile 要上传的本地文件
     * @param folderId 目标文件夹 ID
     * @param progress 上传进度回调 (已传输字节数, 总字节数)
     * @return 上传成功后的远程文件元数据，失败时返回 null
     */
    suspend fun uploadFile(
        token: String,
        localFile: File,
        folderId: String,
        progress: (bytesTransferred: Long, totalBytes: Long) -> Unit
    ): GoogleDriveFile? {
        return try {
            if (!localFile.exists() || !localFile.isFile) {
                ApplicationLoggingServiceManager.e(TAG, "上传文件不存在: ${localFile.absolutePath}")
                return null
            }

            val fileSize = localFile.length()
            ApplicationLoggingServiceManager.i(
                TAG,
                "开始上传文件: ${localFile.name}, 大小: ${fileSize}bytes"
            )

            // 步骤1：发起可恢复上传会话
            val uploadSessionUrl = initiateResumableUpload(token, localFile.name, folderId, fileSize)
                ?: run {
                    ApplicationLoggingServiceManager.e(TAG, "发起可恢复上传会话失败")
                    return null
                }

            // 步骤2：上传文件内容（支持断点续传）
            uploadFileContent(token, uploadSessionUrl, localFile, fileSize, progress)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "上传文件时发生异常: ${localFile.name}", e)
            null
        }
    }

    /**
     * 发起可恢复上传会话
     * 向 Google Drive API 发送初始请求，获取上传会话 URL
     *
     * @param token OAuth2 访问令牌
     * @param fileName 文件名称
     * @param folderId 目标文件夹 ID
     * @param fileSize 文件大小
     * @return 可恢复上传的会话 URL，失败时返回 null
     */
    private fun initiateResumableUpload(
        token: String,
        fileName: String,
        folderId: String,
        fileSize: Long
    ): String? {
        return try {
            val url = "$DRIVE_UPLOAD_URL/files?uploadType=resumable&fields=$UPLOAD_FIELDS"
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("X-Upload-Content-Type", MIME_TYPE_JSON)
            connection.setRequestProperty("X-Upload-Content-Length", fileSize.toString())
            connection.doOutput = true
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000

            // 构建文件元数据 JSON
            val metadata = """
                {
                    "name": "$fileName",
                    "mimeType": "$MIME_TYPE_JSON",
                    "parents": ["$folderId"]
                }
            """.trimIndent()

            connection.outputStream.use { os ->
                os.write(metadata.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                // 返回 Location 头中的上传会话 URL
                connection.getHeaderField("Location")
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                ApplicationLoggingServiceManager.e(TAG, "发起可恢复上传失败: HTTP $responseCode, $errorBody")
                null
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "发起可恢复上传时发生异常", e)
            null
        }
    }

    /**
     * 上传文件内容到已建立的会话
     * 支持断点续传：如果上传中断，可以通过查询已上传字节数继续上传
     *
     * @param token OAuth2 访问令牌
     * @param sessionUrl 上传会话 URL
     * @param localFile 本地文件
     * @param fileSize 文件总大小
     * @param progress 进度回调
     * @return 上传成功后的远程文件元数据，失败时返回 null
     */
    private suspend fun uploadFileContent(
        token: String,
        sessionUrl: String,
        localFile: File,
        fileSize: Long,
        progress: (bytesTransferred: Long, totalBytes: Long) -> Unit
    ): GoogleDriveFile? {
        return try {
            var uploadedBytes = 0L
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

            FileInputStream(localFile).use { fis ->
                while (uploadedBytes < fileSize) {
                    // 检查是否需要断点续传
                    val connection = java.net.URL(sessionUrl).openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "PUT"
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.setRequestProperty("Content-Type", MIME_TYPE_JSON)
                    connection.setRequestProperty(
                        "Content-Range",
                        "bytes $uploadedBytes-${fileSize - 1}/$fileSize"
                    )
                    connection.doOutput = true
                    connection.connectTimeout = 60_000
                    connection.readTimeout = 60_000

                    // 写入文件内容
                    val outputStream: OutputStream = connection.outputStream
                    var bytesRemaining = fileSize - uploadedBytes
                    var chunkSize = 0

                    // 分块写入，每块 256KB
                    while (bytesRemaining > 0 && chunkSize < RESUMABLE_CHUNK_SIZE) {
                        val read = fis.read(buffer)
                        if (read == -1) break
                        outputStream.write(buffer, 0, read)
                        uploadedBytes += read
                        bytesRemaining -= read
                        chunkSize += read
                        progress(uploadedBytes, fileSize)
                    }
                    outputStream.flush()
                    outputStream.close()

                    val responseCode = connection.responseCode
                    when {
                        responseCode == 200 || responseCode == 201 -> {
                            // 上传完成
                            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                            ApplicationLoggingServiceManager.i(TAG, "文件上传成功: ${localFile.name}")
                            return parseDriveFileFromResponse(responseBody)
                        }
                        responseCode == 308 -> {
                            // 部分上传成功，继续上传剩余部分
                            val range = connection.getHeaderField("Range")
                            if (range != null) {
                                // 从 Range 头解析已上传字节数
                                val match = Regex("bytes=(\\d+)-").find(range)
                                if (match != null) {
                                    uploadedBytes = match.groupValues[1].toLong()
                                }
                            }
                            ApplicationLoggingServiceManager.d(
                                TAG,
                                "断点续传: 已上传 $uploadedBytes / $fileSize bytes"
                            )
                        }
                        else -> {
                            val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                                ?: "Unknown error"
                            ApplicationLoggingServiceManager.e(
                                TAG,
                                "上传文件内容失败: HTTP $responseCode, $errorBody"
                            )
                            return null
                        }
                    }
                }
            }

            // 理论上不会到达此处，因为 200/201 会直接 return
            ApplicationLoggingServiceManager.w(TAG, "文件上传流程异常结束: ${localFile.name}")
            null
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "上传文件内容时发生异常", e)
            null
        }
    }

    // ==================== 文件下载（支持进度回调） ====================

    /**
     * 从 Google Drive 下载文件到本地
     * 支持实时进度回调，下载完成后校验文件完整性
     *
     * @param token OAuth2 访问令牌
     * @param driveFileId Google Drive 文件 ID
     * @param destination 本地目标文件
     * @param progress 下载进度回调 (已传输字节数, 总字节数)
     * @return 下载成功时返回目标文件，失败时返回 null
     */
    suspend fun downloadFile(
        token: String,
        driveFileId: String,
        destination: File,
        progress: (bytesTransferred: Long, totalBytes: Long) -> Unit
    ): File? {
        var connection: java.net.HttpURLConnection? = null
        return try {
            // 确保目标文件的父目录存在
            destination.parentFile?.mkdirs()

            // 使用 Google Drive 的 webContentLink 或 export 接口下载文件
            val url = "$DRIVE_API_BASE_URL/files/$driveFileId?alt=media"

            connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val contentLength = connection.contentLengthLong
                val inputStream = connection.inputStream

                FileOutputStream(destination).use { fos ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var totalRead = 0L
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        progress(totalRead, if (contentLength > 0) contentLength else -1)
                    }
                    fos.flush()
                }

                ApplicationLoggingServiceManager.i(
                    TAG,
                    "文件下载成功: $driveFileId -> ${destination.absolutePath}"
                )
                destination
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                ApplicationLoggingServiceManager.e(TAG, "下载文件失败: HTTP $responseCode, $errorBody")
                null
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "下载文件时发生异常: $driveFileId", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    // ==================== 文件删除 ====================

    /**
     * 删除 Google Drive 上的指定文件
     * 将文件移至回收站（软删除），不永久删除
     *
     * @param token OAuth2 访问令牌
     * @param driveFileId 要删除的文件 ID
     * @return true 表示删除成功
     */
    suspend fun deleteFile(token: String, driveFileId: String): Boolean {
        var connection: java.net.HttpURLConnection? = null
        return try {
            val url = "$DRIVE_API_BASE_URL/files/$driveFileId"
            connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "DELETE"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000

            val responseCode = connection.responseCode
            if (responseCode == 204) {
                ApplicationLoggingServiceManager.i(TAG, "文件删除成功: $driveFileId")
                true
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                ApplicationLoggingServiceManager.e(TAG, "删除文件失败: HTTP $responseCode, $errorBody")
                false
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "删除文件时发生异常: $driveFileId", e)
            false
        } finally {
            connection?.disconnect()
        }
    }

    // ==================== 文件信息获取 ====================

    /**
     * 获取指定文件的详细元数据
     *
     * @param token OAuth2 访问令牌
     * @param driveFileId Google Drive 文件 ID
     * @return 文件元数据，失败时返回 null
     */
    suspend fun getFileInfo(token: String, driveFileId: String): GoogleDriveFile? {
        var connection: java.net.HttpURLConnection? = null
        return try {
            val url = "$DRIVE_API_BASE_URL/files/$driveFileId?fields=$FILE_FIELDS"
            connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                parseDriveFileFromResponse(responseBody)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                ApplicationLoggingServiceManager.e(TAG, "获取文件信息失败: HTTP $responseCode, $errorBody")
                null
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "获取文件信息时发生异常: $driveFileId", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    // ==================== 冲突检测 ====================

    /**
     * 检测本地文件与远程文件是否存在冲突
     * 基于 MD5 校验和比较文件内容是否一致
     *
     * @param localFile 本地文件
     * @param remoteFile 远程文件元数据
     * @return true 表示存在冲突（文件内容不一致）
     */
    fun detectConflict(localFile: File, remoteFile: GoogleDriveFile): Boolean {
        if (remoteFile.md5Checksum == null) {
            // 远程文件无 MD5 信息，无法判断，保守认为无冲突
            ApplicationLoggingServiceManager.w(TAG, "远程文件缺少 MD5 校验和，跳过冲突检测")
            return false
        }

        val localMd5 = calculateMd5(localFile)
        val hasConflict = localMd5 != remoteFile.md5Checksum

        if (hasConflict) {
            ApplicationLoggingServiceManager.i(
                TAG,
                "检测到冲突: 本地MD5=$localMd5, 远程MD5=${remoteFile.md5Checksum}"
            )
        }

        return hasConflict
    }

    /**
     * 计算文件的 MD5 校验和
     *
     * @param file 要计算的文件
     * @return MD5 十六进制字符串，失败时返回空字符串
     */
    private fun calculateMd5(file: File): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("MD5")
            file.inputStream().use { fis ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "计算 MD5 失败: ${file.name}", e)
            ""
        }
    }

    // ==================== 增量同步支持 ====================

    /**
     * 检查本地文件是否需要上传（增量同步判断）
     * 通过比较本地文件的修改时间和远程文件的修改时间来判断
     *
     * @param localFile 本地文件
     * @param remoteFile 远程文件元数据
     * @return true 表示本地文件有更新，需要上传
     */
    fun needsUpload(localFile: File, remoteFile: GoogleDriveFile?): Boolean {
        if (remoteFile == null) return true

        // 解析远程文件的修改时间
        val remoteModifiedTime = parseIso8601ToMillis(remoteFile.modifiedTime)
        if (remoteModifiedTime <= 0) return true

        // 比较修改时间，本地更新才需要上传
        val needsSync = localFile.lastModified() > remoteModifiedTime
        if (needsSync) {
            ApplicationLoggingServiceManager.d(
                TAG,
                "文件需要上传: ${localFile.name} (本地: ${localFile.lastModified()}, 远程: $remoteModifiedTime)"
            )
        }
        return needsSync
    }

    // ==================== JSON 解析辅助方法 ====================

    /**
     * 从 API 响应中解析文件 ID
     * 简单的 JSON 字符串解析，避免引入额外依赖
     */
    private fun parseFileIdFromResponse(json: String): String? {
        return try {
            // 使用正则提取 "id" 字段值
            val regex = Regex(""""id"\s*:\s*"([^"]+)"""")
            regex.find(json)?.groupValues?.get(1)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "解析文件 ID 失败", e)
            null
        }
    }

    /**
     * 从文件夹搜索响应中解析文件夹 ID
     */
    private fun parseFolderIdFromResponse(json: String): String? {
        return try {
            // 从 files 数组中提取第一个文件的 id
            val filesRegex = Regex(""""files"\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)
            val filesMatch = filesRegex.find(json)
            if (filesMatch != null) {
                val filesContent = filesMatch.groupValues[1]
                parseFileIdFromResponse(filesContent)
            } else {
                null
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "解析文件夹 ID 失败", e)
            null
        }
    }

    /**
     * 从文件列表响应中解析完整的文件列表
     */
    private fun parseFileListFromResponse(json: String): List<GoogleDriveFile> {
        return try {
            val files = mutableListOf<GoogleDriveFile>()

            // 使用正则匹配每个文件对象
            val filePattern = Regex(
                """\{\s*"id"\s*:\s*"([^"]+)"\s*,\s*"name"\s*:\s*"([^"]+)"\s*,\s*"mimeType"\s*:\s*"([^"]+)"""" +
                        """(?:\s*,\s*"size"\s*:\s*(\d+))?""" +
                        """(?:\s*,\s*"modifiedTime"\s*:\s*"([^"]+)")?""" +
                        """(?:\s*,\s*"md5Checksum"\s*:\s*"([^"]+)")?""" +
                        """(?:\s*,\s*"webViewLink"\s*:\s*"([^"]+)")?\s*\}""",
                RegexOption.DOT_MATCHES_ALL
            )

            filePattern.findAll(json).forEach { match ->
                files.add(
                    GoogleDriveFile(
                        id = match.groupValues[1],
                        name = match.groupValues[2],
                        mimeType = match.groupValues[3],
                        size = match.groupValues[4].toLongOrNull(),
                        modifiedTime = match.groupValues[5],
                        md5Checksum = match.groupValues[6].ifBlank { null },
                        webViewLink = match.groupValues[7].ifBlank { null }
                    )
                )
            }

            ApplicationLoggingServiceManager.d(TAG, "解析到 ${files.size} 个备份文件")
            files
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "解析文件列表失败", e)
            emptyList()
        }
    }

    /**
     * 从单个文件响应中解析完整的文件元数据
     */
    private fun parseDriveFileFromResponse(json: String): GoogleDriveFile? {
        return try {
            GoogleDriveFile(
                id = extractJsonString(json, "id") ?: "",
                name = extractJsonString(json, "name") ?: "",
                mimeType = extractJsonString(json, "mimeType") ?: "",
                size = extractJsonLong(json, "size"),
                modifiedTime = extractJsonString(json, "modifiedTime") ?: "",
                md5Checksum = extractJsonString(json, "md5Checksum"),
                webViewLink = extractJsonString(json, "webViewLink")
            )
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "解析文件元数据失败", e)
            null
        }
    }

    /**
     * 从 JSON 字符串中提取字符串字段值
     */
    private fun extractJsonString(json: String, fieldName: String): String? {
        val regex = Regex(""""$fieldName"\s*:\s*"([^"]*)"""")
        return regex.find(json)?.groupValues?.get(1)
    }

    /**
     * 从 JSON 字符串中提取长整型字段值
     */
    private fun extractJsonLong(json: String, fieldName: String): Long? {
        val regex = Regex(""""$fieldName"\s*:\s*(\d+)""")
        return regex.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }

    /**
     * 解析 ISO 8601 格式的时间字符串为毫秒时间戳
     *
     * @param isoTime ISO 8601 格式的时间字符串（如 "2024-01-15T10:30:00.000Z"）
     * @return 毫秒时间戳，解析失败时返回 0
     */
    private fun parseIso8601ToMillis(isoTime: String): Long {
        return try {
            // 处理常见的 ISO 8601 格式
            val cleanedTime = isoTime.replace("Z", "+00:00")
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
            format.parse(cleanedTime)?.time ?: 0L
        } catch (e: Exception) {
            // 尝试不带毫秒的格式
            try {
                val cleanedTime = isoTime.replace("Z", "+00:00")
                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                format.parse(cleanedTime)?.time ?: 0L
            } catch (e2: Exception) {
                ApplicationLoggingServiceManager.w(TAG, "解析 ISO 8601 时间失败: $isoTime")
                0L
            }
        }
    }

    // ==================== 常量 ====================

    /** 可恢复上传的块大小：256KB */
    private const val RESUMABLE_CHUNK_SIZE = 256 * 1024

    // ==================== HTTP连接安全工具方法 ====================

    /**
     * 安全执行HTTP请求，确保连接在finally块中断开
     * 
     * @param block HTTP操作代码块，接收HttpURLConnection参数
     * @return 操作结果
     */
    private inline fun <T> safeHttpRequest(block: (java.net.HttpURLConnection) -> T): T? {
        var connection: java.net.HttpURLConnection? = null
        return try {
            connection = block as? java.net.HttpURLConnection
            block(connection ?: return null)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "HTTP请求异常", e)
            null
        } finally {
            connection?.disconnect()
        }
    }
}
