package com.appclonex.infrastructure.caching

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Coil Fetcher：通过应用包名从 PackageManager 加载应用图标。
 *
 * 注册后，Coil 可以直接处理 ApplicationIconModel 类型的数据，
 * 并自动享受 Coil 的 MemoryCache / DiskCache 缓存能力。
 */
class ApplicationIconImageFetcherService(
    private val data: ApplicationIconModel,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return withContext(Dispatchers.IO) {
            val context = options.context
            val packageManager = context.packageManager

            val applicationInfo = packageManager.getApplicationInfo(data.packageName, 0)
            val drawable = applicationInfo.loadIcon(packageManager)

            val bitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                else -> {
                    val width = drawable.intrinsicWidth.coerceAtLeast(1)
                    val height = drawable.intrinsicHeight.coerceAtLeast(1)
                    val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bmp
                }
            }

            DrawableResult(
                drawable = BitmapDrawable(options.context.resources, bitmap),
                isSampled = false,
                dataSource = DataSource.DISK
            )
        }
    }

    /**
     * Factory：创建 ApplicationIconImageFetcherService 实例
     */
    class Factory : Fetcher.Factory<ApplicationIconModel> {
        override fun create(data: ApplicationIconModel, options: Options, imageLoader: ImageLoader): Fetcher {
            return ApplicationIconImageFetcherService(data, options)
        }
    }
}

/**
 * Keyer：为 ApplicationIconModel 生成缓存 key，确保相同包名命中同一缓存。
 */
class ApplicationIconKeyer : Keyer<ApplicationIconModel> {
    override fun key(data: ApplicationIconModel, options: Options): String {
        return "app_icon:${data.packageName}"
    }
}
