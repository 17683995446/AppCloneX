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

import com.appclonex.domain.model.GoogleDriveAuthState
import com.appclonex.domain.model.GoogleDriveConflictResolution
import com.appclonex.domain.model.GoogleDriveFile
import com.appclonex.domain.model.GoogleDriveSyncStatus
import com.appclonex.domain.model.GoogleDriveUploadProgress
import com.appclonex.domain.model.SyncConflictInfo
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Google Drive 仓库接口
 * 定义与 Google Drive 云存储交互的所有操作，包括认证、文件管理和同步功能。
 * 遵循 Clean Architecture 的 Domain 层规范，不依赖任何具体实现细节。
 */
interface GoogleDriveRepository {

    // ==================== 认证管理 / Authentication ====================

    /**
     * 发起 Google 账号认证流程
     * 如果已有有效认证则直接返回当前状态，否则启动 Google Sign-In 流程
     *
     * @return 认证结果，成功时包含 [GoogleDriveAuthState]，失败时包含异常信息
     */
    suspend fun authenticate(): Result<GoogleDriveAuthState>

    /**
     * 退出当前 Google 账号登录
     * 清除本地认证凭据并断开 Google Drive 连接
     *
     * @return 退出操作结果
     */
    suspend fun signOut(): Result<Unit>

    /**
     * 检查当前是否已通过 Google 账号认证
     *
     * @return true 表示已认证且凭据有效
     */
    suspend fun isAuthenticated(): Boolean

    /**
     * 获取认证状态的响应式数据流
     * 认证状态变化时会自动推送新值
     *
     * @return [GoogleDriveAuthState] 的 Flow
     */
    fun getAuthState(): Flow<GoogleDriveAuthState>

    // ==================== 文件操作 / File Operations ====================

    /**
     * 列出 Google Drive 中 AppCloneX 备份文件夹下的所有备份文件
     * 文件按修改时间降序排列，仅返回 .json 格式的备份文件
     *
     * @return 备份文件列表，失败时返回异常信息
     */
    suspend fun listBackups(): Result<List<GoogleDriveFile>>

    /**
     * 上传本地备份文件到 Google Drive
     * 支持断点续传和实时进度回调
     *
     * @param localFile 要上传的本地文件
     * @param progress 传输进度回调，用于更新 UI 进度条
     * @return 上传成功后的远程文件元数据
     */
    suspend fun uploadBackup(
        localFile: File,
        progress: (GoogleDriveUploadProgress) -> Unit
    ): Result<GoogleDriveFile>

    /**
     * 从 Google Drive 下载备份文件到本地
     * 支持实时进度回调
     *
     * @param driveFileId Google Drive 文件 ID
     * @param destination 本地目标文件路径
     * @param progress 传输进度回调，用于更新 UI 进度条
     * @return 下载完成后的本地文件
     */
    suspend fun downloadBackup(
        driveFileId: String,
        destination: File,
        progress: (GoogleDriveUploadProgress) -> Unit
    ): Result<File>

    /**
     * 删除 Google Drive 上的指定备份文件
     *
     * @param driveFileId 要删除的 Google Drive 文件 ID
     * @return 删除操作结果
     */
    suspend fun deleteBackup(driveFileId: String): Result<Unit>

    /**
     * 获取指定 Google Drive 文件的详细信息
     *
     * @param driveFileId Google Drive 文件 ID
     * @return 文件元数据，失败时返回异常信息
     */
    suspend fun getBackupInfo(driveFileId: String): Result<GoogleDriveFile>

    // ==================== 冲突解决 / Conflict Resolution ====================

    /**
     * 解决同步冲突
     * 根据用户选择的策略处理本地与远程文件的不一致
     *
     * @param conflict 冲突信息，包含本地和远程文件的元数据
     * @param resolution 用户选择的冲突解决策略
     * @return 冲突解决操作结果
     */
    suspend fun resolveConflict(
        conflict: SyncConflictInfo,
        resolution: GoogleDriveConflictResolution
    ): Result<Unit>

    // ==================== 同步状态 / Sync Status ====================

    /**
     * 获取当前同步状态的响应式数据流
     * 同步操作进行中状态会实时更新
     *
     * @return [GoogleDriveSyncStatus] 的 Flow
     */
    fun getSyncStatus(): Flow<GoogleDriveSyncStatus>
}
