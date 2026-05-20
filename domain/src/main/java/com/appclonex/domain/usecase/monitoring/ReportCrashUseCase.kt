package com.appclonex.domain.usecase.monitoring

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 报告崩溃用例
 *
 * 用于在非致命异常发生时上报到崩溃报告服务
 */
@Singleton
class ReportCrashUseCase @Inject constructor(
    private val crashReportingService: CrashReportingService
) {
    /**
     * 执行崩溃报告
     *
     * @param throwable 异常
     * @param context 上下文信息
     */
    operator fun invoke(throwable: Throwable, context: Map<String, String> = emptyMap()) {
        crashReportingService.report(throwable, context)
    }
}

/**
 * 崩溃报告服务接口（Domain层定义）
 */
interface CrashReportingService {
    fun report(throwable: Throwable, context: Map<String, String>)
}
