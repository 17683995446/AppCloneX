package com.appclonex.application

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.appclonex.infrastructure.caching.ApplicationIconMemoryCacheManager
import com.appclonex.infrastructure.external.shizuku.ShizukuServiceConnectionProvider
import com.appclonex.infrastructure.logging.ApplicationLoggingServiceManager
import com.appclonex.infrastructure.utility.ApplicationMemoryOptimizationManager
import com.appclonex.infrastructure.utility.GlobalApplicationCrashExceptionHandler
import com.appclonex.worker.ApplicationStateSynchronizationWorker
import com.appclonex.worker.ConfigurationAutoBackupWorker
import com.appclonex.worker.StorageCleanupWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * AppCloneX Application类
 * 负责初始化全局组件、通知渠道等
 */
@HiltAndroidApp
class AppCloneXAndroidApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化日志系统（最先初始化，确保后续所有操作都能被日志记录）
        ApplicationLoggingServiceManager.init(this, BuildConfig.DEBUG)

        ApplicationLoggingServiceManager.i("App", "AppCloneX 启动")
        ApplicationLoggingServiceManager.i("App", ApplicationLoggingServiceManager.getDeviceInfo())

        // 初始化 Shizuku 服务连接
        ShizukuServiceConnectionProvider.init()

        // 初始化 Coil 图标缓存
        ApplicationIconMemoryCacheManager.initialize(this)

        // 初始化崩溃处理
        Thread.setDefaultUncaughtExceptionHandler(GlobalApplicationCrashExceptionHandler(this))

        // 初始化通知渠道
        createNotificationChannels()

        // 初始化内存管理
        ApplicationMemoryOptimizationManager.getInstance(this)

        // 调度周期性后台任务
        ApplicationStateSynchronizationWorker.schedule(this)
        ConfigurationAutoBackupWorker.schedule(this)
        StorageCleanupWorker.schedule(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        // 清理 Shizuku 服务连接
        ShizukuServiceConnectionProvider.destroy()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // 分身操作通知渠道
            val cloneChannel = NotificationChannel(
                CHANNEL_CLONE_OPERATIONS,
                getString(R.string.channel_clone_operations),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_clone_operations_desc)
            }
            notificationManager.createNotificationChannel(cloneChannel)

            // 安装进度通知渠道
            val installChannel = NotificationChannel(
                CHANNEL_INSTALL_PROGRESS,
                getString(R.string.channel_install_progress),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.channel_install_progress_desc)
            }
            notificationManager.createNotificationChannel(installChannel)
        }
    }

    companion object {
        lateinit var instance: AppCloneXAndroidApplication
            private set

        const val CHANNEL_CLONE_OPERATIONS = "clone_operations"
        const val CHANNEL_INSTALL_PROGRESS = "install_progress"
    }
}
