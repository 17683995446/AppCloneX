package com.appclonex.infrastructure.persistence.sandbox

import android.content.Context
import android.content.SharedPreferences
import android.util.JsonReader
import android.util.JsonWriter
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 沙盒信息数据类
 */
data class SandboxInfo(
    val userId: Int,
    val sizeBytes: Long,
    val createdTime: Long,
    val appCount: Int,
    val sharedPrefsSize: Long = 0,
    val filesSize: Long = 0,
    val cacheSize: Long = 0,
    val databaseSize: Long = 0
)

/**
 * 沙盒存储详情
 */
data class SandboxStorageDetail(
    val sharedPrefsSize: Long,
    val filesSize: Long,
    val cacheSize: Long,
    val databaseSize: Long,
    val totalSize: Long
)

/**
 * 沙盒数据管理器

 * 为克隆应用创建独立的虚拟数据空间，实现：
 * - 独立的 SharedPreferences 存储
 * - 独立的文件存储空间
 * - 独立的数据库隔离
 * - 可随时清除沙盒数据（"重置应用"功能）
 * - 支持沙盒数据导出/导入（备份恢复）

 * 路径结构:
 * /data/data/com.appclonex/sandboxes/{userId}/
 *   ├── shared_prefs/     # SharedPreferences
 *   ├── files/            # 文件存储
 *   ├── cache/            # 缓存
 *   ├── databases/        # 数据库
 *   └── sandbox_meta.json # 沙盒元数据
 */
