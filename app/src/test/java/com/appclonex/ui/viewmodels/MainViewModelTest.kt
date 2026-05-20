package com.appclonex.ui.viewmodels

import android.app.Application
import app.cash.turbine.test
import com.appclonex.infrastructure.external.shizuku.ShizukuServiceConnectionProvider
import com.appclonex.domain.model.ShizukuConnectionState
import com.appclonex.domain.repository.UserProfileRepository
import com.appclonex.infrastructure.persistence.datastore.ApplicationPreferencesDataStoreManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
 * MainViewModel 集成测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class MainViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var application: Application
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var preferencesDataStoreManager: ApplicationPreferencesDataStoreManager
    private lateinit var shizukuServiceConnectionProvider: ShizukuServiceConnectionProvider

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        application = mockk(relaxed = true)
        userProfileRepository = mockk(relaxed = true)
        preferencesDataStoreManager = mockk(relaxed = true)
        shizukuServiceConnectionProvider = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 创建 MainViewModel 并 mock ShizukuServiceConnectionProvider
     */
    private fun createViewModel(
        onboardingCompleted: Boolean = false,
        initialShizukuState: ShizukuConnectionState = ShizukuConnectionState.NOT_INSTALLED
    ): MainViewModel {
        // Mock ShizukuServiceConnectionProvider 返回一个可控的 Flow
        every { shizukuServiceConnectionProvider.getConnectionStateFlow() } returns flowOf(initialShizukuState)
        every { shizukuServiceConnectionProvider.getCurrentConnectionState() } returns initialShizukuState

        // Mock ApplicationPreferencesDataStoreManager.isOnboardingCompleted
        coEvery { preferencesDataStoreManager.isOnboardingCompleted.first() } returns onboardingCompleted
        every { preferencesDataStoreManager.isOnboardingCompleted } returns flowOf(onboardingCompleted)

        return MainViewModel(application, userProfileRepository, preferencesDataStoreManager, shizukuServiceConnectionProvider)
    }

    @Test
    fun `初始状态 shizukuState 默认值为 NOT_INSTALLED`() = runTest {
        val viewModel = createViewModel(
            initialShizukuState = ShizukuConnectionState.NOT_INSTALLED
        )

        viewModel.uiState.test {
            val initialState = awaitItem()
            assert(initialState.shizukuState == ShizukuConnectionState.NOT_INSTALLED) {
                "期望初始 shizukuState 为 NOT_INSTALLED，实际为 ${initialState.shizukuState}"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `初始状态 showOnboarding 根据引导完成状态决定`() = runTest {
        // 未完成引导 -> showOnboarding = true
        val viewModelNotCompleted = createViewModel(onboardingCompleted = false)
        viewModelNotCompleted.uiState.test {
            val state = awaitItem()
            assert(state.showOnboarding == true) {
                "期望未完成引导时 showOnboarding 为 true"
            }
            cancelAndIgnoreRemainingEvents()
        }

        // 已完成引导 -> showOnboarding = false
        val viewModelCompleted = createViewModel(onboardingCompleted = true)
        viewModelCompleted.uiState.test {
            val state = awaitItem()
            assert(state.showOnboarding == false) {
                "期望已完成引导时 showOnboarding 为 false"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `初始状态 message 为 null`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initialState = awaitItem()
            assert(initialState.message == null) {
                "期望初始 message 为 null，实际为 ${initialState.message}"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `checkShizukuState 更新 shizukuState`() = runTest {
        val newState = ShizukuConnectionState.READY
        val viewModel = createViewModel()

        // 更新 mock 返回新状态
        every { shizukuServiceConnectionProvider.getCurrentConnectionState() } returns newState

        viewModel.checkShizukuState()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            // 跳过可能存在的旧状态
            val states = mutableListOf<MainUiState>()
            while (states.isEmpty() || states.last().shizukuState != newState) {
                states.add(awaitItem())
            }
            val finalState = states.last()
            assert(finalState.shizukuState == ShizukuConnectionState.READY) {
                "期望 checkShizukuState 后状态为 READY，实际为 ${finalState.shizukuState}"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `checkShizukuState 更新为 PERMISSION_DENIED`() = runTest {
        val viewModel = createViewModel()
        every { shizukuServiceConnectionProvider.getCurrentConnectionState() } returns ShizukuConnectionState.PERMISSION_DENIED

        viewModel.checkShizukuState()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val states = mutableListOf<MainUiState>()
            while (states.isEmpty() || states.last().shizukuState != ShizukuConnectionState.PERMISSION_DENIED) {
                states.add(awaitItem())
            }
            assert(states.last().shizukuState == ShizukuConnectionState.PERMISSION_DENIED) {
                "期望状态为 PERMISSION_DENIED"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `completeOnboarding 将 showOnboarding 设为 false`() = runTest {
        // 初始未完成引导
        val viewModel = createViewModel(onboardingCompleted = false)

        coEvery { preferencesDataStoreManager.setOnboardingCompleted() } just runs

        viewModel.completeOnboarding()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val states = mutableListOf<MainUiState>()
            while (states.isEmpty() || states.last().showOnboarding == true) {
                states.add(awaitItem())
            }
            val finalState = states.last()
            assert(finalState.showOnboarding == false) {
                "期望 completeOnboarding 后 showOnboarding 为 false"
            }
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { preferencesDataStoreManager.setOnboardingCompleted() }
    }

    @Test
    fun `completeOnboarding 已完成引导时仍设为 false`() = runTest {
        val viewModel = createViewModel(onboardingCompleted = true)
        coEvery { preferencesDataStoreManager.setOnboardingCompleted() } just runs

        viewModel.completeOnboarding()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val states = mutableListOf<MainUiState>()
            while (states.isEmpty() || states.last().showOnboarding != false) {
                states.add(awaitItem())
            }
            assert(states.last().showOnboarding == false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `showMessage 更新 uiState 中的 message`() = runTest {
        val viewModel = createViewModel()
        val testMessage = "测试消息"

        viewModel.showMessage(testMessage)

        viewModel.uiState.test {
            val states = mutableListOf<MainUiState>()
            while (states.isEmpty() || states.last().message != testMessage) {
                states.add(awaitItem())
            }
            assert(states.last().message == testMessage) {
                "期望 message 为 '$testMessage'，实际为 '${states.last().message}'"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearMessage 将 message 设为 null`() = runTest {
        val viewModel = createViewModel()

        // 先设置消息
        viewModel.showMessage("临时消息")

        // 清除消息
        viewModel.clearMessage()

        viewModel.uiState.test {
            val states = mutableListOf<MainUiState>()
            while (states.isEmpty() || states.last().message != null) {
                states.add(awaitItem())
            }
            assert(states.last().message == null) {
                "期望 clearMessage 后 message 为 null"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `showMessage 后 clearMessage 完整流程`() = runTest {
        val viewModel = createViewModel()

        viewModel.showMessage("Hello")
        viewModel.clearMessage()

        viewModel.uiState.test {
            val states = mutableListOf<MainUiState>()
            var foundMessage = false
            var foundNull = false

            repeat(10) {
                val state = awaitItem()
                if (state.message == "Hello") foundMessage = true
                if (state.message == null && foundMessage) foundNull = true
            }

            assert(foundMessage) { "应该先看到 message = 'Hello'" }
            assert(foundNull) { "应该在 clearMessage 后看到 message = null" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init 时调用 shizukuServiceConnectionProvider init`() = runTest {
        createViewModel()

        verify { shizukuServiceConnectionProvider.init() }
    }

    @Test
    fun `onCleared 时调用 shizukuServiceConnectionProvider destroy`() = runTest {
        val viewModel = createViewModel()

        viewModel.onCleared()

        verify { shizukuServiceConnectionProvider.destroy() }
    }
}
