package com.appclonex.infrastructure.logging

import com.appclonex.domain.usecase.monitoring.CrashReportingService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 崩溃报告服务实现
 */
@Singleton
class CrashReportingServiceImpl @Inject constructor(
    private val firebaseCrashReportingService: FirebaseCrashReportingService
) : CrashReportingService {

    override fun report(throwable: Throwable, context: Map<String, String>) {
        firebaseCrashReportingService.recordException(throwable, context)
    }
}
