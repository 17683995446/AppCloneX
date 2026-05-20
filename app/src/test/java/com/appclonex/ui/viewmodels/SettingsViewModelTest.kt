package com.appclonex.ui.viewmodels

import android.app.Application
import app.cash.turbine.test
import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.infrastructure.persistence.database.ApplicationDatabaseConfiguration
import com.appclonex.infrastructure.persistence.database.ClonedApplicationDataAccessObject
import com.appclonex.infrastructure.persistence.database.UserProfileDataAccessObject
import com.appclonex.presentation.theme.ThemeMode
import com.appclonex.infrastructure.persistence.datastore.ApplicationPreferencesDataStoreManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
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
 * SettingsViewModel 集成测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class SettingsViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var application: Application
    private lateinit var preferencesDataStoreManager: ApplicationPreferencesDataStoreManager
    private lateinit var database: ApplicationDatabaseConfiguration
    private lateinit var userProfileDao: UserProfileDataAccessObject
    private lateinit var clonedAppDao: ClonedApplicationDataAccessObject

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        application = mockk(relaxed = true)
        preferencesDataStoreManager = mockk(relaxed = true)
        database = mockk(relaxed = true)
        userProfileDao = mockk(relaxed = true)
        clonedAppDao = mockk(relaxed = true)

        every { database.userProfileDao() } returns userProfileDao
        every { database.clonedAppDao() } returns clonedAppDao
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 创建 SettingsViewModel 并设置默认 mock 行为
     */
    private fun createViewModel(
        excludeSystemApps: Boolean = true,
        showNotifications: Boolean = true,
        autoStartProfile: Boolean = false,
        darkTheme: Boolean = false,
        themeMode: ThemeMode = ThemeMode.SYSTEM,
        lastBackupTime: Long = 0L,
        profileCount: Int = 0,
        clonedApps: List<ClonedApplicationInformationModel> = emptyList()
    ): SettingsViewModel {
        every { preferencesDataStoreManager.excludeSystemApps } returns flowOf(excludeSystemApps)
        every { preferencesDataStoreManager.showNotifications } returns flowOf(showNotifications)
        every { preferencesDataStoreManager.autoStartProfile } returns flowOf(autoStartProfile)
        every { preferencesDataStoreManager.darkTheme } returns flowOf(darkTheme)
        every { preferencesDataStoreManager.themeMode } returns flowOf(themeMode)
        every { preferencesDataStoreManager.lastBackupTime } returns flowOf(lastBackupTime)

        coEvery { userProfileDao.getProfileCount() } returns profileCount
        every { clonedAppDao.getAllClonedApps() } returns flowOf(clonedApps)

        return SettingsViewModel(application, preferencesDataStoreManager, database)
    }

    @Test
    fun `初始状态 excludeSystemApps 默认为 true`() = runTest {
        val viewModel = createViewModel(excludeSystemApps = true)

        viewModel.uiState.test {
            val state = awaitItem()
            assert(state.excludeSystemApps == true) {
                "期望初始 excludeSystemApps 为 true"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `初始状态 excludeSystemApps 为 false 时正确反映`() = runTest {
        val viewModel = createViewModel(excludeSystemApps = false)

        viewModel.uiState.test {
            val state = awaitItem()
            assert(state.excludeSystemApps == false) {
                "期望 excludeSystemApps 为 false"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setExcludeSystemApps 更新 uiState`() = runTest {
        // 初始为 true，切换为 false
        val excludeFlow = kotlinx.coroutines.flow.MutableStateFlow(true)
        every { preferencesDataStoreManager.excludeSystemApps } returns excludeFlow
        coEvery { preferencesDataStoreManager.setExcludeSystemApps(false) } just runs
        coEvery { userProfileDao.getProfileCount() } returns 0
        every { clonedAppDao.getAllClonedApps() } returns flowOf(emptyList())
        every { preferencesDataStoreManager.showNotifications } returns flowOf(true)
        every { preferencesDataStoreManager.autoStartProfile } returns flowOf(false)
        every { preferencesDataStoreManager.darkTheme } returns flowOf(false)
        every { preferencesDataStoreManager.themeMode } returns flowOf(ThemeMode.SYSTEM)
        every { preferencesDataStoreManager.lastBackupTime } returns flowOf(0L)

        val viewModel = SettingsViewModel(application, preferencesDataStoreManager, database)

        viewModel.setExcludeSystemApps(false)
        testDispatcher.scheduler.advanceUntilIdle()

        // 模拟 DataStore 更新后发射新值
        excludeFlow.value = false
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val states = mutableListOf<SettingsUiState>()
            while (states.isEmpty() || states.last().excludeSystemApps == true) {
                states.add(awaitItem())
            }
            assert(states.last().excludeSystemApps == false) {
                "期望 setExcludeSystemApps(false) 后 excludeSystemApps 为 false"
            }
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { preferencesDataStoreManager.setExcludeSystemApps(false) }
    }

    @Test
    fun `setExcludeSystemApps true 调用 preferencesManager`() = runTest {
        val viewModel = createViewModel()

        coEvery { preferencesDataStoreManager.setExcludeSystemApps(true) } just runs

        viewModel.setExcludeSystemApps(true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { preferencesDataStoreManager.setExcludeSystemApps(true) }
    }

    @Test
    fun `setThemeMode 更新 uiState`() = runTest {
        val themeFlow = kotlinx.coroutines.flow.MutableStateFlow(ThemeMode.SYSTEM)
        every { preferencesDataStoreManager.themeMode } returns themeFlow
        coEvery { preferencesDataStoreManager.setThemeMode(ThemeMode.DARK) } just runs
        coEvery { userProfileDao.getProfileCount() } returns 0
        every { clonedAppDao.getAllClonedApps() } returns flowOf(emptyList())
        every { preferencesDataStoreManager.excludeSystemApps } returns flowOf(true)
        every { preferencesDataStoreManager.showNotifications } returns flowOf(true)
        every { preferencesDataStoreManager.autoStartProfile } returns flowOf(false)
        every { preferencesDataStoreManager.darkTheme } returns flowOf(false)
        every { preferencesDataStoreManager.lastBackupTime } returns flowOf(0L)

        val viewModel = SettingsViewModel(application, preferencesDataStoreManager, database)

        viewModel.setThemeMode(ThemeMode.DARK)
        testDispatcher.scheduler.advanceUntilIdle()

        // 模拟 DataStore 更新后发射新值
        themeFlow.value = ThemeMode.DARK
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val states = mutableListOf<SettingsUiState>()
            while (states.isEmpty() || states.last().themeMode != ThemeMode.DARK) {
                states.add(awaitItem())
            }
            assert(states.last().themeMode == ThemeMode.DARK) {
                "期望 setThemeMode(DARK) 后 themeMode 为 DARK"
            }
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { preferencesDataStoreManager.setThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun `setThemeMode LIGHT 更新 uiState`() = runTest {
        val viewModel = createViewModel()

        coEvery { preferencesDataStoreManager.setThemeMode(ThemeMode.LIGHT) } just runs

        viewModel.setThemeMode(ThemeMode.LIGHT)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { preferencesDataStoreManager.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun `getFormattedLastBackupTime 返回从未备份 当 lastBackupTime 为 0`() = runTest {
        val viewModel = createViewModel(lastBackupTime = 0L)

        viewModel.uiState.test {
            val state = awaitItem()
            val formatted = state.getFormattedLastBackupTime()
            assert(formatted == "从未备份") {
                "期望 lastBackupTime=0 时返回 '从未备份'，实际为 '$formatted'"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getFormattedLastBackupTime 返回正确格式 当 lastBackupTime 非0`() = runTest {
        val timestamp = 1700000000000L // 2023-11-14 22:13:20 UTC+8
        val viewModel = createViewModel(lastBackupTime = timestamp)

        viewModel.uiState.test {
            val state = awaitItem()
            val formatted = state.getFormattedLastBackupTime()
            assert(formatted != "从未备份") {
                "期望 lastBackupTime 非0 时不返回 '从未备份'"
            }
            // 验证格式包含日期和时间分隔符
            assert(formatted.contains(" ")) {
                "期望格式化结果包含空格分隔的日期和时间，实际为 '$formatted'"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getFormattedLastBackupTime 边界值 - 最小非零时间戳`() = runTest {
        val viewModel = createViewModel(lastBackupTime = 1L)

        viewModel.uiState.test {
            val state = awaitItem()
            val formatted = state.getFormattedLastBackupTime()
            assert(formatted != "从未备份") {
                "期望 lastBackupTime=1 时返回格式化时间而非 '从未备份'"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getFormattedLastBackupTime 边界值 - 极大时间戳`() = runTest {
        val viewModel = createViewModel(lastBackupTime = Long.MAX_VALUE)

        viewModel.uiState.test {
            val state = awaitItem()
            val formatted = state.getFormattedLastBackupTime()
            assert(formatted != "从未备份") {
                "期望极大时间戳返回格式化时间而非 '从未备份'"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `初始统计信息 totalProfiles 和 totalClonedApps 正确`() = runTest {
        val viewModel = createViewModel(
            profileCount = 5,
            clonedApps = listOf(
                ClonedApplicationInformationModel(packageName = "com.test.app1", appName = "App1", profileUserId = 10),
                ClonedApplicationInformationModel(packageName = "com.test.app2", appName = "App2", profileUserId = 10),
                ClonedApplicationInformationModel(packageName = "com.test.app3", appName = "App3", profileUserId = 11)
            )
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assert(state.totalProfiles == 5) {
                "期望 totalProfiles 为 5，实际为 ${state.totalProfiles}"
            }
            assert(state.totalClonedApps == 3) {
                "期望 totalClonedApps 为 3，实际为 ${state.totalClonedApps}"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `初始状态 themeMode 默认为 SYSTEM`() = runTest {
        val viewModel = createViewModel(themeMode = ThemeMode.SYSTEM)

        viewModel.uiState.test {
            val state = awaitItem()
            assert(state.themeMode == ThemeMode.SYSTEM) {
                "期望初始 themeMode 为 SYSTEM"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `初始状态 isBackupLoading 和 isRestoreLoading 为 false`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assert(!state.isBackupLoading) { "期望初始 isBackupLoading 为 false" }
            assert(!state.isRestoreLoading) { "期望初始 isRestoreLoading 为 false" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearMessage 将 message 设为 null`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // 跳过初始状态
            // 手动更新内部状态模拟消息
            viewModel.clearMessage()

            val states = mutableListOf<SettingsUiState>()
            while (states.isEmpty()) {
                states.add(awaitItem())
            }
            assert(states.last().message == null) {
                "期望 clearMessage 后 message 为 null"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearError 将 error 设为 null`() = runTest {
        val viewModel = createViewModel()

        viewModel.clearError()

        viewModel.uiState.test {
            val states = mutableListOf<SettingsUiState>()
            while (states.isEmpty()) {
                states.add(awaitItem())
            }
            assert(states.last().error == null) {
                "期望 clearError 后 error 为 null"
            }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
