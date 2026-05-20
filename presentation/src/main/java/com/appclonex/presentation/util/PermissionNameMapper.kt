package com.appclonex.presentation.util

object PermissionNameMapper {
    
    private val permissionNameMap = mapOf(
        "android.permission.CAMERA" to "相机",
        "android.permission.RECORD_AUDIO" to "麦克风",
        "android.permission.ACCESS_FINE_LOCATION" to "精确位置",
        "android.permission.ACCESS_COARSE_LOCATION" to "粗略位置",
        "android.permission.ACCESS_BACKGROUND_LOCATION" to "后台位置",
        "android.permission.READ_CONTACTS" to "读取联系人",
        "android.permission.WRITE_CONTACTS" to "写入联系人",
        "android.permission.READ_PHONE_STATE" to "读取电话状态",
        "android.permission.CALL_PHONE" to "拨打电话",
        "android.permission.SEND_SMS" to "发送短信",
        "android.permission.RECEIVE_SMS" to "接收短信",
        "android.permission.READ_SMS" to "读取短信",
        "android.permission.READ_EXTERNAL_STORAGE" to "读取存储",
        "android.permission.WRITE_EXTERNAL_STORAGE" to "写入存储",
        "android.permission.INTERNET" to "网络访问",
        "android.permission.ACCESS_NETWORK_STATE" to "网络状态",
        "android.permission.ACCESS_WIFI_STATE" to "WiFi 状态",
        "android.permission.BLUETOOTH" to "蓝牙",
        "android.permission.BLUETOOTH_ADMIN" to "蓝牙管理",
        "android.permission.NFC" to "NFC",
        "android.permission.SYSTEM_ALERT_WINDOW" to "悬浮窗",
        "android.permission.WRITE_SETTINGS" to "修改系统设置",
        "android.permission.REQUEST_INSTALL_PACKAGES" to "安装应用",
        "android.permission.PACKAGE_USAGE_STATS" to "使用情况统计"
    )
    
    fun getDisplayName(permission: String): String {
        return permissionNameMap[permission] ?: permission.substringAfterLast(".")
    }
    
    fun getDescription(permission: String): String {
        return when (permission) {
            "android.permission.CAMERA" -> "允许应用拍摄照片和录制视频"
            "android.permission.RECORD_AUDIO" -> "允许应用录制音频"
            "android.permission.ACCESS_FINE_LOCATION" -> "允许应用访问精确位置"
            "android.permission.ACCESS_COARSE_LOCATION" -> "允许应用访问大致位置"
            "android.permission.READ_CONTACTS" -> "允许应用读取联系人信息"
            "android.permission.READ_EXTERNAL_STORAGE" -> "允许应用读取设备存储"
            "android.permission.WRITE_EXTERNAL_STORAGE" -> "允许应用写入设备存储"
            "android.permission.INTERNET" -> "允许应用访问网络"
            else -> "允许应用使用此权限"
        }
    }
}
