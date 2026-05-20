package com.appclonex.domain.usecase

import com.appclonex.domain.model.*
import com.appclonex.domain.repository.ClonedApplicationRepository
import com.appclonex.domain.repository.UserProfileRepository
import com.appclonex.domain.usecase.clone.CloneApplicationUseCase
import com.appclonex.domain.usecase.clone.DeleteClonedApplicationUseCase
import com.appclonex.domain.usecase.clone.GetInstallableAppsUseCase
import com.appclonex.domain.usecase.clone.LaunchClonedApplicationUseCase
import com.appclonex.domain.usecase.profile.CreateUserProfileUseCase
import com.appclonex.domain.usecase.profile.DeleteUserProfileUseCase
import com.appclonex.domain.usecase.profile.SwitchUserProfileUseCase
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * V10 改进验证测试
 * 
 * 测试覆盖：
 * 1. Repository 接口 - 验证接口存在且定义正确
 * 2. UseCase 类 - 验证 UseCase 可以实例化且有正确的签名
 * 3. BaseViewModel - 验证基类功能
 * 4. Error Handling - 验证 OperationError 类型正确工作
 * 5. Documentation - 通过验证类存在间接验证
 */
class UseCaseVerificationTest {

    // ==================== Mock 对象 ====================
    
    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var clonedAppRepository: ClonedApplicationRepository
    
    // ==================== UseCase 实例 ====================
    
    private lateinit var createUserProfileUseCase: CreateUserProfileUseCase
    private lateinit var deleteUserProfileUseCase: DeleteUserProfileUseCase
    private lateinit var switchUserProfileUseCase: SwitchUserProfileUseCase
    private lateinit var cloneApplicationUseCase: CloneApplicationUseCase
    private lateinit var deleteClonedApplicationUseCase: DeleteClonedApplicationUseCase
    private lateinit var getInstallableAppsUseCase: GetInstallableAppsUseCase
    private lateinit var launchClonedApplicationUseCase: LaunchClonedApplicationUseCase

    @Before
    fun setup() {
        // 初始化 Mock 对象
        userProfileRepository = mockk()
        clonedAppRepository = mockk()
        
        // 初始化 UseCase 实例
        createUserProfileUseCase = CreateUserProfileUseCase(userProfileRepository)
        deleteUserProfileUseCase = DeleteUserProfileUseCase(userProfileRepository)
        switchUserProfileUseCase = SwitchUserProfileUseCase(userProfileRepository)
        cloneApplicationUseCase = CloneApplicationUseCase(clonedAppRepository)
        deleteClonedApplicationUseCase = DeleteClonedApplicationUseCase(clonedAppRepository)
        getInstallableAppsUseCase = GetInstallableAppsUseCase(clonedAppRepository)
        launchClonedApplicationUseCase = LaunchClonedApplicationUseCase(clonedAppRepository)
    }

    // ==================== 1. Repository 接口验证测试 ====================

    @Test
    fun `UserProfileRepository interface should define getAllProfilesFlow method`() {
        // 验证接口方法存在 - 通过 mock 验证
        coEvery { userProfileRepository.getAllProfilesFlow() } returns mockk()
        
        val flow = userProfileRepository.getAllProfilesFlow()
        
        assertNotNull(flow)
        coVerify { userProfileRepository.getAllProfilesFlow() }
    }

    @Test
    fun `UserProfileRepository interface should define createProfile method`() {
        coEvery { userProfileRepository.createProfile(any()) } returns Result.success(10)
        
        runTest {
            val result = userProfileRepository.createProfile("Test Profile")
            
            assertTrue(result.isSuccess)
            assertEquals(10, result.getOrNull())
        }
    }

