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

import com.appclonex.domain.model.NextcloudConfig
import com.appclonex.domain.model.NextcloudShareInfo

/**
 * Nextcloud 仓库接口
 * 继承 WebDavRepository 的全部能力，并扩展 Nextcloud 特有的 OCS API 功能，
 * 包括分享管理、服务器信息查询和用户配额获取。
 *
 * Nextcloud 的文件操作（上传、下载、列表等）通过 WebDAV 协议实现，
 * 管理功能（分享、配额等）通过 OCS REST API 实现。
 */
interface NextcloudRepository : WebDavRepository {

    /**
     * 创建分享链接
     * 通过 Nextcloud OCS API 创建文件或目录的分享
     *
     * @param config Nextcloud 连接配置
     * @param remotePath 要分享的远程路径（相对于 basePath）
     * @param shareWith 分享目标（用户名、组名或邮箱），null 表示创建公开链接
     * @param permissions 分享权限位掩码（1=读取, 2=更新, 4=创建, 8=删除, 16=共享, 31=全部）
     * @return Result<NextcloudShareInfo> 创建的分享信息
     */
    suspend fun createShare(
        config: NextcloudConfig,
        remotePath: String,
        shareWith: String?,
        permissions: Int
    ): Result<NextcloudShareInfo>

    /**
     * 列出指定路径的所有分享
     * 通过 Nextcloud OCS API 查询文件或目录的现有分享
     *
     * @param config Nextcloud 连接配置
     * @param remotePath 远程路径（相对于 basePath）
     * @return Result<List<NextcloudShareInfo>> 该路径下的分享列表
     */
    suspend fun listShares(config: NextcloudConfig, remotePath: String): Result<List<NextcloudShareInfo>>

    /**
     * 删除指定的分享
     * 通过 Nextcloud OCS API 删除分享链接
     *
     * @param config Nextcloud 连接配置
     * @param shareId 要删除的分享 ID
     * @return Result<Unit> 删除成功时返回 Unit
     */
    suspend fun deleteShare(config: NextcloudConfig, shareId: String): Result<Unit>

    /**
     * 获取 Nextcloud 服务器信息
     * 通过 OCS API 查询服务器版本、功能等元数据
     *
     * @param config Nextcloud 连接配置
     * @return Result<Map<String, String>> 服务器信息键值对（如 version, hostname, edition 等）
     */
    suspend fun getServerInfo(config: NextcloudConfig): Result<Map<String, String>>

    /**
     * 获取当前用户的存储配额
     * 通过 OCS API 查询用户已使用和总配额空间
     *
     * @param config Nextcloud 连接配置
     * @return Result<Pair<Long, Long>> Pair(已使用字节数, 总配额字节数)，-1 表示无限制
     */
    suspend fun getUserQuota(config: NextcloudConfig): Result<Pair<Long, Long>>
}
