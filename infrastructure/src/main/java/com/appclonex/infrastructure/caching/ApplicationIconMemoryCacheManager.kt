package com.appclonex.infrastructure.caching

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用程序图标缓存管理器
 * 使用 Coil 的 ImageLoader、MemoryCache 和 DiskCache 自动管理缓存，
 * 替代手写的 LruCache 实现。
 *
 * 同时保留 getIcon / getIconDrawable 接口供非 Compose 场景使用（如快捷方式创建）。
 */
object ApplicationIconMemoryCacheManager {

    companion object {
        /**
         * 根据设备内存动态计算缓存大小百分比
         */
        private fun calculateCacheSizePercent(): Double {
            val maxMemory = Runtime.getRuntime().maxMemory()
            return when {
                maxMemory < 128 * 1024 * 1024L -> 0.05  // 低内存设备(<128MB)使用5%
                maxMemory < 256 * 1024 * 1024L -> 0.10  // 中等内存(<256MB)使用10%
                maxMemory < 512 * 1024 * 1024L -> 0.12  // 较高内存(<512MB)使用12%
                else -> 0.15  // 高内存设备使用15%
            }
        }
    }

    /**
     * 初始化 Coil 的 ImageLoader（带磁盘缓存）。
     * 应在 Application.onCreate 中调用一次。
     */
    fun initialize(context: Context) {
        val cacheSizePercent = calculateCacheSizePercent()
        val maxMemory = Runtime.getRuntime().maxMemory()
        val cacheSizeMB = (maxMemory * cacheSizePercent / 1024 / 1024).toInt()

        val imageLoader = ImageLoader.Builder(context)
            .components {
                // 注册自定义 Fetcher 和 Keyer，使 Coil 能处理 AppIconModel 数据
                add(ApplicationIconImageFetcherService.Factory())
                add(ApplicationIconKeyer())
            }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(cacheSizePercent)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("app_icon_cache"))
                    .maxSizeBytes(10L * 1024 * 1024) // 10MB 磁盘缓存
                    .build()
            }
            .respectCacheHeaders(false)
            .build()

        Coil.setImageLoader(imageLoader)
        com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager.d(
            "IconCache",
            "Cache initialized: ${cacheSizePercent * 100}% of memory (~${cacheSizeMB}MB)"
        )
    }

    /**
     * 获取应用图标 Bitmap（通过 Coil 缓存）
     *
     * @param context 上下文
     * @param packageName 应用包名
     * @return 图标 Bitmap，如果不存在返回 null
     */
    suspend fun getIcon(context: Context, packageName: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(ApplicationIconModel(packageName))
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCacheKey("app_icon:$packageName")
                    .diskCacheKey("app_icon:$packageName")
                    .size(Size(512, 512))
                    .build()

                val result = Coil.imageLoader(context).execute(request)
                if (result is SuccessResult) {
                    (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * 获取应用图标 Drawable（通过 Coil 缓存）
     *
     * @param context 上下文
     * @param packageName 应用包名
     * @return Drawable，如果不存在返回 null
     */
    suspend fun getIconDrawable(context: Context, packageName: String): Drawable? {
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(ApplicationIconModel(packageName))
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCacheKey("app_icon:$packageName")
                    .diskCacheKey("app_icon:$packageName")
                    .size(Size(512, 512))
                    .build()

                val result = Coil.imageLoader(context).execute(request)
                if (result is SuccessResult) {
                    result.drawable
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * 同步获取应用图标 Drawable（不走 Coil 缓存，用于快捷方式等非协程场景）
     *
     * @param context 上下文
     * @param packageName 应用包名
     * @return Drawable，如果不存在返回 null
     */
    fun getIconDrawableSync(context: Context, packageName: String): Drawable? {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            applicationInfo.loadIcon(packageManager)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 预加载应用图标到 Coil 缓存
     *
     * @param context 上下文
     * @param packageNames 应用包名列表
     */
    suspend fun preloadIcons(context: Context, packageNames: List<String>) {
        withContext(Dispatchers.IO) {
            packageNames.forEach { packageName ->
                try {
                    val request = ImageRequest.Builder(context)
                        .data(ApplicationIconModel(packageName))
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCacheKey("app_icon:$packageName")
                        .diskCacheKey("app_icon:$packageName")
                        .size(Size(512, 512))
                        .build()

                    Coil.imageLoader(context).execute(request)
                } catch (_: Exception) {
                    // 忽略单个图标加载失败
                }
            }
        }
    }

    /**
     * 清除 Coil 的内存缓存和磁盘缓存
     */
    fun clearCache(context: Context) {
        val imageLoader = Coil.imageLoader(context)
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }

    /**
     * 获取内存缓存大小（字节数）
     */
    fun getCacheSize(context: Context): Int {
        return Coil.imageLoader(context).memoryCache?.size?.toInt() ?: 0
    }
}

/**
 * 自定义数据模型，用于 Coil 通过包名加载应用图标。
 * 配合 ApplicationIconImageFetcherService 使用。
 */
data class ApplicationIconModel(val packageName: String)
