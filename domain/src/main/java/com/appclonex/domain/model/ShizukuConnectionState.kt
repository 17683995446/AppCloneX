package com.appclonex.domain.model

/**
 * Shizuku连接状态
 */
enum class ShizukuConnectionState {
    DISCONNECTED,   // 未连接
    CONNECTING,     // 连接中
    CONNECTED,      // 已连接
    PERMISSION_REQUIRED,  // 需要授权
    ERROR          // 错误
}
