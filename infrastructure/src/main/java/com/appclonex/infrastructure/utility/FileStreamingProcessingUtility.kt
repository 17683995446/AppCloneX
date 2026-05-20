package com.appclonex.infrastructure.utility

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 文件流式处理工具

 * 解决 P1 缺陷：大文件导出 OOM
 * 使用流式读写替代全量加载，支持分块处理
 */
object FileStreamingProcessingUtility {

    /**
     * 默认缓冲区大小：8KB
     */
    const val DEFAULT_BUFFER_SIZE = 8192

    /**
     * 最大单文件大小：500MB
     */
    const val MAX_SINGLE_FILE_SIZE = 500L * 1024 * 1024

    /**
     * 流式复制文件
     * 使用固定大小缓冲区，避免一次性加载大文件到内存
     */
    @Throws(IOException::class)
    fun copyFileStream(
        source: File,
        destination: File,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        progressCallback: ((bytesCopied: Long, totalBytes: Long) -> Unit)? = null
    ): Long {
        var bytesCopied: Long = 0
        val totalBytes = source.length()

        source.inputStream().buffered(bufferSize).use { input ->
            destination.outputStream().buffered(bufferSize).use { output ->
                val buffer = ByteArray(bufferSize)
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead
                    progressCallback?.invoke(bytesCopied, totalBytes)
                }
            }
        }

        return bytesCopied
    }

    /**
     * 流式创建 ZIP 压缩包
     * 逐个文件添加到 ZIP，避免将所有文件内容加载到内存
     */
    @Throws(IOException::class)
    fun createZipArchive(
        sourceDirectory: File,
        destinationZipFile: File,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        progressCallback: ((currentFile: String, filesProcessed: Int, totalFiles: Int) -> Unit)? = null
    ) {
        val files = sourceDirectory.walkTopDown().filter { it.isFile }.toList()

        ZipOutputStream(destinationZipFile.outputStream().buffered(bufferSize)).use { zipOut ->
            files.forEachIndexed { index, file ->
                val entryName = sourceDirectory.toPath()
                    .relativize(file.toPath())
                    .toString()
                    .replace("\\", "/")

                zipOut.putNextEntry(ZipEntry(entryName))

                file.inputStream().buffered(bufferSize).use { input ->
                    val buffer = ByteArray(bufferSize)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        zipOut.write(buffer, 0, bytesRead)
                    }
                }

                zipOut.closeEntry()
                progressCallback?.invoke(file.name, index + 1, files.size)
            }
        }
    }

    /**
     * 流式解压 ZIP 压缩包
     * 逐个文件解压，避免将所有文件内容加载到内存
     */
    @Throws(IOException::class)
    fun extractZipArchive(
        sourceZipFile: File,
        destinationDirectory: File,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        progressCallback: ((currentFile: String, filesProcessed: Int, totalFiles: Int) -> Unit)? = null
    ) {
        destinationDirectory.mkdirs()

        ZipInputStream(sourceZipFile.inputStream().buffered(bufferSize)).use { zipIn ->
            var filesProcessed = 0
            generateSequence { zipIn.nextEntry }.forEach { zipEntry ->
                if (!zipEntry.isDirectory) {
                    val outputFile = File(destinationDirectory, zipEntry.name)
                    // 防止 Zip Slip 路径遍历攻击
                    if (!outputFile.canonicalPath.startsWith(destinationDirectory.canonicalPath)) {
                        throw SecurityException("Zip entry path traversal detected: ${zipEntry.name}")
                    }
                    outputFile.parentFile?.mkdirs()
                    outputFile.outputStream().buffered(bufferSize).use { output ->
                        val buffer = ByteArray(bufferSize)
                        var bytesRead: Int
                        while (zipIn.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                    filesProcessed++
                    progressCallback?.invoke(zipEntry.name, filesProcessed, 0)
                }
            }
        }
    }

    /**
     * 流式计算目录大小
     * 不加载文件内容，仅统计大小
     */
    fun calculateDirectorySize(directory: File): Long {
        return directory.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    /**
     * 流式计算 ZIP 文件大小（解压后）
     */
    fun calculateZipContentSize(zipFile: File): Long {
        var totalSize = 0L

        ZipInputStream(zipFile.inputStream()).use { zipIn ->
            generateSequence { zipIn.nextEntry }.forEach { entry ->
                if (!entry.isDirectory) {
                    totalSize += entry.size
                }
            }
        }

        return totalSize
    }

    /**
     * 检查文件是否超过最大大小限制
     */
    fun isFileSizeWithinLimit(file: File, maxSizeBytes: Long = MAX_SINGLE_FILE_SIZE): Boolean {
        return file.length() <= maxSizeBytes
    }

    /**
     * 安全删除目录（流式遍历删除）
     */
    fun deleteDirectorySafely(directory: File): Boolean {
        if (!directory.exists()) return true

        return directory.walkBottomUp().fold(true) { success, file ->
            success && (file.delete() || !file.exists())
        }
    }
}
