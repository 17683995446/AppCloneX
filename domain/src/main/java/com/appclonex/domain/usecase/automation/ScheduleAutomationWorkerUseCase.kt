package com.appclonex.domain.usecase.automation

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 调度自动化Worker用例
 *
 * 提供统一的Worker调度接口
 */
@Singleton
class ScheduleAutomationWorkerUseCase @Inject constructor() {

    /**
     * 获取自动冻结Worker的调度间隔（分钟）
     */
    fun getAutoFreezeIntervalMinutes(): Int = 15

    /**
     * 获取存储清理Worker的调度间隔（小时）
     */
    fun getStorageCleanupIntervalHours(): Int = 24

    /**
     * 是否应该启用自动冻结
     */
    fun shouldEnableAutoFreeze(): Boolean = true

    /**
     * 是否应该启用存储清理
     */
    fun shouldEnableStorageCleanup(): Boolean = true
}
