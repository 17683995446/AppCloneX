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

import com.appclonex.domain.model.WebDavConfig
import com.appclonex.domain.model.WebDavResource
import com.appclonex.domain.model.WebDavSyncResult
import com.appclonex.domain.repository.WebDavRepository
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import com.appclonex.infrastructure.network.WebDavApiService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebDAV 仓库实现
 * 实现所有 WebDavRepository 接口方法，通过 WebDavApiService 执行实际的 WebDAV 协议通信。
 *
 * 核心功能：
 * - 文件 CRUD 操作（通过 WebDavApiService 的 HTTP 方法）
 * - 增量同步：比较本地和远程文件的修改时间，只传输有变化的文件
 * - 线程安全：使用 Mutex 保证并发操作的安全性
 *
 * @param apiService WebDAV API 服务，负责底层 HTTP 通信和 XML 解析
 */
@Singleton
class WebDavRepositoryImpl @Inject constructor(
    private val apiService: WebDavApiService
) : WebDavRepository {

    companion object {
        private const val TAG = "WebDavRepo"
    }

    /** 互斥锁，保证同步操作的线程安全 */
    private val syncMutex = Mutex()

    // ==================== 连接测试 ====================

    /**
     * 测试与 WebDAV 服务器的连接
     * 尝试列出根目录内容来验证连接和认证是否正常
     *
     * @param config WebDAV 连接配置
     * @return Result<Boolean>，true 表示连接成功
     */
    override suspend fun testConnection(config: WebDavConfig): Result<Boolean> {
        return try {
            ApplicationLoggingServiceManager.i(TAG, "测试 WebDAV 连接: ${config.serverUrl}")
            val resources = apiService.propfindList(config, "/")
            val connected = resources != null
            ApplicationLoggingServiceManager.i(
                TAG,
                "WebDAV 连接测试${if (connected) "成功" else "失败"}"
            )
            Result.success(connected)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "WebDAV 连接测试异常", e)
            Result.failure(e)
        }
    }

    // ==================== 文件列表 ====================

    /**
     * 列出指定目录下的文件和子目录
     *
     * @param config WebDAV 连接配置
     * @param path 远程目录路径
     * @return Result<List<WebDavResource>> 目录下的资源列表
     */
    override suspend fun listFiles(config: WebDavConfig, path: String): Result<List<WebDavResource>> {
        return try {
            val resources = apiService.propfindList(config, path)
            if (resources != null) {
                ApplicationLoggingServiceManager.d(TAG, "列出文件成功: $path, 共 ${resources.size} 个资源")
                Result.success(resources)
            } else {
                Result.failure(IllegalStateException("无法获取远程目录列表: $path"))
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "列出文件异常: $path", e)
            Result.failure(e)
        }
    }

    // ==================== 文件上传 ====================

    /**
     * 上传文件到 WebDAV 服务器
     *
     * @param config WebDAV 连接配置
     * @param localPath 本地文件的绝对路径
     * @param remotePath 远程目标路径
     * @return Result<WebDavResource> 上传成功后的远程资源信息
     */
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
            val parentPath = extractParentPath(remotePath)
            if (parentPath.isNotEmpty() && parentPath != "/") {
                val dirCreated = apiService.makeCollection(config, parentPath)
                if (dirCreated) {
                    ApplicationLoggingServiceManager.d(TAG, "远程目录已就绪: $parentPath")
                }
            }

            val resource = apiService.putFile(config, localFile, remotePath) { transferred, total ->
                ApplicationLoggingServiceManager.d(
                    TAG,
                    "上传进度: $transferred / $total bytes"
                )
            }

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

    // ==================== 文件下载 ====================

    /**
     * 从 WebDAV 服务器下载文件
     *
     * @param config WebDAV 连接配置
     * @param remotePath 远程文件路径
     * @param localPath 本地目标文件的绝对路径
     * @return Result<File> 下载成功后的本地文件引用
     */
    override suspend fun downloadFile(
        config: WebDavConfig,
        remotePath: String,
        localPath: String
    ): Result<File> {
        return try {
            val localFile = File(localPath)
            // 确保本地目录存在
            localFile.parentFile?.mkdirs()

            val result = apiService.getFile(config, remotePath, localFile) { transferred, total ->
                ApplicationLoggingServiceManager.d(
                    TAG,
                    "下载进度: $transferred / $total bytes"
                )
            }

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

    // ==================== 文件删除 ====================

    /**
     * 删除 WebDAV 服务器上的文件或目录
     *
     * @param config WebDAV 连接配置
     * @param remotePath 要删除的远程路径
     * @return Result<Unit> 删除成功时返回 Unit
     */
    override suspend fun deleteFile(config: WebDavConfig, remotePath: String): Result<Unit> {
        return try {
            val deleted = apiService.deleteResource(config, remotePath)
            if (deleted) {
                ApplicationLoggingServiceManager.i(TAG, "删除成功: $remotePath")
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("删除文件失败: $remotePath"))
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "删除文件异常: $remotePath", e)
            Result.failure(e)
        }
    }

    // ==================== 目录创建 ====================

    /**
     * 在 WebDAV 服务器上创建目录
     *
     * @param config WebDAV 连接配置
     * @param path 要创建的目录路径
     * @return Result<Unit> 创建成功时返回 Unit
     */
    override suspend fun createDirectory(config: WebDavConfig, path: String): Result<Unit> {
        return try {
            val created = apiService.makeCollection(config, path)
            if (created) {
                ApplicationLoggingServiceManager.i(TAG, "创建目录成功: $path")
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("创建目录失败: $path"))
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "创建目录异常: $path", e)
            Result.failure(e)
        }
    }

    // ==================== 文件存在检查 ====================

    /**
     * 检查远程文件是否存在
     *
     * @param config WebDAV 连接配置
     * @param remotePath 远程文件路径
     * @return Result<Boolean>，true 表示文件存在
     */
    override suspend fun checkFileExists(config: WebDavConfig, remotePath: String): Result<Boolean> {
        return try {
            val exists = apiService.headResource(config, remotePath)
            Result.success(exists)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "检查文件存在异常: $remotePath", e)
            Result.failure(e)
        }
    }

    // ==================== 文件信息获取 ====================

    /**
     * 获取远程文件的详细信息
     *
     * @param config WebDAV 连接配置
     * @param remotePath 远程文件路径
     * @return Result<WebDavResource> 文件的详细资源信息
     */
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

    // ==================== 增量同步 ====================

    /**
     * 增量同步本地目录与远程目录
     * 使用 Mutex 保证同步操作的线程安全。
     *
     * 同步策略：
     * 1. 获取本地和远程文件列表
     * 2. 比较文件名和修改时间，确定需要上传和下载的文件
     * 3. 上传本地有但远程没有或本地更新的文件
     * 4. 下载远程有但本地没有的文件
     * 5. 跳过两端都存在且未变化的文件
     *
     * @param config WebDAV 连接配置
     * @param localDir 本地目录的绝对路径
     * @param remoteDir 远程目录路径
     * @return Result<WebDavSyncResult> 同步操作的统计结果
     */
    override suspend fun syncBackupFiles(
        config: WebDavConfig,
        localDir: String,
        remoteDir: String
    ): Result<WebDavSyncResult> {
        return syncMutex.withLock {
            val startTime = System.currentTimeMillis()
            var uploadedFiles = 0
            var downloadedFiles = 0
            var skippedFiles = 0
            var failedFiles = 0
            var totalBytes = 0L

            try {
                ApplicationLoggingServiceManager.i(TAG, "开始增量同步: 本地=$localDir, 远程=$remoteDir")

                // 验证本地目录
                val localDirectory = File(localDir)
                if (!localDirectory.exists() || !localDirectory.isDirectory) {
                    return Result.failure(IllegalArgumentException("本地目录不存在: $localDir"))
                }

                // 确保远程目录存在
                val dirCreated = apiService.makeCollection(config, remoteDir)
                if (!dirCreated) {
                    return Result.failure(IllegalStateException("无法创建远程目录: $remoteDir"))
                }

                // 获取远程文件列表
                val remoteResources = apiService.propfindList(config, remoteDir) ?: emptyList()
                val remoteFileMap = remoteResources
                    .filter { !it.isCollection }
                    .associateBy { it.getFileName() }

                // 获取本地文件列表
                val localFiles = localDirectory.listFiles { file -> file.isFile && !file.isHidden }
                    ?: emptyArray()

                // 构建本地文件名到文件的映射
                val localFileMap = localFiles.associateBy { it.name }

                // 第一阶段：上传本地有但远程没有或本地更新的文件
                for ((fileName, localFile) in localFileMap) {
                    try {
                        val remoteResource = remoteFileMap[fileName]
                        val needsUpload = remoteResource == null ||
                            localFile.lastModified() > remoteResource.lastModified

                        if (needsUpload) {
                            val remotePath = normalizePath(remoteDir, fileName)
                            val result = apiService.putFile(config, localFile, remotePath)
                            if (result != null) {
                                uploadedFiles++
                                totalBytes += localFile.length()
                                ApplicationLoggingServiceManager.d(TAG, "已上传: $fileName")
                            } else {
                                failedFiles++
                                ApplicationLoggingServiceManager.w(TAG, "上传失败: $fileName")
                            }
                        } else {
                            skippedFiles++
                            ApplicationLoggingServiceManager.d(TAG, "已跳过（未变化）: $fileName")
                        }
                    } catch (e: Exception) {
                        failedFiles++
                        ApplicationLoggingServiceManager.w(TAG, "上传异常: $fileName", e)
                    }
                }

                // 第二阶段：下载远程有但本地没有的文件
                for ((fileName, remoteResource) in remoteFileMap) {
                    if (localFileMap.containsKey(fileName)) continue // 本地已有，跳过

                    try {
                        val localPath = File(localDirectory, fileName).absolutePath
                        val remotePath = normalizePath(remoteDir, fileName)
                        val result = apiService.getFile(config, remotePath, File(localPath))
                        if (result != null) {
                            downloadedFiles++
                            totalBytes += remoteResource.contentLength
                            ApplicationLoggingServiceManager.d(TAG, "已下载: $fileName")
                        } else {
                            failedFiles++
                            ApplicationLoggingServiceManager.w(TAG, "下载失败: $fileName")
                        }
                    } catch (e: Exception) {
                        failedFiles++
                        ApplicationLoggingServiceManager.w(TAG, "下载异常: $fileName", e)
                    }
                }

                val durationMs = System.currentTimeMillis() - startTime
                val syncResult = WebDavSyncResult(
                    uploadedFiles = uploadedFiles,
                    downloadedFiles = downloadedFiles,
                    skippedFiles = skippedFiles,
                    failedFiles = failedFiles,
                    totalBytes = totalBytes,
                    durationMs = durationMs
                )

                ApplicationLoggingServiceManager.i(
                    TAG,
                    "增量同步完成: 上传=$uploadedFiles, 下载=$downloadedFiles, " +
                        "跳过=$skippedFiles, 失败=$failedFiles, " +
                        "总字节=$totalBytes, 耗时=${durationMs}ms"
                )

                Result.success(syncResult)
            } catch (e: Exception) {
                ApplicationLoggingServiceManager.e(TAG, "增量同步异常", e)
                Result.failure(e)
            }
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从路径中提取父目录路径
     * 例如: "/backups/file.json" -> "/backups"
     *
     * @param path 文件路径
     * @return 父目录路径
     */
    private fun extractParentPath(path: String): String {
        val normalized = path.trimEnd('/')
        val lastSlash = normalized.lastIndexOf('/')
        return if (lastSlash > 0) normalized.substring(0, lastSlash) else ""
    }

    /**
     * 拼接并规范化远程路径
     * 确保目录和文件名之间只有一个斜杠
     *
     * @param dir 目录路径
     * @param fileName 文件名
     * @return 规范化的完整路径
     */
    private fun normalizePath(dir: String, fileName: String): String {
        val normalizedDir = dir.trimEnd('/')
        return "$normalizedDir/$fileName"
    }
}