@Singleton
class ApplicationSandboxDataManagementService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SandboxDataManager"
        private const val SANDBOX_DIR = "sandboxes"
        private const val META_FILE = "sandbox_meta.json"
        private const val SHARED_PREFS_DIR = "shared_prefs"
        private const val FILES_DIR = "files"
        private const val CACHE_DIR = "cache"
        private const val DATABASES_DIR = "databases"
        
        // 备份文件扩展名
        private const val BACKUP_EXTENSION = ".sbx"
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * 目录大小缓存
     */
    private val dirSizeCache = ConcurrentHashMap<String, Pair<Long, Long>>() // path -> (size, timestamp)
    private val CACHE_VALIDITY_MS = 30_000L // 缓存有效期30秒
    
    /**
     * 为指定用户创建沙盒目录
     * 路径: /data/data/com.appclonex/sandboxes/{userId}/
     */
    fun createSandbox(userId: Int): Result<File> {
        return try {
            val sandboxDir = getSandboxDir(userId)
            
            if (sandboxDir == null) {
                val baseDir = File(context.filesDir, SANDBOX_DIR)
                if (!baseDir.exists()) {
                    baseDir.mkdirs()
                }
                
                val newSandboxDir = File(baseDir, userId.toString())
                if (!newSandboxDir.exists()) {
                    // 创建子目录
                    File(newSandboxDir, SHARED_PREFS_DIR).mkdirs()
                    File(newSandboxDir, FILES_DIR).mkdirs()
                    File(newSandboxDir, CACHE_DIR).mkdirs()
                    File(newSandboxDir, DATABASES_DIR).mkdirs()
                    
                    // 创建元数据文件
                    createMetaFile(newSandboxDir, userId)
                    
                    ApplicationLoggingServiceManager.i(TAG, "沙盒创建成功: userId=$userId, path=${newSandboxDir.absolutePath}")
                    Result.success(newSandboxDir)
                } else {
                    ApplicationLoggingServiceManager.w(TAG, "沙盒已存在: userId=$userId")
                    Result.success(newSandboxDir)
                }
            } else {
                ApplicationLoggingServiceManager.i(TAG, "沙盒已存在，返回现有目录: userId=$userId")
                Result.success(sandboxDir)
            }
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "创建沙盒失败: userId=$userId", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取沙盒目录
     */
    fun getSandboxDir(userId: Int): File? {
        val sandboxDir = File(context.filesDir, "$SANDBOX_DIR/$userId")
        return if (sandboxDir.exists()) sandboxDir else null
    }
    
    /**
     * 获取沙盒内的 SharedPreferences
     * 使用自定义目录存储，实现数据隔离
     */
    fun getSandboxSharedPreferences(userId: Int, name: String): SharedPreferences {
        // 确保沙盒存在
        val sandboxDir = getSandboxDir(userId) ?: createSandbox(userId).getOrNull()
            ?: throw IllegalStateException("Failed to create sandbox for user $userId")
        
        val prefsDir = File(sandboxDir, SHARED_PREFS_DIR)
        if (!prefsDir.exists()) {
            prefsDir.mkdirs()
        }
        
        // 使用独立的 SandboxIsolatedSharedPreferencesImplementation 实现（支持 FileObserver 监听器）
        return SandboxIsolatedSharedPreferencesImplementation(prefsDir, name)
    }
    
    /**
     * 获取沙盒内的文件目录
     */
    fun getSandboxFilesDir(userId: Int): File? {
        val sandboxDir = getSandboxDir(userId) ?: return null
        val filesDir = File(sandboxDir, FILES_DIR)
        return if (filesDir.exists()) filesDir else null
    }
    
    /**
     * 获取沙盒内的缓存目录
     */
    fun getSandboxCacheDir(userId: Int): File? {
        val sandboxDir = getSandboxDir(userId) ?: return null
        val cacheDir = File(sandboxDir, CACHE_DIR)
        return if (cacheDir.exists()) cacheDir else null
    }
    
    /**
     * 获取沙盒内的数据库目录
     */
    fun getSandboxDatabasesDir(userId: Int): File? {
        val sandboxDir = getSandboxDir(userId) ?: return null
        val dbDir = File(sandboxDir, DATABASES_DIR)
        return if (dbDir.exists()) dbDir else null
    }
    
    /**
     * 清除沙盒所有数据（重置应用）
     */
    fun clearSandbox(userId: Int): Result<Unit> {
        return try {
            val sandboxDir = getSandboxDir(userId)
            
            if (sandboxDir == null) {
                ApplicationLoggingServiceManager.w(TAG, "沙盒不存在，无需清除: userId=$userId")
                return Result.success(Unit)
            }
            
            // 递归删除所有内容
            deleteRecursive(sandboxDir)
            
            // 重新创建空沙盒
            createSandbox(userId)
            
            ApplicationLoggingServiceManager.i(TAG, "沙盒清除成功: userId=$userId")
            ApplicationLoggingServiceManager.logOperation("CLEAR_SANDBOX", "userId=$userId")
            
            Result.success(Unit)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "清除沙盒失败: userId=$userId", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取沙盒存储使用量
     */
    fun getSandboxSize(userId: Int): Long {
        val sandboxDir = getSandboxDir(userId) ?: return 0
        return calculateDirSize(sandboxDir)
    }
    
    /**
     * 获取沙盒存储详情
     */
    fun getSandboxStorageDetail(userId: Int): SandboxStorageDetail {
        val sandboxDir = getSandboxDir(userId)
        
        if (sandboxDir == null) {
            return SandboxStorageDetail(0, 0, 0, 0, 0)
        }
        
        val sharedPrefsSize = calculateDirSize(File(sandboxDir, SHARED_PREFS_DIR))
        val filesSize = calculateDirSize(File(sandboxDir, FILES_DIR))
        val cacheSize = calculateDirSize(File(sandboxDir, CACHE_DIR))
        val databaseSize = calculateDirSize(File(sandboxDir, DATABASES_DIR))
        val totalSize = sharedPrefsSize + filesSize + cacheSize + databaseSize
        
        return SandboxStorageDetail(
            sharedPrefsSize = sharedPrefsSize,
            filesSize = filesSize,
            cacheSize = cacheSize,
            databaseSize = databaseSize,
            totalSize = totalSize
        )
    }
    
    /**
     * 导出沙盒数据（备份）
     * 将沙盒数据打包为 zip 文件
     */
    suspend fun exportSandbox(userId: Int, destFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sandboxDir = getSandboxDir(userId)
            
            if (sandboxDir == null) {
                return@withContext Result.failure(Exception("沙盒不存在"))
            }
            
            // 创建临时备份目录
            val tempDir = File(context.cacheDir, "sandbox_backup_$userId")
            if (tempDir.exists()) {
                deleteRecursive(tempDir)
            }
            tempDir.mkdirs()
            
            // 复制沙盒内容到临时目录
            copyDirectory(sandboxDir, tempDir)
            
            // 创建 zip 文件
            ZipOutputStream(FileOutputStream(destFile)).use { zipOut ->
                zipDirectory(tempDir, "", zipOut)
            }
            
            // 清理临时目录
            deleteRecursive(tempDir)
            
            ApplicationLoggingServiceManager.i(TAG, "沙盒导出成功: userId=$userId, dest=${destFile.absolutePath}")
            ApplicationLoggingServiceManager.logOperation("EXPORT_SANDBOX", "userId=$userId, size=${destFile.length()}")
            
            Result.success(Unit)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "导出沙盒失败: userId=$userId", e)
            Result.failure(e)
        }
    }
    
    /**
     * 导入沙盒数据（恢复）
     * 从 zip 文件恢复沙盒数据
     */
    suspend fun importSandbox(userId: Int, sourceFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("备份文件不存在"))
            }
            
            // 清除现有沙盒
            val sandboxDir = getSandboxDir(userId)
            if (sandboxDir != null) {
                deleteRecursive(sandboxDir)
            }
            
            // 创建新沙盒
            createSandbox(userId)
            val newSandboxDir = getSandboxDir(userId)!!
            
            // 解压备份文件
            ZipInputStream(FileInputStream(sourceFile)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    val file = File(newSandboxDir, entry.name)

                    // 防止 Zip Slip 路径遍历攻击
                    if (!file.canonicalPath.startsWith(newSandboxDir.canonicalPath)) {
                        throw SecurityException("Zip entry path traversal detected: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { output ->
                            zipIn.copyTo(output)
                        }
                    }
                    
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            
            ApplicationLoggingServiceManager.i(TAG, "沙盒导入成功: userId=$userId, source=${sourceFile.absolutePath}")
            ApplicationLoggingServiceManager.logOperation("IMPORT_SANDBOX", "userId=$userId, size=${sourceFile.length()}")
            
            Result.success(Unit)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "导入沙盒失败: userId=$userId", e)
            Result.failure(e)
        }
    }
    
    /**
     * 列出所有沙盒
     */
    fun listSandboxes(): List<SandboxInfo> {
        val baseDir = File(context.filesDir, SANDBOX_DIR)
        
        if (!baseDir.exists()) {
            return emptyList()
        }
        
        return baseDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir ->
                val userId = dir.name.toIntOrNull() ?: return@mapNotNull null
                val metaFile = File(dir, META_FILE)
                val createdTime = if (metaFile.exists()) {
                    metaFile.lastModified()
                } else {
                    dir.lastModified()
                }
                
                // 计算应用数量（基于 shared_prefs 目录中的文件数）
                val prefsDir = File(dir, SHARED_PREFS_DIR)
                val appCount = if (prefsDir.exists()) {
                    prefsDir.listFiles()?.count { it.extension == "json" } ?: 0
                } else {
                    0
                }
                
                val storageDetail = getSandboxStorageDetail(userId)
                
                SandboxInfo(
                    userId = userId,
                    sizeBytes = storageDetail.totalSize,
                    createdTime = createdTime,
                    appCount = appCount,
                    sharedPrefsSize = storageDetail.sharedPrefsSize,
                    filesSize = storageDetail.filesSize,
                    cacheSize = storageDetail.cacheSize,
                    databaseSize = storageDetail.databaseSize
                )
            }
            ?.sortedByDescending { it.createdTime }
            ?: emptyList()
    }
    
    /**
     * 检查沙盒是否存在
     */
    fun sandboxExists(userId: Int): Boolean {
        return getSandboxDir(userId) != null
    }
    
    /**
     * 删除沙盒（完全删除，不可恢复）
     */
    fun deleteSandbox(userId: Int): Result<Unit> {
        return try {
            val sandboxDir = getSandboxDir(userId)
            
            if (sandboxDir != null) {
                deleteRecursive(sandboxDir)
                ApplicationLoggingServiceManager.i(TAG, "沙盒删除成功: userId=$userId")
                ApplicationLoggingServiceManager.logOperation("DELETE_SANDBOX", "userId=$userId")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            ApplicationLoggingServiceManager.e(TAG, "删除沙盒失败: userId=$userId", e)
            Result.failure(e)
        }
    }
    
    /**
     * 格式化存储大小
     */
    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    private fun createMetaFile(sandboxDir: File, userId: Int) {
        val metaFile = File(sandboxDir, META_FILE)
        JsonWriter(OutputStreamWriter(FileOutputStream(metaFile))).use { writer ->
            writer.beginObject()
            writer.name("userId").value(userId)
            writer.name("createdTime").value(System.currentTimeMillis())
            writer.name("createdTimeFormatted").value(dateFormat.format(Date()))
            writer.name("version").value(1)
            writer.endObject()
        }
    }
    
    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0

        return dir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }

    /**
     * 计算目录大小（带缓存）
     */
    private fun calculateDirSizeWithCache(dir: File): Long {
        val path = dir.absolutePath
        val now = System.currentTimeMillis()

        // 检查缓存
        dirSizeCache[path]?.let { (cachedSize, timestamp) ->
            if (now - timestamp < CACHE_VALIDITY_MS) {
                return cachedSize
            }
        }

        // 计算新大小
        val size = calculateDirSize(dir)
        dirSizeCache[path] = size to now
        return size
    }

    /**
     * 清除目录大小缓存
     */
    fun clearDirSizeCache(path: String? = null) {
        if (path == null) {
            dirSizeCache.clear()
        } else {
            dirSizeCache.remove(path)
        }
    }
    
    private fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        file.delete()
    }
    
    private fun copyDirectory(source: File, dest: File) {
        if (source.isDirectory) {
            if (!dest.exists()) {
                dest.mkdirs()
            }
            source.listFiles()?.forEach { child ->
                copyDirectory(child, File(dest, child.name))
            }
        } else {
            source.copyTo(dest, overwrite: true)
        }
    }
    
    private fun zipDirectory(dir: File, parentPath: String, zipOut: ZipOutputStream) {
        dir.listFiles()?.forEach { file ->
            val path = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
            
            if (file.isDirectory) {
                zipOut.putNextEntry(ZipEntry("$path/"))
                zipOut.closeEntry()
                zipDirectory(file, path, zipOut)
            } else {
                zipOut.putNextEntry(ZipEntry(path))
                FileInputStream(file).use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
        }
    }
}
