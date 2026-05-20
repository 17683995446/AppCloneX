package com.appclonex.infrastructure.utility

import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import java.io.File
import java.util.UUID

/**
 * 原子文件写入工具
 *
 * 使用"先写临时文件再重命名"策略，确保写入过程中崩溃不会损坏目标文件。
 * 
 * ## 安全性说明
 * - 处理parentFile为null的情况（如根目录文件）
 * - 临时文件在写入完成后自动清理
 * - 支持跨文件系统的回退写入
 * 
 * ## R14-005 修复：临时文件名唯一性
 * - 使用 UUID 生成唯一临时文件名，避免并发写入冲突
 * - 回退写入前备份原始文件，防止崩溃导致数据损坏
 * - 添加符号链接攻击防护
 */
object AtomicFileWriter {

    private const val TAG = "AtomicFileWriter"

    /**
     * 原子写入字符串内容到文件
     *
     * R14-005 修复：使用唯一临时文件名
     *
     * @param file 目标文件
     * @param content 要写入的内容
     * @throws SecurityException 如果没有写入权限
     */
    fun write(file: File, content: String) {
        val parent = file.parentFile
        if (parent == null || !parent.exists()) {
            file.writeText(content, Charsets.UTF_8)
            return
        }

        // R14-005 修复：使用 UUID 生成唯一临时文件名
        val tempFile = File(parent, ".${file.name}.${UUID.randomUUID()}.tmp")
        try {
            // R14-005 修复：检查符号链接攻击
            if (file.exists() && isSymlink(file)) {
                ApplicationLoggingServiceManager.w(TAG, "检测到符号链接，跳过写入: ${file.absolutePath}")
                return
            }

            tempFile.writeText(content, Charsets.UTF_8)
            val renamed = tempFile.renameTo(file)
            if (!renamed) {
                // R14-005 修复：回退写入前备份原始文件
                val backupFile = File(parent, ".${file.name}.bak")
                try {
                    if (file.exists()) {
                        file.copyTo(backupFile, overwrite = true)
                    }
                    file.writeText(content, Charsets.UTF_8)
                    backupFile.delete()
                } catch (e: Exception) {
                    ApplicationLoggingServiceManager.e(TAG, "回退写入失败", e)
                    // 尝试从备份恢复
                    if (backupFile.exists()) {
                        try {
                            backupFile.copyTo(file, overwrite = true)
                        } catch (recoveryError: Exception) {
                            ApplicationLoggingServiceManager.e(TAG, "从备份恢复失败", recoveryError)
                        }
                    }
                    throw e
                }
            }
        } finally {
            tempFile.delete()
        }
    }

    /**
     * 原子写入字节数组到文件
     *
     * R14-005 修复：使用唯一临时文件名
     *
     * @param file 目标文件
     * @param bytes 要写入的字节数组
     * @throws SecurityException 如果没有写入权限
     */
    fun writeBytes(file: File, bytes: ByteArray) {
        val parent = file.parentFile
        if (parent == null || !parent.exists()) {
            file.writeBytes(bytes)
            return
        }

        // R14-005 修复：使用 UUID 生成唯一临时文件名
        val tempFile = File(parent, ".${file.name}.${UUID.randomUUID()}.tmp")
        try {
            if (file.exists() && isSymlink(file)) {
                ApplicationLoggingServiceManager.w(TAG, "检测到符号链接，跳过写入: ${file.absolutePath}")
                return
            }

            tempFile.writeBytes(bytes)
            val renamed = tempFile.renameTo(file)
            if (!renamed) {
                val backupFile = File(parent, ".${file.name}.bak")
                try {
                    if (file.exists()) {
                        file.copyTo(backupFile, overwrite = true)
                    }
                    file.writeBytes(bytes)
                    backupFile.delete()
                } catch (e: Exception) {
                    ApplicationLoggingServiceManager.e(TAG, "回退写入失败", e)
                    if (backupFile.exists()) {
                        try {
                            backupFile.copyTo(file, overwrite = true)
                        } catch (recoveryError: Exception) {
                            ApplicationLoggingServiceManager.e(TAG, "从备份恢复失败", recoveryError)
                        }
                    }
                    throw e
                }
            }
        } finally {
            tempFile.delete()
        }
    }

    /**
     * R14-005 修复：检查文件是否为符号链接
     */
    private fun isSymlink(file: File): Boolean {
        return try {
            val canonicalPath = file.canonicalPath
            val absolutePath = file.absolutePath
            canonicalPath != absolutePath
        } catch (e: Exception) {
            false
        }
    }
}
