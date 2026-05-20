package com.appclonex.data.repository

import android.content.Context
import app.cash.turbine.test
import com.appclonex.core.usermanager.MultiUserManager
import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.domain.model.UserProfileModel
import com.appclonex.domain.model.UserProfileInfo
import com.appclonex.domain.model.UserProfileState
import com.appclonex.infrastructure.persistence.database.ApplicationDatabaseConfiguration
import com.appclonex.infrastructure.persistence.database.ClonedApplicationDataAccessObject
import com.appclonex.infrastructure.persistence.database.UserProfileDataAccessObject
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
 * UserProfileRepository 集成测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class UserProfileRepositoryTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var context: Context
    private lateinit var database: ApplicationDatabaseConfiguration
    private lateinit var userProfileDao: UserProfileDataAccessObject
    private lateinit var clonedAppDao: ClonedApplicationDataAccessObject
    private lateinit var multiUserManager: MultiUserManager

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true)
        database = mockk(relaxed = true)
        userProfileDao = mockk(relaxed = true)
        clonedAppDao = mockk(relaxed = true)
        multiUserManager = mockk(relaxed = true)

        every { database.userProfileDao() } returns userProfileDao
        every { database.clonedAppDao() } returns clonedAppDao
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createRepository(): UserProfileRepository {
        return UserProfileRepository(context, database, userProfileDao, multiUserManager)
    }

    // ==================== createProfile 测试 ====================

    @Test
    fun `createProfile 成功路径 - 系统API成功并保存到本地数据库`() = runTest {
        val repository = createRepository()
        val profileName = "测试用户"
        val userId = 99

        // Mock 系统API创建用户成功
        coEvery { multiUserManager.createUserProfile(profileName) } returns Result.success(userId)

        // Mock 本地数据库插入
        coEvery { userProfileDao.insertProfile(any()) } just runs

        val result = repository.createProfile(profileName)

        assert(result.isSuccess) { "期望 createProfile 成功" }
        assert(result.getOrNull() == userId) {
            "期望返回 userId=$userId，实际为 ${result.getOrNull()}"
        }

        // 验证调用了系统API
        coVerify { multiUserManager.createUserProfile(profileName) }

        // 验证保存到本地数据库，检查传入的 profile 参数
        coVerify {
            userProfileDao.insertProfile(match { profile ->
                profile.userId == userId &&
                profile.profileName == profileName
            })
        }
    }

    @Test
    fun `createProfile 系统API返回无效userId时失败`() = runTest {
        val repository = createRepository()
        val profileName = "测试用户"

        // Mock 系统API返回无效 userId (0)
        coEvery { multiUserManager.createUserProfile(profileName) } returns Result.success(0)

        val result = repository.createProfile(profileName)

        assert(result.isFailure) { "期望 userId=0 时 createProfile 失败" }
        assert(result.exceptionOrNull()?.message?.contains("无效的用户ID") == true) {
            "期望错误消息包含 '无效的用户ID'"
        }

        // 验证不会插入本地数据库
        coVerify(exactly = 0) { userProfileDao.insertProfile(any()) }
    }

    @Test
    fun `createProfile 系统API返回负数userId时失败`() = runTest {
        val repository = createRepository()
        val profileName = "测试用户"

        coEvery { multiUserManager.createUserProfile(profileName) } returns Result.success(-1)

        val result = repository.createProfile(profileName)

        assert(result.isFailure) { "期望 userId=-1 时 createProfile 失败" }
        coVerify(exactly = 0) { userProfileDao.insertProfile(any()) }
    }

    @Test
    fun `createProfile 系统API失败时不保存本地数据`() = runTest {
        val repository = createRepository()
        val profileName = "测试用户"

        coEvery { multiUserManager.createUserProfile(profileName) } returns
            Result.failure(Exception("系统API调用失败"))

        val result = repository.createProfile(profileName)

        assert(result.isFailure) { "期望系统API失败时 createProfile 失败" }
        coVerify(exactly = 0) { userProfileDao.insertProfile(any()) }
    }

    // ==================== deleteProfile 测试 ====================

    @Test
    fun `deleteProfile 系统API失败时不删本地数据`() = runTest {
        val repository = createRepository()
        val userId = 99

        // Mock 系统API删除失败
        coEvery { multiUserManager.deleteUserProfile(userId) } returns
            Result.failure(Exception("系统用户删除失败"))

        val result = repository.deleteProfile(userId)

        assert(result.isFailure) { "期望系统API失败时 deleteProfile 失败" }

        // 验证没有删除本地数据
        coVerify(exactly = 0) { clonedAppDao.deleteAppsByProfile(userId) }
        coVerify(exactly = 0) { userProfileDao.deleteProfileById(userId) }
    }

    @Test
    fun `deleteProfile 成功路径 - 先删系统用户再删本地数据`() = runTest {
        val repository = createRepository()
        val userId = 99

        // Mock 系统API删除成功
        coEvery { multiUserManager.deleteUserProfile(userId) } returns Result.success(true)

        // Mock 本地数据删除
        coEvery { clonedAppDao.deleteAppsByProfile(userId) } just runs
        coEvery { userProfileDao.deleteProfileById(userId) } just runs

        val result = repository.deleteProfile(userId)

        assert(result.isSuccess) { "期望 deleteProfile 成功" }
        assert(result.getOrNull() == true) { "期望返回 true" }

        // 验证调用顺序：先系统API，再删本地数据
        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            multiUserManager.deleteUserProfile(userId)
            clonedAppDao.deleteAppsByProfile(userId)
            userProfileDao.deleteProfileById(userId)
        }
    }

    @Test
    fun `deleteProfile 系统成功但本地删除克隆应用失败时返回失败`() = runTest {
        val repository = createRepository()
        val userId = 99

        coEvery { multiUserManager.deleteUserProfile(userId) } returns Result.success(true)
        coEvery { clonedAppDao.deleteAppsByProfile(userId) } throws Exception("数据库错误")

        val result = repository.deleteProfile(userId)

        assert(result.isFailure) { "期望本地删除失败时返回失败" }
        assert(result.exceptionOrNull()?.message?.contains("本地数据清理失败") == true) {
            "期望错误消息包含 '本地数据清理失败'"
        }
    }

    // ==================== getAllProfilesWithInfo 测试 ====================

    @Test
    fun `getAllProfilesWithInfo 返回正确数据 - 运行中用户`() = runTest {
        val repository = createRepository()

        val profiles = listOf(
            UserProfileModel(
                userId = 10,
                profileName = "用户A",
                isRunning = true,
                createdAt = 1000L,
                updatedAt = 2000L
            ),
            UserProfileModel(
                userId = 11,
                profileName = "用户B",
                isRunning = false,
                createdAt = 3000L,
                updatedAt = 4000L
            )
        )

        coEvery { userProfileDao.getAllProfiles() } returns flowOf(profiles)

        val result = repository.getAllProfilesWithInfo()

        assert(result.size == 2) { "期望返回 2 个 profile" }

        // 验证运行中的用户状态为 RUNNING
        assert(result[0].state == UserProfileState.RUNNING) {
            "期望 isRunning=true 的用户状态为 RUNNING，实际为 ${result[0].state}"
        }
        assert(result[0].profile.profileName == "用户A")

        // 验证未运行的用户状态为 STOPPED
        assert(result[1].state == UserProfileState.STOPPED) {
            "期望 isRunning=false 的用户状态为 STOPPED，实际为 ${result[1].state}"
        }
        assert(result[1].profile.profileName == "用户B")
    }

    @Test
    fun `getAllProfilesWithInfo 返回空列表 当无用户时`() = runTest {
        val repository = createRepository()

        coEvery { userProfileDao.getAllProfiles() } returns flowOf(emptyList())

        val result = repository.getAllProfilesWithInfo()

        assert(result.isEmpty()) { "期望无用户时返回空列表" }
    }

    @Test
    fun `getAllProfilesWithInfo 所有用户都运行时全部为 RUNNING`() = runTest {
        val repository = createRepository()

        val profiles = listOf(
            UserProfileModel(userId = 10, profileName = "A", isRunning = true),
            UserProfileModel(userId = 11, profileName = "B", isRunning = true),
            UserProfileModel(userId = 12, profileName = "C", isRunning = true)
        )

        coEvery { userProfileDao.getAllProfiles() } returns flowOf(profiles)

        val result = repository.getAllProfilesWithInfo()

        assert(result.all { it.state == UserProfileState.RUNNING }) {
            "期望所有运行中用户状态为 RUNNING"
        }
    }

    @Test
    fun `getAllProfilesWithInfo 所有用户都停止时全部为 STOPPED`() = runTest {
        val repository = createRepository()

        val profiles = listOf(
            UserProfileModel(userId = 10, profileName = "A", isRunning = false),
            UserProfileModel(userId = 11, profileName = "B", isRunning = false)
        )

        coEvery { userProfileDao.getAllProfiles() } returns flowOf(profiles)

        val result = repository.getAllProfilesWithInfo()

        assert(result.all { it.state == UserProfileState.STOPPED }) {
            "期望所有未运行用户状态为 STOPPED"
        }
    }

    // ==================== getProfileById 测试 ====================

    @Test
    fun `getProfileById 返回正确用户`() = runTest {
        val repository = createRepository()
        val profile = UserProfileModel(userId = 10, profileName = "测试用户")

        coEvery { userProfileDao.getProfileById(10) } returns profile

        val result = repository.getProfileById(10)

        assert(result != null) { "期望找到用户" }
        assert(result!!.userId == 10) { "期望 userId 为 10" }
        assert(result.profileName == "测试用户") { "期望名称为 '测试用户'" }
    }

    @Test
    fun `getProfileById 返回 null 当用户不存在时`() = runTest {
        val repository = createRepository()

        coEvery { userProfileDao.getProfileById(999) } returns null

        val result = repository.getProfileById(999)

        assert(result == null) { "期望用户不存在时返回 null" }
    }

    // ==================== updateProfileName 测试 ====================

    @Test
    fun `updateProfileName 更新用户名称`() = runTest {
        val repository = createRepository()
        val oldProfile = UserProfileModel(userId = 10, profileName = "旧名称", updatedAt = 1000L)

        coEvery { userProfileDao.getProfileById(10) } returns oldProfile
        coEvery { userProfileDao.updateProfile(any()) } just runs

        repository.updateProfileName(10, "新名称")

        coVerify {
            userProfileDao.updateProfile(match { profile ->
                profile.profileName == "新名称" && profile.userId == 10
            })
        }
    }

    @Test
    fun `updateProfileName 用户不存在时不执行更新`() = runTest {
        val repository = createRepository()

        coEvery { userProfileDao.getProfileById(999) } returns null

        repository.updateProfileName(999, "新名称")

        coVerify(exactly = 0) { userProfileDao.updateProfile(any()) }
    }

    // ==================== getProfileCount 测试 ====================

    @Test
    fun `getProfileCount 返回正确数量`() = runTest {
        val repository = createRepository()

        coEvery { userProfileDao.getProfileCount() } returns 7

        val count = repository.getProfileCount()

        assert(count == 7) { "期望 profileCount 为 7，实际为 $count" }
    }

    // ==================== getAllProfilesFlow 测试 ====================

    @Test
    fun `getAllProfilesFlow 返回正确的 Flow`() = runTest {
        val repository = createRepository()
        val profiles = listOf(
            UserProfileModel(userId = 10, profileName = "A"),
            UserProfileModel(userId = 11, profileName = "B")
        )

        every { userProfileDao.getAllProfiles() } returns flowOf(profiles)

        repository.getAllProfilesFlow().test {
            val result = awaitItem()
            assert(result.size == 2) { "期望 Flow 发射 2 个 profile" }
            awaitComplete()
        }
    }
}
