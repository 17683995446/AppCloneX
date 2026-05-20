package com.appclonex.infrastructure.system

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import com.appclonex.domain.model.ClonedApplicationInformationModel as ClonedApp
import java.util.UUID

/**
 * 应用程序快捷方式管理器
 * 用于创建和管理分身应用的桌面快捷方式
 */
class ApplicationDesktopShortcutManager(private val context: Context) {

    private val shortcutManager: ShortcutManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            context.getSystemService(ShortcutManager::class.java)
        } else {
            null
        }
    }

    /**
     * 创建分身应用快捷方式
     * @param clonedApp 克隆应用信息
     * @param icon 应用图标
     * @return 是否创建成功
     */
    fun createShortcut(clonedApp: ClonedApp, icon: Bitmap? = null): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return false
        }

        return try {
            val shortcutId = "clone_${clonedApp.profileUserId}_${clonedApp.packageName}_${UUID.randomUUID().hashCode()}"
            
            // 创建启动Intent
            val intent = createLaunchIntent(clonedApp)
            
            // 创建快捷方式信息
            val shortcutBuilder = ShortcutInfo.Builder(context, shortcutId)
                .setShortLabel(clonedApp.getDisplayName())
                .setLongLabel("${clonedApp.getDisplayName()} (分身)")
                .setIntent(intent)
            
            // 设置图标
            if (icon != null) {
                shortcutBuilder.setIcon(Icon.createWithBitmap(icon))
            } else {
                shortcutBuilder.setIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_manage))
            }
            
            val shortcut = shortcutBuilder.build()
            
            // 添加动态快捷方式
            shortcutManager?.addDynamicShortcuts(listOf(shortcut)) ?: false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 创建固定快捷方式（Android 8.0+）
     * @param clonedApp 克隆应用信息
     * @param icon 应用图标
     * @return 是否请求成功
     */
    fun requestPinShortcut(clonedApp: ClonedApp, icon: Bitmap? = null): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return createShortcut(clonedApp, icon)
        }

        return try {
            val shortcutId = "clone_${clonedApp.profileUserId}_${clonedApp.packageName}_${UUID.randomUUID().hashCode()}"
            
            // 创建启动Intent
            val intent = createLaunchIntent(clonedApp)
            
            // 创建快捷方式信息
            val shortcutBuilder = ShortcutInfo.Builder(context, shortcutId)
                .setShortLabel(clonedApp.getDisplayName())
                .setLongLabel("${clonedApp.getDisplayName()} (分身)")
                .setIntent(intent)
            
            // 设置图标
            if (icon != null) {
                shortcutBuilder.setIcon(Icon.createWithBitmap(icon))
            } else {
                shortcutBuilder.setIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_manage))
            }
            
            val shortcut = shortcutBuilder.build()
            
            // 请求固定快捷方式
            if (shortcutManager?.isRequestPinShortcutSupported == true) {
                shortcutManager?.requestPinShortcut(shortcut, null) ?: false
            } else {
                // 不支持固定，使用动态快捷方式
                createShortcut(clonedApp, icon)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 删除快捷方式
     * @param shortcutId 快捷方式ID
     */
    fun removeShortcut(shortcutId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                shortcutManager?.removeDynamicShortcuts(listOf(shortcutId))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 删除所有快捷方式
     */
    fun removeAllShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                shortcutManager?.removeAllDynamicShortcuts()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 获取所有快捷方式
     */
    fun getAllShortcuts(): List<ShortcutInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return emptyList()
        }
        return shortcutManager?.dynamicShortcuts ?: emptyList()
    }

    /**
     * 创建启动Intent
     * 使用显式Intent启动AppCloneX的LauncherActivity
     */
    private fun createLaunchIntent(clonedApp: ClonedApp): Intent {
        return Intent(context, com.appclonex.presentation.activity.ApplicationCloneLauncherActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("profileUserId", clonedApp.profileUserId)
            putExtra("packageName", clonedApp.packageName)
            putExtra("appName", clonedApp.getDisplayName())
        }
    }

    /**
     * 检查是否支持固定快捷方式
     */
    fun isPinShortcutSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false
        }
        return shortcutManager?.isRequestPinShortcutSupported ?: false
    }
}
