package com.appclonex.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.appclonex.core.freezer.AppFreezer
import com.appclonex.core.usermanager.MultiUserManager
import com.appclonex.data.model.ClonedApp
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * WidgetActionWorker 单元测试
 *
 * 测试覆盖：
 * - 冻结全部应用操作
 * - 前台操作转发
 * - 异常处理
 */
class WidgetActionWorkerTest {

    private lateinit var context: Context
    private lateinit var params: WorkerParameters
    private lateinit var appFreezer: AppFreezer
    private lateinit var multiUserManager: MultiUserManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        params = mockk(relaxed = true)
        appFreezer = mockk(relaxed = true)
        multiUserManager = mockk(relaxed = true)
    }

    @Test
    fun `freeze all apps should call freezer for each app`() = runBlocking {
        // 准备测试数据
        val clonedApps = listOf(
            ClonedApp("com.app1", "App1", 10),
            ClonedApp("com.app2", "App2", 11),
            ClonedApp("com.app3", "App3", 12)
        )

        every { params.inputData.getString("action") } returns "action_freeze_all"
        coEvery { multiUserManager.getClonedApps() } returns clonedApps
        coEvery { appFreezer.freezeApp(any(), any()) } returns Result.success(Unit)

        // 创建 Worker
        val worker = WidgetActionWorker(context, params, appFreezer, multiUserManager)

        // 执行
        val result = worker.doWork()

        // 验证
        assertEquals(ListenableWorker.Result.success(), result)
        verify { appFreezer.freezeApp("com.app1", 10) }
        verify { appFreezer.freezeApp("com.app2", 11) }
        verify { appFreezer.freezeApp("com.app3", 12) }
    }

    @Test
    fun `freeze all with no apps should return success`() = runBlocking {
        every { params.inputData.getString("action") } returns "action_freeze_all"
        coEvery { multiUserManager.getClonedApps() } returns emptyList()

        val worker = WidgetActionWorker(context, params, appFreezer, multiUserManager)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `unknown action should return failure`() = runBlocking {
        every { params.inputData.getString("action") } returns "unknown_action"

        val worker = WidgetActionWorker(context, params, appFreezer, multiUserManager)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `null action should return failure`() = runBlocking {
        every { params.inputData.getString("action") } returns null

        val worker = WidgetActionWorker(context, params, appFreezer, multiUserManager)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `freeze failure should return failure result`() = runBlocking {
        val clonedApps = listOf(ClonedApp("com.app1", "App1", 10))

        every { params.inputData.getString("action") } returns "action_freeze_all"
        coEvery { multiUserManager.getClonedApps() } returns clonedApps
        coEvery { appFreezer.freezeApp(any(), any()) } throws RuntimeException("Freeze failed")

        val worker = WidgetActionWorker(context, params, appFreezer, multiUserManager)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `new profile action should return success`() = runBlocking {
        every { params.inputData.getString("action") } returns "action_new_profile"

        val worker = WidgetActionWorker(context, params, appFreezer, multiUserManager)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `quick clone action should return success`() = runBlocking {
        every { params.inputData.getString("action") } returns "action_quick_clone"

        val worker = WidgetActionWorker(context, params, appFreezer, multiUserManager)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `open app action should return success`() = runBlocking {
        every { params.inputData.getString("action") } returns "action_open_app"

        val worker = WidgetActionWorker(context, params, appFreezer, multiUserManager)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }
}
