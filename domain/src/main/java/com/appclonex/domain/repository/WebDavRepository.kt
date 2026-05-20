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

package com.appclonex.domain.repository

import com.appclonex.domain.model.WebDavConfig
import com.appclonex.domain.model.WebDavResource
import com.appclonex.domain.model.WebDavSyncResult
import java.io.File

/**
 * WebDAV 仓库接口
 * 定义 WebDAV 协议（RFC 4918）的核心文件操作能力，
 * 包括文件列表、上传、下载、删除、目录创建和增量同步。
 *
 * 实现层需处理 HTTP 请求、XML 响应解析、认证和错误处理。
 */
interface WebDavRepository {

    /**
     * 测试与 WebDAV 服务器的连接
     * 向服务器发送 PROPFIND 请求验证配置是否正确、认证是否通过
     *
     * @param config WebDAV 连接配置
     * @return Result<Boolean>，true 表示连接成功
     */
    suspend fun testConnection(config: WebDavConfig): Result<Boolean>

    /**
     * 列出指定目录下的文件和子目录
     * 发送 PROPFIND 请求（深度1）获取目录内容
     *
     * @param config WebDAV 连接配置
     * @param path 远程目录路径（相对于 basePath）
     * @return Result<List<WebDavResource>> 目录下的资源列表
     */
    suspend fun listFiles(config: WebDavConfig, path: String): Result<List<WebDavResource>>

    /**
     * 上传文件到 WebDAV 服务器
     * 使用 PUT 请求将本地文件上传到指定远程路径
     *
     * @param config WebDAV 连接配置
     * @param localPath 本地文件的绝对路径
     * @param remotePath 远程目标路径（相对于 basePath）
     * @return Result<WebDavResource> 上传成功后的远程资源信息
     */
    suspend fun uploadFile(config: WebDavConfig, localPath: String, remotePath: String): Result<WebDavResource>

    /**
     * 从 WebDAV 服务器下载文件
     * 使用 GET 请求下载远程文件到本地指定路径
     *
     * @param config WebDAV 连接配置
     * @param remotePath 远程文件路径（相对于 basePath）
     * @param localPath 本地目标文件的绝对路径
     * @return Result<File> 下载成功后的本地文件引用
     */
    suspend fun downloadFile(config: WebDavConfig, remotePath: String, localPath: String): Result<File>

    /**
     * 删除 WebDAV 服务器上的文件或目录
     * 使用 DELETE 请求删除指定远程路径的资源
     *
     * @param config WebDAV 连接配置
     * @param remotePath 要删除的远程路径（相对于 basePath）
     * @return Result<Unit> 删除成功时返回 Unit
     */
    suspend fun deleteFile(config: WebDavConfig, remotePath: String): Result<Unit>

    /**
     * 在 WebDAV 服务器上创建目录
     * 使用 MKCOL 请求创建指定路径的目录
     *
     * @param config WebDAV 连接配置
     * @param path 要创建的目录路径（相对于 basePath）
     * @return Result<Unit> 创建成功时返回 Unit
     */
    suspend fun createDirectory(config: WebDavConfig, path: String): Result<Unit>

    /**
     * 检查远程文件是否存在
     * 使用 HEAD 请求检查指定路径的资源是否存在
     *
     * @param config WebDAV 连接配置
     * @param remotePath 远程文件路径（相对于 basePath）
     * @return Result<Boolean>，true 表示文件存在
     */
    suspend fun checkFileExists(config: WebDavConfig, remotePath: String): Result<Boolean>

    /**
     * 获取远程文件的详细信息
     * 使用 PROPFIND 请求（深度0）获取单个资源的属性
     *
     * @param config WebDAV 连接配置
     * @param remotePath 远程文件路径（相对于 basePath）
     * @return Result<WebDavResource> 文件的详细资源信息
     */
    suspend fun getFileInfo(config: WebDavConfig, remotePath: String): Result<WebDavResource>

    /**
     * 增量同步本地目录与远程目录
     * 比较本地和远程文件的修改时间，只上传新文件或修改过的文件，
     * 同时下载远程存在但本地缺失的文件
     *
     * @param config WebDAV 连接配置
     * @param localDir 本地目录的绝对路径
     * @param remoteDir 远程目录路径（相对于 basePath）
     * @return Result<WebDavSyncResult> 同步操作的统计结果
     */
    suspend fun syncBackupFiles(config: WebDavConfig, localDir: String, remoteDir: String): Result<WebDavSyncResult>
}