    @Test
    fun `UserProfileRepository interface should define deleteProfile method`() {
        coEvery { userProfileRepository.deleteProfile(any()) } returns Result.success(true)
        
        runTest {
            val result = userProfileRepository.deleteProfile(10)
            
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull() == true)
        }
    }

    @Test
    fun `ClonedApplicationRepository interface should define cloneApp method`() {
        val mockClonedApp = createMockClonedApp()
        coEvery { clonedAppRepository.cloneApp(any(), any()) } returns Result.success(mockClonedApp)
        
        runTest {
            val result = clonedAppRepository.cloneApp("com.example.app", 10)
            
            assertTrue(result.isSuccess)
            assertNotNull(result.getOrNull())
        }
    }

    @Test
    fun `ClonedApplicationRepository interface should define getInstallableApps method`() {
        coEvery { clonedAppRepository.getInstallableApps(any()) } returns Result.success(emptyList())
        
        runTest {
            val result = clonedAppRepository.getInstallableApps(true)
            
            assertTrue(result.isSuccess)
        }
    }

    // ==================== 2. UseCase 类验证测试 ====================

    @Test
    fun `CreateUserProfileUseCase should be instantiable with repository`() {
        assertNotNull(createUserProfileUseCase)
    }

    @Test
    fun `CreateUserProfileUseCase invoke should validate name is not blank`() {
        runTest {
            assertThrows(IllegalArgumentException::class.java) {
                createUserProfileUseCase("")
            }
        }
    }

    @Test
    fun `CreateUserProfileUseCase invoke should validate name length`() {
        val longName = "a".repeat(21)
        
        runTest {
            assertThrows(IllegalArgumentException::class.java) {
                createUserProfileUseCase(longName)
            }
        }
    }

    @Test
    fun `CreateUserProfileUseCase invoke should call repository with trimmed name`() {
        coEvery { userProfileRepository.createProfile("Test") } returns Result.success(10)
        
        runTest {
            val result = createUserProfileUseCase("  Test  ")
            
            assertTrue(result.isSuccess)
            coVerify { userProfileRepository.createProfile("Test") }
        }
    }

    @Test
    fun `DeleteUserProfileUseCase should be instantiable with repository`() {
        assertNotNull(deleteUserProfileUseCase)
    }

    @Test
    fun `DeleteUserProfileUseCase invoke should validate userId is positive`() {
        runTest {
            assertThrows(IllegalArgumentException::class.java) {
                deleteUserProfileUseCase(0)
            }
        }
    }

    @Test
    fun `DeleteUserProfileUseCase invoke should call repository with correct userId`() {
        coEvery { userProfileRepository.deleteProfile(10) } returns Result.success(true)
        
        runTest {
            val result = deleteUserProfileUseCase(10)
            
            assertTrue(result.isSuccess)
            coVerify { userProfileRepository.deleteProfile(10) }
        }
    }

    @Test
    fun `SwitchUserProfileUseCase should be instantiable with repository`() {
        assertNotNull(switchUserProfileUseCase)
    }

    @Test
    fun `SwitchUserProfileUseCase invoke should validate userId is positive`() {
        runTest {
            assertThrows(IllegalArgumentException::class.java) {
                switchUserProfileUseCase(-1)
            }
        }
    }

    @Test
    fun `CloneApplicationUseCase should be instantiable with repository`() {
        assertNotNull(cloneApplicationUseCase)
    }

    @Test
    fun `CloneApplicationUseCase invoke should validate packageName is not blank`() {
        runTest {
            assertThrows(IllegalArgumentException::class.java) {
                cloneApplicationUseCase("", 10)
            }
        }
    }

    @Test
    fun `CloneApplicationUseCase invoke should validate profileUserId is positive`() {
        runTest {
            assertThrows(IllegalArgumentException::class.java) {
                cloneApplicationUseCase("com.example.app", 0)
            }
        }
    }

    @Test
    fun `CloneApplicationUseCase invoke should call repository with correct parameters`() {
        val mockClonedApp = createMockClonedApp()
        coEvery { clonedAppRepository.cloneApp("com.example.app", 10) } returns Result.success(mockClonedApp)
        
        runTest {
            val result = cloneApplicationUseCase("com.example.app", 10)
            
            assertTrue(result.isSuccess)
            coVerify { clonedAppRepository.cloneApp("com.example.app", 10) }
        }
    }

    @Test
    fun `DeleteClonedApplicationUseCase should be instantiable with repository`() {
        assertNotNull(deleteClonedApplicationUseCase)
    }

    @Test
    fun `DeleteClonedApplicationUseCase invoke should validate clonedApp id is positive`() {
        val invalidApp = createMockClonedApp(id = 0)
        
        runTest {
            assertThrows(IllegalArgumentException::class.java) {
                deleteClonedApplicationUseCase(invalidApp)
            }
        }
    }

    @Test
    fun `GetInstallableAppsUseCase should be instantiable with repository`() {
        assertNotNull(getInstallableAppsUseCase)
    }

    @Test
    fun `GetInstallableAppsUseCase invoke should call repository with default parameter`() {
        coEvery { clonedAppRepository.getInstallableApps(false) } returns Result.success(emptyList())
        
        runTest {
            val result = getInstallableAppsUseCase()
            
            assertTrue(result.isSuccess)
            coVerify { clonedAppRepository.getInstallableApps(false) }
        }
    }

    @Test
    fun `LaunchClonedApplicationUseCase should be instantiable with repository`() {
        assertNotNull(launchClonedApplicationUseCase)
    }

    @Test
    fun `LaunchClonedApplicationUseCase invoke should validate clonedApp id is positive`() {
        val invalidApp = createMockClonedApp(id = 0)
        
        runTest {
            assertThrows(IllegalArgumentException::class.java) {
                launchClonedApplicationUseCase(invalidApp)
            }
        }
    }

    // ==================== 3. BaseViewModel 验证测试 ====================

    @Test
    fun `BaseViewModel should be abstract class`() {
        // 通过反射验证 BaseViewModel 是抽象类
        val viewModelClass = com.appclonex.presentation.base.BaseViewModel::class.java
        assertTrue(java.lang.reflect.Modifier.isAbstract(viewModelClass.modifiers))
    }

    @Test
    fun `BaseViewModel should have uiState property`() {
        // 验证 BaseViewModel 有 uiState 属性
        val viewModelClass = com.appclonex.presentation.base.BaseViewModel::class.java
        val uiStateField = viewModelClass.getDeclaredField("uiState")
        assertNotNull(uiStateField)
    }

    @Test
    fun `BaseViewModel should have updateState method`() {
        // 验证 BaseViewModel 有 updateState 方法
        val viewModelClass = com.appclonex.presentation.base.BaseViewModel::class.java
        val updateStateMethod = viewModelClass.getDeclaredMethod("updateState", kotlin.jvm.functions.Function1::class.java)
        assertNotNull(updateStateMethod)
    }

    @Test
    fun `BaseViewModel should have exceptionHandler property`() {
        // 验证 BaseViewModel 有 exceptionHandler 属性
        val viewModelClass = com.appclonex.presentation.base.BaseViewModel::class.java
        val exceptionHandlerField = viewModelClass.getDeclaredField("exceptionHandler")
        assertNotNull(exceptionHandlerField)
    }

    // ==================== 4. Error Handling 验证测试 ====================

    @Test
    fun `OperationError ShizukuNotAvailable should have message property`() {
        val error = OperationError.ShizukuNotAvailable("Test message")
        assertEquals("Test message", error.message)
    }

    @Test
    fun `OperationError PermissionDenied should have message property`() {
        val error = OperationError.PermissionDenied("Permission required")
        assertEquals("Permission required", error.message)
    }

    @Test
    fun `OperationError ApplicationNotFound should have packageName property`() {
        val error = OperationError.ApplicationNotFound("com.example.app")
        assertEquals("com.example.app", error.packageName)
    }

    @Test
    fun `OperationError CloneFailed should have reason property`() {
        val error = OperationError.CloneFailed("Installation failed")
        assertEquals("Installation failed", error.reason)
    }

    @Test
    fun `OperationError FreezeFailed should have reason property`() {
        val error = OperationError.FreezeFailed("Freeze operation failed")
        assertEquals("Freeze operation failed", error.reason)
    }

    @Test
    fun `OperationError SandboxError should have reason property`() {
        val error = OperationError.SandboxError("Sandbox creation failed")
        assertEquals("Sandbox creation failed", error.reason)
    }

    @Test
    fun `OperationError DatabaseError should have reason property`() {
        val error = OperationError.DatabaseError("Query failed")
        assertEquals("Query failed", error.reason)
    }

    @Test
    fun `OperationError NetworkError should have reason property`() {
        val error = OperationError.NetworkError("Connection timeout")
        assertEquals("Connection timeout", error.reason)
    }

    @Test
    fun `OperationError UnknownError should have throwable property`() {
        val exception = RuntimeException("Test exception")
        val error = OperationError.UnknownError(exception)
        assertEquals(exception, error.throwable)
    }

    @Test
    fun `OperationResult should be type alias for Result`() {
        // 验证 OperationResult 是 Result<T, OperationError> 的类型别名
        val result: OperationResult<String> = Ok("test")
        assertTrue(result.isOk)
    }

    @Test
    fun `getErrorMessage extension should return correct message for each error type`() {
        assertEquals(
            "Shizuku error",
            Err(OperationError.ShizukuNotAvailable("Shizuku error")).getErrorMessage()
        )
        assertEquals(
            "Permission denied",
            Err(OperationError.PermissionDenied("Permission denied")).getErrorMessage()
        )
        assertEquals(
            "Application not found: com.test.app",
            Err(OperationError.ApplicationNotFound("com.test.app")).getErrorMessage()
        )
        assertEquals(
            "Clone failed: test reason",
            Err(OperationError.CloneFailed("test reason")).getErrorMessage()
        )
    }

    @Test
    fun `isRetryable extension should return true for retryable errors`() {
        assertTrue(Err(OperationError.NetworkError("timeout")).isRetryable())
        assertTrue(Err(OperationError.ShizukuNotAvailable()).isRetryable())
        assertTrue(Err(OperationError.CloneFailed("failed")).isRetryable())
    }

    @Test
    fun `isRetryable extension should return false for non-retryable errors`() {
        assertFalse(Err(OperationError.PermissionDenied()).isRetryable())
        assertFalse(Err(OperationError.ApplicationNotFound("com.app")).isRetryable())
        assertFalse(Err(OperationError.DatabaseError("error")).isRetryable())
    }

    // ==================== 5. Documentation 验证测试（通过类存在验证）====================

    @Test
    fun `Repository interfaces should exist in domain layer`() {
        // 验证 Repository 接口存在
        assertNotNull(Class.forName("com.appclonex.domain.repository.UserProfileRepository"))
        assertNotNull(Class.forName("com.appclonex.domain.repository.ClonedApplicationRepository"))
    }

    @Test
    fun `UseCase classes should exist in domain layer`() {
        // 验证 UseCase 类存在
        assertNotNull(Class.forName("com.appclonex.domain.usecase.profile.CreateUserProfileUseCase"))
        assertNotNull(Class.forName("com.appclonex.domain.usecase.profile.DeleteUserProfileUseCase"))
        assertNotNull(Class.forName("com.appclonex.domain.usecase.profile.SwitchUserProfileUseCase"))
        assertNotNull(Class.forName("com.appclonex.domain.usecase.clone.CloneApplicationUseCase"))
        assertNotNull(Class.forName("com.appclonex.domain.usecase.clone.DeleteClonedApplicationUseCase"))
        assertNotNull(Class.forName("com.appclonex.domain.usecase.clone.GetInstallableAppsUseCase"))
        assertNotNull(Class.forName("com.appclonex.domain.usecase.clone.LaunchClonedApplicationUseCase"))
    }

    @Test
    fun `BaseViewModel should exist in presentation layer`() {
        assertNotNull(Class.forName("com.appclonex.presentation.base.BaseViewModel"))
    }

    @Test
    fun `BaseUiState interface should exist in presentation layer`() {
        assertNotNull(Class.forName("com.appclonex.presentation.base.BaseUiState"))
    }

    @Test
    fun `MessageState class should exist in presentation layer`() {
        assertNotNull(Class.forName("com.appclonex.presentation.base.MessageState"))
    }

    @Test
    fun `OperationError sealed class should exist in domain layer`() {
        assertNotNull(Class.forName("com.appclonex.domain.model.OperationError"))
    }

    // ==================== 辅助方法 ====================

    private fun createMockClonedApp(
        id: Long = 1L,
        packageName: String = "com.example.app",
        appName: String = "Test App",
        profileUserId: Int = 10
    ): ClonedApplicationInformationModel {
        return ClonedApplicationInformationModel(
            id = id,
            packageName = packageName,
            appName = appName,
            profileUserId = profileUserId,
            cloneIndex = 1,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
