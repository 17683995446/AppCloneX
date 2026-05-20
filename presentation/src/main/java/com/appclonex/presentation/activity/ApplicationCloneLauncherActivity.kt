package com.appclonex.presentation.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * 克隆应用启动器 Activity
 * 用于启动指定用户空间中的克隆应用
 */
@AndroidEntryPoint
class ApplicationCloneLauncherActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val profileUserId = intent.getIntExtra("profileUserId", 0)
        val packageName = intent.getStringExtra("packageName") ?: run {
            finish()
            return
        }
        
        // 启动克隆应用
        launchClonedApp(profileUserId, packageName)
        
        finish()
    }
    
    private fun launchClonedApp(userId: Int, packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivityAsUser(launchIntent, android.os.UserHandle.of(userId))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
