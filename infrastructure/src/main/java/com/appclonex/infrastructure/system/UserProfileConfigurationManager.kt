package com.appclonex.infrastructure.system

import android.content.Context
import android.content.pm.UserInfo
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.appclonex.infrastructure.external.shizuku.ShizukuException
import com.appclonex.infrastructure.external.shizuku.ShizukuServiceConnectionProvider
import com.appclonex.domain.model.UserProfileConfigurationModel as UserProfile
import com.appclonex.domain.model.UserProfileInfo
import com.appclonex.domain.model.UserProfileState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Method
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户配置文件管理器
 * 核心功能：通过Shizuku调用系统API创建和管理多个用户空间
 * 实现无上限分身的关键组件
 */
@Singleton
class UserProfileConfigurationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val systemUserManager: UserManager by lazy {
        context.getSystemService(Context.USER_SERVICE) as UserManager
    }

    /**
     * 获取所有用户列表
     */
    suspend fun getAllUsers(): Result<List<UserProfileInfo>> = withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            val userManager = ShizukuServiceConnectionProvider.getUserManager()
                ?: throw ShizukuException("无法获取UserManager服务")
            
            // 通过反射调用隐藏API获取用户列表
            val users = getUsersInternal(userManager)
            users.map { userInfo ->
                UserProfileInfo(
                    profile = UserProfile(
                        userId = userInfo.id,
                        profileName = userInfo.name ?: "用户${userInfo.id}",
                        serialNumber = userInfo.serialNumber,
                        isActive = userInfo.id == Process.myUserHandle().hashCode(),
                        isRunning = userInfo.isRunning,
                        clonedAppCount = 0 // 需要单独查询
                    ),
                    state = if (userInfo.isRunning) UserProfileState.RUNNING else UserProfileState.STOPPED
                )
            }
        }
    }

    /**
     * 创建新用户配置文件
     * @param name 用户名称
     * @return 创建的用户ID
     */
    suspend fun createUserProfile(name: String): Result<Int> = withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            val userManager = ShizukuServiceConnectionProvider.getUserManager()
                ?: throw ShizukuException("无法获取UserManager服务")
            
            // 使用反射调用隐藏API创建用户
            val userId = createUserInternal(userManager, name)
            userId
        }
    }

    /**
     * 删除用户配置文件
     * @param userId 用户ID
     */
    suspend fun deleteUserProfile(userId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            val userManager = ShizukuServiceConnectionProvider.getUserManager()
                ?: throw ShizukuException("无法获取UserManager服务")
            
            // 不能删除系统用户
            if (userId == 0) {
                throw ShizukuException("不能删除主用户")
            }
            
            deleteUserInternal(userManager, userId)
            true
        }
    }

    /**
     * 启动用户配置文件
     * @param userId 用户ID
     */
    suspend fun startUserProfile(userId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            val userManager = ShizukuServiceConnectionProvider.getUserManager()
                ?: throw ShizukuException("无法获取UserManager服务")
            
            // 使用ActivityManager启动用户
            startActivityManagerUser(userId)
            true
        }
    }

    /**
     * 停止用户配置文件
     * @param userId 用户ID
     */
    suspend fun stopUserProfile(userId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            val userManager = ShizukuServiceConnectionProvider.getUserManager()
                ?: throw ShizukuException("无法获取UserManager服务")
            
            // 不能停止当前用户
            if (userId == Process.myUserHandle().hashCode()) {
                throw ShizukuException("不能停止当前用户")
            }
            
            stopUserInternal(userManager, userId)
            true
        }
    }

    /**
     * 切换到指定用户
     * @param userId 目标用户ID
     */
    suspend fun switchToUser(userId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            switchUserInternal(userId)
            true
        }
    }

    /**
     * 获取用户信息
     * @param userId 用户ID
     */
    suspend fun getUserInfo(userId: Int): Result<UserProfileInfo?> = withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            val userManager = ShizukuServiceConnectionProvider.getUserManager()
                ?: throw ShizukuException("无法获取UserManager服务")
            
            val userInfo = getUserInfoInternal(userManager, userId)
            userInfo?.let {
                UserProfileInfo(
                    profile = UserProfile(
                        userId = it.id,
                        profileName = it.name ?: "用户${it.id}",
                        serialNumber = it.serialNumber,
                        isActive = it.id == Process.myUserHandle().hashCode(),
                        isRunning = it.isRunning
                    ),
                    state = if (it.isRunning) UserProfileState.RUNNING else UserProfileState.STOPPED
                )
            }
        }
    }

    /**
     * 获取最大用户数量限制
     */
    suspend fun getMaxUserCount(): Result<Int> = withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            val userManager = ShizukuServiceConnectionProvider.getUserManager()
                ?: throw ShizukuException("无法获取UserManager服务")
            
            getMaxUserCountInternal(userManager)
        }
    }

    /**
     * 获取当前用户数量
     */
    suspend fun getCurrentUserCount(): Result<Int> = withContext(Dispatchers.IO) {
        ShizukuServiceConnectionProvider.executeWithShizuku {
            val userManager = ShizukuServiceConnectionProvider.getUserManager()
                ?: throw ShizukuException("无法获取UserManager服务")
            
            getUsersInternal(userManager).size
        }
    }

    // ==================== 内部方法（使用反射调用隐藏API）====================

    /**
     * 获取用户列表（反射调用）
     */
    private fun getUsersInternal(userManager: android.os.IUserManager): List<UserInfo> {
        return try {
            // 尝试方法1：getUsers(boolean excludePartial)
            val method: Method = userManager.javaClass.getDeclaredMethod("getUsers", Boolean::class.java)
            method.invoke(userManager, true) as? List<UserInfo> ?: emptyList()
        } catch (e: NoSuchMethodException) {
            try {
                // 尝试方法2：getUsers(int excludeFlags)
                val method2: Method = userManager.javaClass.getDeclaredMethod("getUsers", Int::class.java)
                method2.invoke(userManager, 0) as? List<UserInfo> ?: emptyList()
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    /**
     * 创建用户（反射调用）
     * @return 创建的用户ID
     */
    private fun createUserInternal(userManager: android.os.IUserManager, name: String): Int {
        return try {
            // 方法1：createUser(String name, String userType)
            val method: Method = userManager.javaClass.getDeclaredMethod(
                "createUser", 
                String::class.java, 
                String::class.java
            )
            val userInfo = method.invoke(userManager, name, "android.os.usertype.full.SECONDARY") as? UserInfo
            userInfo?.id ?: -1
        } catch (e: NoSuchMethodException) {
            try {
                // 方法2：createUser(String name, int flags)
                val method2: Method = userManager.javaClass.getDeclaredMethod(
                    "createUser",
                    String::class.java,
                    Int::class.java
                )
                // flags: 0 = 普通用户
                val userInfo = method2.invoke(userManager, name, 0) as? UserInfo
                userInfo?.id ?: -1
            } catch (e2: Exception) {
                -1
            }
        }
    }

    /**
     * 删除用户（反射调用）
     */
    private fun deleteUserInternal(userManager: android.os.IUserManager, userId: Int) {
        try {
            val method: Method = userManager.javaClass.getDeclaredMethod("removeUser", Int::class.java)
            method.invoke(userManager, userId)
        } catch (e: Exception) {
            throw ShizukuException("删除用户失败: ${e.message}")
        }
    }

    /**
     * 停止用户（反射调用）
     */
    private fun stopUserInternal(userManager: android.os.IUserManager, userId: Int) {
        try {
            val method: Method = userManager.javaClass.getDeclaredMethod("stopUser", Int::class.java)
            method.invoke(userManager, userId)
        } catch (e: NoSuchMethodException) {
            // 尝试使用ActivityManager停止用户
            stopUserViaActivityManager(userId)
        }
    }

    /**
     * 获取用户信息（反射调用）
     */
    private fun getUserInfoInternal(userManager: android.os.IUserManager, userId: Int): UserInfo? {
        return try {
            val method: Method = userManager.javaClass.getDeclaredMethod("getUserInfo", Int::class.java)
            method.invoke(userManager, userId) as? UserInfo
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取最大用户数量（反射调用）
     */
    private fun getMaxUserCountInternal(userManager: android.os.IUserManager): Int {
        return try {
            val method: Method = userManager.javaClass.getDeclaredMethod("getMaxSupportedUsers")
            method.invoke(userManager) as? Int ?: 10
        } catch (e: Exception) {
            // 默认返回10，大多数设备支持至少10个用户
            10
        }
    }

    /**
     * 通过ActivityManager启动用户
     */
    private fun startActivityManagerUser(userId: Int) {
        try {
            val amClass = Class.forName("android.app.ActivityManager")
            val getServiceMethod = amClass.getDeclaredMethod("getService")
            val amService = getServiceMethod.invoke(null)
            
            val startUserMethod = amService.javaClass.getDeclaredMethod("startUserInBackground", Int::class.java)
            startUserMethod.invoke(amService, userId)
        } catch (e: Exception) {
            throw ShizukuException("启动用户失败: ${e.message}")
        }
    }

    /**
     * 通过ActivityManager停止用户
     */
    private fun stopUserViaActivityManager(userId: Int) {
        try {
            val amClass = Class.forName("android.app.ActivityManager")
            val getServiceMethod = amClass.getDeclaredMethod("getService")
            val amService = getServiceMethod.invoke(null)
            
            val stopUserMethod = amService.javaClass.getDeclaredMethod("stopUser", Int::class.java)
            stopUserMethod.invoke(amService, userId)
        } catch (e: Exception) {
            // 忽略错误
        }
    }

    /**
     * 切换用户（反射调用）
     */
    private fun switchUserInternal(userId: Int) {
        try {
            val amClass = Class.forName("android.app.ActivityManager")
            val getServiceMethod = amClass.getDeclaredMethod("getService")
            val amService = getServiceMethod.invoke(null)
            
            val switchUserMethod = amService.javaClass.getDeclaredMethod("switchUser", Int::class.java)
            switchUserMethod.invoke(amService, userId)
        } catch (e: Exception) {
            throw ShizukuException("切换用户失败: ${e.message}")
        }
    }

    companion object {
        const val USER_TYPE_SECONDARY = "android.os.usertype.full.SECONDARY"
        const val USER_TYPE_MANAGED_PROFILE = "android.os.usertype.profile.MANAGED"
    }
}
