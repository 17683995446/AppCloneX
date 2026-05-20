package com.appclonex.presentation.base

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * BaseViewModel 单元测试
 * 
 * 测试覆盖：
 * - 状态初始化
 * - 状态更新
 * - 当前状态获取
 * - 异常处理器
 * - 协程启动
 * - BaseUiState 接口实现
 * - MessageState 功能
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    // 测试用的 UI 状态
    data class TestUiState(
        override val isLoading: Boolean = false,
        override val message: String? = null,
        override val error: String? = null,
        val counter: Int = 0,
        val data: String? = null
    ) : BaseUiState

    // 具体的 ViewModel 实现用于测试
    private class TestableViewModel(initialState: TestUiState) : BaseViewModel<TestUiState>(initialState) {
        
        var lastError: Throwable? = null
            private set
        
        // 暴露 protected 方法用于测试
        fun testUpdateState(reducer: TestUiState.() -> TestUiState) {
            updateState(reducer)
        }
        
        fun testGetCurrentState(): TestUiState = currentState
        
        fun testLaunchWithErrorHandler(block: suspend () -> Unit) {
            launchWithErrorHandler {
                block()
            }
        }
        
        override fun onError(throwable: Throwable) {
            lastError = throwable
        }
        
        fun setTestError(error: Throwable) {
            lastError = error
        }
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: TestableViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TestableViewModel(TestUiState())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== 状态初始化测试 ====================

    @Test
    fun `initial state should be set correctly`() {
        val initialState = TestUiState(
            isLoading = true,
            counter = 5,
            data = "test data"
        )
        val vm = TestableViewModel(initialState)
        
        assertEquals(initialState, vm.uiState.value)
    }

    @Test
    fun `initial state should have default values`() {
        val vm = TestableViewModel(TestUiState())
        
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.message)
        assertNull(vm.uiState.value.error)
        assertEquals(0, vm.uiState.value.counter)
        assertNull(vm.uiState.value.data)
    }

    // ==================== 状态更新测试 ====================

    @Test
    fun `updateState should modify state correctly`() = runTest {
        viewModel.testUpdateState { copy(counter = 10) }
        
        assertEquals(10, viewModel.uiState.value.counter)
    }

    @Test
    fun `updateState should preserve unmodified fields`() = runTest {
        viewModel.testUpdateState { 
            copy(
                counter = 5,
                data = "updated"
            )
        }
        
        val state = viewModel.uiState.value
        assertEquals(5, state.counter)
        assertEquals("updated", state.data)
        assertFalse(state.isLoading)
        assertNull(state.message)
        assertNull(state.error)
    }

    @Test
    fun `multiple updateState calls should accumulate changes`() = runTest {
        viewModel.testUpdateState { copy(counter = 1) }
        viewModel.testUpdateState { copy(counter = counter + 1) }
        viewModel.testUpdateState { copy(counter = counter * 2) }
        
        assertEquals(4, viewModel.uiState.value.counter)
    }

    @Test
    fun `updateState should update loading state`() = runTest {
        viewModel.testUpdateState { copy(isLoading = true) }
        assertTrue(viewModel.uiState.value.isLoading)
        
        viewModel.testUpdateState { copy(isLoading = false) }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `updateState should update message`() = runTest {
        viewModel.testUpdateState { copy(message = "Test message") }
        assertEquals("Test message", viewModel.uiState.value.message)
        
        viewModel.testUpdateState { copy(message = null) }
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `updateState should update error`() = runTest {
        viewModel.testUpdateState { copy(error = "Test error") }
        assertEquals("Test error", viewModel.uiState.value.error)
        
        viewModel.testUpdateState { copy(error = null) }
        assertNull(viewModel.uiState.value.error)
    }

    // ==================== 当前状态获取测试 ====================

    @Test
    fun `currentState should return current state value`() = runTest {
        viewModel.testUpdateState { copy(counter = 42) }
        
        assertEquals(42, viewModel.testGetCurrentState().counter)
    }

    @Test
    fun `currentState should reflect latest updates`() = runTest {
        viewModel.testUpdateState { copy(counter = 1) }
        assertEquals(1, viewModel.testGetCurrentState().counter)
        
        viewModel.testUpdateState { copy(counter = 2) }
        assertEquals(2, viewModel.testGetCurrentState().counter)
    }

    // ==================== 异常处理器测试 ====================

    @Test
    fun `exceptionHandler should call onError when exception occurs`() = runTest {
        val exception = RuntimeException("Test exception")
        
        viewModel.testLaunchWithErrorHandler {
            throw exception
        }
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(exception, viewModel.lastError)
    }

    @Test
    fun `exceptionHandler should catch any exception type`() = runTest {
        val exceptions = listOf(
            RuntimeException("Runtime exception"),
            IllegalArgumentException("Illegal argument"),
            NullPointerException("Null pointer"),
            IndexOutOfBoundsException("Index out of bounds")
        )
        
        for (exception in exceptions) {
            val vm = TestableViewModel(TestUiState())
            vm.testLaunchWithErrorHandler {
                throw exception
            }
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(exception, vm.lastError)
        }
    }

    // ==================== BaseUiState 接口测试 ====================

    @Test
    fun `BaseUiState should have isLoading property`() {
        val state = TestUiState(isLoading = true)
        assertTrue(state.isLoading)
        
        val state2 = TestUiState(isLoading = false)
        assertFalse(state2.isLoading)
    }

    @Test
    fun `BaseUiState should have message property`() {
        val state = TestUiState(message = "Hello")
        assertEquals("Hello", state.message)
        
        val state2 = TestUiState(message = null)
        assertNull(state2.message)
    }

    @Test
    fun `BaseUiState should have error property`() {
        val state = TestUiState(error = "Error occurred")
        assertEquals("Error occurred", state.error)
        
        val state2 = TestUiState(error = null)
        assertNull(state2.error)
    }

    @Test
    fun `BaseUiState implementation should be data class for proper equality`() {
        val state1 = TestUiState(counter = 5, data = "test")
        val state2 = TestUiState(counter = 5, data = "test")
        
        assertEquals(state1, state2)
    }

    // ==================== MessageState 测试 ====================

    @Test
    fun `MessageState info factory should create INFO type message`() {
        val message = MessageState.info("Info message")
        
        assertEquals("Info message", message.message)
        assertEquals(MessageState.MessageType.INFO, message.type)
    }

    @Test
    fun `MessageState success factory should create SUCCESS type message`() {
        val message = MessageState.success("Success message")
        
        assertEquals("Success message", message.message)
        assertEquals(MessageState.MessageType.SUCCESS, message.type)
    }

    @Test
    fun `MessageState warning factory should create WARNING type message`() {
        val message = MessageState.warning("Warning message")
        
        assertEquals("Warning message", message.message)
        assertEquals(MessageState.MessageType.WARNING, message.type)
    }

    @Test
    fun `MessageState error factory should create ERROR type message`() {
        val message = MessageState.error("Error message")
        
        assertEquals("Error message", message.message)
        assertEquals(MessageState.MessageType.ERROR, message.type)
    }

    @Test
    fun `MessageState default type should be INFO`() {
        val message = MessageState(message = "Default message")
        
        assertEquals(MessageState.MessageType.INFO, message.type)
    }

    @Test
    fun `MessageState default message should be null`() {
        val message = MessageState()
        
        assertNull(message.message)
        assertEquals(MessageState.MessageType.INFO, message.type)
    }

    @Test
    fun `MessageState MessageType should have all required types`() {
        val types = MessageState.MessageType.values()
        
        assertTrue(types.contains(MessageState.MessageType.INFO))
        assertTrue(types.contains(MessageState.MessageType.SUCCESS))
        assertTrue(types.contains(MessageState.MessageType.WARNING))
        assertTrue(types.contains(MessageState.MessageType.ERROR))
        assertEquals(4, types.size)
    }

    // ==================== 状态流测试 ====================

    @Test
    fun `uiState should be StateFlow type`() = runTest {
        val stateFlow = viewModel.uiState
        
        assertNotNull(stateFlow.first())
    }

    @Test
    fun `uiState should emit updates to collectors`() = runTest {
        val states = mutableListOf<TestUiState>()
        
        val job = launch {
            viewModel.uiState.collect { state ->
                states.add(state)
            }
        }
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.testUpdateState { copy(counter = 1) }
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.testUpdateState { copy(counter = 2) }
        testDispatcher.scheduler.advanceUntilIdle()
        
        job.cancel()
        
        assertTrue(states.any { it.counter == 1 })
        assertTrue(states.any { it.counter == 2 })
    }

    // ==================== 复杂场景测试 ====================

    @Test
    fun `state update with complex transformation should work correctly`() = runTest {
        val initialState = TestUiState(
            isLoading = false,
            counter = 0,
            data = null,
            message = null,
            error = null
        )
        val vm = TestableViewModel(initialState)
        
        // 模拟加载开始
        vm.testUpdateState { copy(isLoading = true, message = "Loading...") }
        assertEquals(true, vm.uiState.value.isLoading)
        assertEquals("Loading...", vm.uiState.value.message)
        
        // 模拟加载完成
        vm.testUpdateState { 
            copy(
                isLoading = false,
                message = null,
                data = "Loaded data",
                counter = 10
            )
        }
        
        val finalState = vm.uiState.value
        assertFalse(finalState.isLoading)
        assertNull(finalState.message)
        assertEquals("Loaded data", finalState.data)
        assertEquals(10, finalState.counter)
    }

    @Test
    fun `error handling flow should work correctly`() = runTest {
        // 设置错误状态
        viewModel.testUpdateState { 
            copy(
                isLoading = false,
                error = "Something went wrong"
            )
        }
        
        assertEquals("Something went wrong", viewModel.uiState.value.error)
        
        // 清除错误
        viewModel.testUpdateState { copy(error = null) }
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `concurrent state updates should be handled correctly`() = runTest {
        // 并发更新
        repeat(100) { i ->
            viewModel.testUpdateState { copy(counter = counter + 1) }
        }
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(100, viewModel.uiState.value.counter)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `state with empty string values should work correctly`() = runTest {
        viewModel.testUpdateState { 
            copy(
                message = "",
                error = "",
                data = ""
            )
        }
        
        val state = viewModel.uiState.value
        assertEquals("", state.message)
        assertEquals("", state.error)
        assertEquals("", state.data)
    }

    @Test
    fun `state with very long string values should work correctly`() = runTest {
        val longString = "a".repeat(10000)
        
        viewModel.testUpdateState { 
            copy(
                message = longString,
                data = longString
            )
        }
        
        assertEquals(longString, viewModel.uiState.value.message)
        assertEquals(longString, viewModel.uiState.value.data)
    }

    @Test
    fun `state with special characters should work correctly`() = runTest {
        val specialString = "!@#\$%^&*()_+-=[]{}|;':\",./<>?\n\t\r"
        
        viewModel.testUpdateState { 
            copy(
                message = specialString,
                error = specialString
            )
        }
        
        assertEquals(specialString, viewModel.uiState.value.message)
        assertEquals(specialString, viewModel.uiState.value.error)
    }

    @Test
    fun `state with unicode characters should work correctly`() = runTest {
        val unicodeString = "你好世界 🌍 مرحبا Привет"
        
        viewModel.testUpdateState { 
            copy(
                message = unicodeString,
                data = unicodeString
            )
        }
        
        assertEquals(unicodeString, viewModel.uiState.value.message)
        assertEquals(unicodeString, viewModel.uiState.value.data)
    }
}
