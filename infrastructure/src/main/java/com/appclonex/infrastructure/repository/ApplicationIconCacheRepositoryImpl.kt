package com.appclonex.infrastructure.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.appclonex.domain.repository.ApplicationIconCacheRepository
import com.appclonex.infrastructure.caching.ApplicationIconMemoryCacheManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用图标缓存仓库实现
 */
@Singleton
class ApplicationIconCacheRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ApplicationIconCacheRepository {
    
    init {
        ApplicationIconMemoryCacheManager.initialize(context)
    }
    
    override suspend fun getIcon(context: Context, packageName: String): Bitmap? {
        return ApplicationIconMemoryCacheManager.getIcon(context, packageName)
    }
    
    override suspend fun getIconDrawable(context: Context, packageName: String): Drawable? {
        return ApplicationIconMemoryCacheManager.getIconDrawable(context, packageName)
    }
    
    override fun getIconDrawableSync(context: Context, packageName: String): Drawable? {
        return ApplicationIconMemoryCacheManager.getIconDrawableSync(context, packageName)
    }
    
    override suspend fun preloadIcons(context: Context, packageNames: List<String>) {
        ApplicationIconMemoryCacheManager.preloadIcons(context, packageNames)
    }
    
    override fun clearCache(context: Context) {
        ApplicationIconMemoryCacheManager.clearCache(context)
    }
    
    override fun getCacheSize(context: Context): Int {
        return ApplicationIconMemoryCacheManager.getCacheSize(context)
    }
}
