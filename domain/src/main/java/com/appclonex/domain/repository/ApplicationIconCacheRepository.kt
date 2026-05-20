package com.appclonex.domain.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable

/**
 * 应用图标缓存仓库接口
 * 
 * 提供应用图标的缓存和获取操作
 */
interface ApplicationIconCacheRepository {
    /**
     * 获取应用图标 Bitmap
     *
     * @param context 上下文
     * @param packageName 应用包名
     * @return 图标 Bitmap，如果不存在返回 null
     */
    suspend fun getIcon(context: Context, packageName: String): Bitmap?
    
    /**
     * 获取应用图标 Drawable
     *
     * @param context 上下文
     * @param packageName 应用包名
     * @return Drawable，如果不存在返回 null
     */
    suspend fun getIconDrawable(context: Context, packageName: String): Drawable?
    
    /**
     * 同步获取应用图标 Drawable
     *
     * @param context 上下文
     * @param packageName 应用包名
     * @return Drawable，如果不存在返回 null
     */
    fun getIconDrawableSync(context: Context, packageName: String): Drawable?
    
    /**
     * 预加载应用图标到缓存
     *
     * @param context 上下文
     * @param packageNames 应用包名列表
     */
    suspend fun preloadIcons(context: Context, packageNames: List<String>)
    
    /**
     * 清除缓存
     */
    fun clearCache(context: Context)
    
    /**
     * 获取缓存大小（字节数）
     */
    fun getCacheSize(context: Context): Int
}
