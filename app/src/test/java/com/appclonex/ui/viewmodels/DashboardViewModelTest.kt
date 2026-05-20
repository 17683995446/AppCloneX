package com.appclonex.ui.viewmodels

import app.cash.turbine.test
import com.appclonex.data.model.ActivityRecord
import com.appclonex.data.model.TopUsedApp
import com.appclonex.data.model.UsageStatistics
import com.appclonex.infrastructure.persistence.datastore.ApplicationPreferencesDataStoreManager
import com.appclonex.utils.UsageTracker
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith
import io.mockk.junit5.MockKExtension

/**
 * DashboardViewModel 单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class DashboardViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var usageTracker: UsageTracker
    private lateinit var preferencesDataStoreManager: ApplicationPreferencesDataStoreManager

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        usageTracker = mockk(relaxed = true)
        preferencesDataStoreManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 创建 DashboardViewModel，mock UsageTracker.getStatistics() 返回指定的 Flow
     */
    private fun createViewModel(
        statisticsFlow: kotlinx.coroutines.flow.Flow<UsageStatistics> = flowOf(UsageStatistics())
    ): DashboardViewModel {
        every { usageTracker.getStatistics() } returns statisticsFlow
        return DashboardViewModel(usageTracker, preferencesDataStoreManager)
    }

    @Test
    fun `初始状态 isLoading 为 true`() = runTest {
        // DashboardUiState 默认 isLoading = true
        // 但由于 getStatistics() 立即发射数据，isLoading 会变为 false
        // 使用一个永不发射的 Flow 来验证初始状态
        val neverFlow = kotlinx.coroutines.flow.flow<UsageStatistics> { /* 永不发射 */ }
        val viewModel = createViewModel(statisticsFlow = neverFlow)

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isTrue()
            assertThat(initialState.statistics).isEqualTo(UsageStatistics())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `当 getStatistics 发射数据时 uiState 正确更新`() = runTest {
        val testStats = UsageStatistics(
            totalProfiles = 5,
            activeProfiles = 3,
            totalClonedApps = 12,
            totalLaunches = 100,
            totalFrozenApps = 2,
            mostUsedApp = "WeChat",
            mostUsedAppLaunches = 30,
            topUsedApps = listOf(
                TopUsedApp("com.tencent.mm", "WeChat", 30),
                TopUsedApp("com.tencent.qq", "QQ", 20)
            ),
            recentActivity = listOf(
                ActivityRecord("com.tencent.mm", "WeChat", "launch", 1000L)
            ),
            storageUsedBytes = 2048L
        )

        val viewModel = createViewModel(statisticsFlow = flowOf(testStats))

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.statistics).isEqualTo(testStats)
            assertThat(state.statistics.totalProfiles).isEqualTo(5)
            assertThat(state.statistics.activeProfiles).isEqualTo(3)
            assertThat(state.statistics.totalClonedApps).isEqualTo(12)
            assertThat(state.statistics.totalLaunches).isEqualTo(100)
            assertThat(state.statistics.mostUsedApp).isEqualTo("WeChat")
            assertThat(state.statistics.topUsedApps).hasSize(2)
            assertThat(state.statistics.recentActivity).hasSize(1)
            assertThat(state.statistics.storageUsedBytes).isEqualTo(2048L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `当 getStatistics 发射默认数据时 isLoading 变为 false`() = runTest {
        val viewModel = createViewModel(statisticsFlow = flowOf(UsageStatistics()))

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.statistics.totalProfiles).isEqualTo(0)
            assertThat(state.statistics.totalClonedApps).isEqualTo(0)
            assertThat(state.statistics.topUsedApps).isEmpty()
            assertThat(state.statistics.recentActivity).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh 触发 isLoading 重新变为 true`() = runTest {
        // 使用永不发射的 Flow，这样 refresh 后 isLoading 保持 true
        val neverFlow = kotlinx.coroutines.flow.flow<UsageStatistics> { }
        val viewModel = createViewModel(statisticsFlow = neverFlow)

        // 等待初始状态
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isTrue()

            // 调用 refresh
            viewModel.refresh()
            testDispatcher.scheduler.advanceUntilIdle()

            // refresh 将 isLoading 设为 true（已经是 true，但逻辑上会重新设置）
            val stateAfterRefresh = awaitItem()
            assertThat(stateAfterRefresh.isLoading).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh 后 Flow 发射新数据时 uiState 更新`() = runTest {
        val mutableFlow = kotlinx.coroutines.flow.MutableStateFlow(UsageStatistics())
        every { usageTracker.getStatistics() } returns mutableFlow

        val viewModel = DashboardViewModel(usageTracker, preferencesDataStoreManager)

        viewModel.uiState.test {
            // 等待初始数据
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isFalse()
            assertThat(initialState.statistics.totalProfiles).isEqualTo(0)

            // 调用 refresh
            viewModel.refresh()
            testDispatcher.scheduler.advanceUntilIdle()

            // refresh 后 isLoading 变为 true
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            // Flow 发射新数据
            val newStats = UsageStatistics(totalProfiles = 10, totalClonedApps = 25)
            mutableFlow.emit(newStats)
            testDispatcher.scheduler.advanceUntilIdle()

            // uiState 更新为新数据，isLoading 变为 false
            val updatedState = awaitItem()
            assertThat(updatedState.isLoading).isFalse()
            assertThat(updatedState.statistics.totalProfiles).isEqualTo(10)
            assertThat(updatedState.statistics.totalClonedApps).isEqualTo(25)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getStatistics 发射多次数据时 uiState 持续更新`() = runTest {
        val mutableFlow = kotlinx.coroutines.flow.MutableStateFlow(
            UsageStatistics(totalProfiles = 1)
        )
        every { usageTracker.getStatistics() } returns mutableFlow

        val viewModel = DashboardViewModel(usageTracker, preferencesDataStoreManager)

        viewModel.uiState.test {
            // 第一次发射
            val state1 = awaitItem()
            assertThat(state1.statistics.totalProfiles).isEqualTo(1)
            assertThat(state1.isLoading).isFalse()

            // 第二次发射
            mutableFlow.emit(UsageStatistics(totalProfiles = 2, totalLaunches = 50))
            testDispatcher.scheduler.advanceUntilIdle()

            val state2 = awaitItem()
            assertThat(state2.statistics.totalProfiles).isEqualTo(2)
            assertThat(state2.statistics.totalLaunches).isEqualTo(50)

            // 第三次发射
            mutableFlow.emit(UsageStatistics(totalProfiles = 3, totalLaunches = 100, totalFrozenApps = 5))
            testDispatcher.scheduler.advanceUntilIdle()

            val state3 = awaitItem()
            assertThat(state3.statistics.totalProfiles).isEqualTo(3)
            assertThat(state3.statistics.totalLaunches).isEqualTo(100)
            assertThat(state3.statistics.totalFrozenApps).isEqualTo(5)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `DashboardUiState 默认值验证`() {
        val state = DashboardUiState()

        assertThat(state.statistics).isEqualTo(UsageStatistics())
        assertThat(state.isLoading).isTrue()
    }

    @Test
    fun `DashboardUiState copy 保持不变值`() {
        val state = DashboardUiState(
            statistics = UsageStatistics(totalProfiles = 5),
            isLoading = false
        )
        val copied = state.copy(isLoading = true)

        assertThat(copied.statistics.totalProfiles).isEqualTo(5)
        assertThat(copied.isLoading).isTrue()
    }
}
