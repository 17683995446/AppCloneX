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

// ==================== Google Drive 文件模型 ====================

/**
 * Google Drive 文件元数据模型
 * 对应 Google Drive REST API v3 的 File 资源
 *
 * @property id Google Drive 文件唯一标识符
 * @property name 文件名称
 * @property mimeType 文件的 MIME 类型
 * @property size 文件大小（字节），文件夹时为 null
 * @property modifiedTime 最后修改时间（ISO 8601 格式字符串）
 * @property md5Checksum 文件的 MD5 校验和，用于冲突检测
 * @property webViewLink 在浏览器中打开文件的链接
 */
@Serializable
data class GoogleDriveFile(
    val id: String = "",
    val name: String = "",
    val mimeType: String = "",
    val size: Long? = null,
    val modifiedTime: String = "",
    val md5Checksum: String? = null,
    val webViewLink: String? = null
)

// ==================== 上传进度模型 ====================

/**
 * 上传/下载进度状态枚举
 */
enum class TransferState {
    /** 空闲状态，未开始传输 */
    IDLE,
    /** 正在传输中 */
    UPLOADING,
    /** 传输已暂停 */
    PAUSED,
    /** 传输已完成 */
    COMPLETED,
    /** 传输失败 */
    FAILED
}

/**
 * Google Drive 文件传输进度模型
 * 用于跟踪文件上传和下载的实时进度
 *
 * @property bytesTransferred 已传输的字节数
 * @property totalBytes 总字节数（未知时为 -1）
 * @property state 当前传输状态
 */
@Serializable
data class GoogleDriveUploadProgress(
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = -1L,
    val state: TransferState = TransferState.IDLE
) {
    /**
     * 计算传输进度百分比（0-100），总大小未知时返回 -1
     */
    fun progressPercent(): Int {
        if (totalBytes <= 0) return -1
        return ((bytesTransferred * 100) / totalBytes).coerceIn(0, 100)
    }
}

// ==================== 同步状态模型 ====================

/**
 * Google Drive 同步状态枚举
 * 表示当前与 Google Drive 的同步操作状态
 */
enum class GoogleDriveSyncStatus {
    /** 空闲状态，无同步操作进行 */
    IDLE,
    /** 正在执行同步操作 */
    SYNCING,
    /** 同步操作成功完成 */
    SUCCESS,
    /** 同步操作失败 */
    FAILED,
    /** 检测到冲突，需要用户介入解决 */
    CONFLICT
}

// ==================== 冲突解决模型 ====================

/**
 * Google Drive 冲突解决策略枚举
 * 定义当本地和远程文件发生冲突时的处理方式
 */
enum class GoogleDriveConflictResolution {
    /** 保留本地版本，覆盖远程 */
    KEEP_LOCAL,
    /** 保留远程版本，覆盖本地 */
    KEEP_REMOTE,
    /** 同时保留两个版本（远程文件重命名） */
    KEEP_BOTH,
    /** 交由用户手动处理 */
    MANUAL
}

/**
 * 同步冲突信息
 * 当本地文件与远程文件存在不一致时，记录双方的元数据以便冲突解决
 *
 * @property localFile 本地文件的元数据信息
 * @property remoteFile 远程 Google Drive 文件的元数据信息
 * @property localModifiedTime 本地文件最后修改时间戳（毫秒）
 * @property remoteModifiedTime 远程文件最后修改时间戳（毫秒）
 * @property suggestedResolution 系统建议的冲突解决策略
 */
@Serializable
data class SyncConflictInfo(
    val localFile: GoogleDriveFile = GoogleDriveFile(),
    val remoteFile: GoogleDriveFile = GoogleDriveFile(),
    val localModifiedTime: Long = 0L,
    val remoteModifiedTime: Long = 0L,
    val suggestedResolution: GoogleDriveConflictResolution = GoogleDriveConflictResolution.MANUAL
) {
    /**
     * 判断本地文件是否比远程文件更新
     */
    fun isLocalNewer(): Boolean = localModifiedTime > remoteModifiedTime

    /**
     * 判断远程文件是否比本地文件更新
     */
    fun isRemoteNewer(): Boolean = remoteModifiedTime > localModifiedTime
}

// ==================== 认证状态模型 ====================

/**
 * Google Drive 认证状态
 * 记录当前用户的 Google 账号认证信息
 *
 * @property isSignedIn 是否已登录
 * @property accountEmail 已登录账号的邮箱地址
 * @property accountName 已登录账号的显示名称
 * @property lastAuthTime 上次成功认证的时间戳（毫秒）
 */
@Serializable
data class GoogleDriveAuthState(
    val isSignedIn: Boolean = false,
    val accountEmail: String = "",
    val accountName: String = "",
    val lastAuthTime: Long = 0L
)
