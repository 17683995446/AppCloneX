package com.appclonex.domain.usecase

import com.appclonex.domain.model.ClonedApplicationInformationModel
import com.appclonex.domain.model.InstallableApp
import com.appclonex.domain.repository.ClonedApplicationRepository
import com.appclonex.domain.repository.UserProfileRepository
import com.appclonex.domain.usecase.clone.CloneApplicationUseCase
import com.appclonex.domain.usecase.clone.DeleteClonedApplicationUseCase
import com.appclonex.domain.usecase.clone.GetInstallableAppsUseCase
import com.appclonex.domain.usecase.clone.LaunchClonedApplicationUseCase
import com.appclonex.domain.usecase.profile.CreateUserProfileUseCase
import com.appclonex.domain.usecase.profile.DeleteUserProfileUseCase
import com.appclonex.domain.usecase.profile.SwitchUserProfileUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * UseCase 集成测试
 *
 * 验证所有 UseCase 的业务逻辑正确性
 */
class UseCaseIntegrationTest {

    // === CloneApplicationUseCase ===

    @Test
    fun `CloneApplicationUseCase - valid params should call repository`() = runTest {
        val repo = mockk<ClonedApplicationRepository>()
        val app = ClonedApplicationInformationModel(
            packageName = "com.test",
            appName = "Test",
            profileUserId = 10
        )
        coEvery { repo.cloneApp("com.test", 10) } returns Result.success(app)

        val useCase = CloneApplicationUseCase(repo)
        val result = useCase("com.test", 10)

        assertTrue(result.isSuccess)
        assertEquals("com.test", result.getOrNull()?.packageName)
        coVerify { repo.cloneApp("com.test", 10) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `CloneApplicationUseCase - blank packageName should throw`() = runTest {
        val repo = mockk<ClonedApplicationRepository>()
        val useCase = CloneApplicationUseCase(repo)
        useCase("  ", 10)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `CloneApplicationUseCase - invalid userId should throw`() = runTest {
        val repo = mockk<ClonedApplicationRepository>()
        val useCase = CloneApplicationUseCase(repo)
        useCase("com.test", 0)
    }

    @Test
    fun `CloneApplicationUseCase - repository failure should propagate`() = runTest {
        val repo = mockk<ClonedApplicationRepository>()
        coEvery { repo.cloneApp("com.test", 10) } returns Result.failure(Exception("fail"))

        val useCase = CloneApplicationUseCase(repo)
        val result = useCase("com.test", 10)

        assertTrue(result.isFailure)
    }

    // === DeleteClonedApplicationUseCase ===

    @Test
    fun `DeleteClonedApplicationUseCase - valid id should call repository`() = runTest {
        val repo = mockk<ClonedApplicationRepository>()
        coEvery { repo.deleteClonedApp(any()) } returns Result.success(true)

        val app = ClonedApplicationInformationModel(
            id = 1,
            packageName = "com.test",
            appName = "Test",
            profileUserId = 10
        )
        val useCase = DeleteClonedApplicationUseCase(repo)
        val result = useCase(app)

        assertTrue(result.isSuccess)
        coVerify { repo.deleteClonedApp(app) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `DeleteClonedApplicationUseCase - zero id should throw`() = runTest {
        val repo = mockk<ClonedApplicationRepository>()
        val app = ClonedApplicationInformationModel(
            id = 0,
            packageName = "com.test",
            appName = "Test",
            profileUserId = 10
        )
        val useCase = DeleteClonedApplicationUseCase(repo)
        useCase(app)
    }

    // === CreateUserProfileUseCase ===

    @Test
    fun `CreateUserProfileUseCase - valid name should call repository`() = runTest {
        val repo = mockk<UserProfileRepository>()
        coEvery { repo.createProfile("Test") } returns Result.success(10)

        val useCase = CreateUserProfileUseCase(repo)
        val result = useCase("Test")

        assertTrue(result.isSuccess)
        assertEquals(10, result.getOrNull())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `CreateUserProfileUseCase - blank name should throw`() = runTest {
        val repo = mockk<UserProfileRepository>()
        val useCase = CreateUserProfileUseCase(repo)
        useCase("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `CreateUserProfileUseCase - name too long should throw`() = runTest {
        val repo = mockk<UserProfileRepository>()
        val useCase = CreateUserProfileUseCase(repo)
        useCase("a".repeat(21))
    }

    @Test
    fun `CreateUserProfileUseCase - name should be trimmed`() = runTest {
        val repo = mockk<UserProfileRepository>()
        coEvery { repo.createProfile("Test") } returns Result.success(10)

        val useCase = CreateUserProfileUseCase(repo)
        useCase("  Test  ")

        coVerify { repo.createProfile("Test") }
    }

    // === DeleteUserProfileUseCase ===

    @Test
    fun `DeleteUserProfileUseCase - valid userId should call repository`() = runTest {
        val repo = mockk<UserProfileRepository>()
        coEvery { repo.deleteProfile(10) } returns Result.success(true)

        val useCase = DeleteUserProfileUseCase(repo)
        val result = useCase(10)

        assertTrue(result.isSuccess)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `DeleteUserProfileUseCase - invalid userId should throw`() = runTest {
        val repo = mockk<UserProfileRepository>()
        val useCase = DeleteUserProfileUseCase(repo)
        useCase(-1)
    }

    // === GetInstallableAppsUseCase ===

    @Test
    fun `GetInstallableAppsUseCase - should delegate to repository`() = runTest {
        val repo = mockk<ClonedApplicationRepository>()
        val apps = listOf(
            InstallableApp("com.app1", "App1", null, "1.0", 1),
            InstallableApp("com.app2", "App2", null, "2.0", 2)
        )
        coEvery { repo.getInstallableApps(false) } returns Result.success(apps)

        val useCase = GetInstallableAppsUseCase(repo)
        val result = useCase(false)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
    }

    // === SwitchUserProfileUseCase ===

    @Test
    fun `SwitchUserProfileUseCase - valid userId should call repository`() = runTest {
        val repo = mockk<UserProfileRepository>()
        coEvery { repo.switchToProfile(10) } returns Result.success(true)

        val useCase = SwitchUserProfileUseCase(repo)
        val result = useCase(10)

        assertTrue(result.isSuccess)
        coVerify { repo.switchToProfile(10) }
    }

    // === LaunchClonedApplicationUseCase ===

    @Test
    fun `LaunchClonedApplicationUseCase - should call repository`() = runTest {
        val repo = mockk<ClonedApplicationRepository>()
        coEvery { repo.launchClonedApp(any()) } returns Result.success(true)

        val app = ClonedApplicationInformationModel(
            id = 1,
            packageName = "com.test",
            appName = "Test",
            profileUserId = 10
        )
        val useCase = LaunchClonedApplicationUseCase(repo)
        val result = useCase(app)

        assertTrue(result.isSuccess)
        coVerify { repo.launchClonedApp(app) }
    }
}
