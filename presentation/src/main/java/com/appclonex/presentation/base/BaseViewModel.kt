package com.appclonex.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel providing common functionality for all ViewModels.
 *
 * Features:
 * - Automatic state management with [StateFlow]
 * - Built-in exception handling via [CoroutineExceptionHandler]
 * - Helper methods for updating state and launching coroutines
 *
 * @param S The type of UI state managed by this ViewModel
 * @param initialState The initial state value
 */
abstract class BaseViewModel<S : BaseUiState>(
    initialState: S
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    /**
     * The current state value.
     */
    protected val currentState: S
        get() = _uiState.value

    /**
     * Exception handler for coroutine errors.
     *
     * CancellationException is re-thrown to allow proper coroutine cancellation.
     * Other exceptions are passed to [onError].
     */
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        // CancellationException 是协程正常取消机制，不应作为错误处理
        if (throwable is CancellationException) return@CoroutineExceptionHandler
        onError(throwable)
    }

    /**
     * Called when an unhandled exception occurs in a coroutine.
     *
     * Default implementation updates the error state.
     * Override to provide custom error handling.
     *
     * @param throwable The exception that was thrown
     */
    protected open fun onError(throwable: Throwable) {
        _uiState.update { currentState ->
            @Suppress("UNCHECKED_CAST")
            (currentState as? S)?.copy(
                error = throwable.message ?: "An unexpected error occurred"
            ) ?: currentState
        }
    }

    /**
     * Updates the state using a reducer function.
     *
     * @param reducer A lambda that receives the current state and returns a new state
     */
    protected fun updateState(reducer: S.() -> S) {
        _uiState.update { currentState ->
            currentState.reducer()
        }
    }

    /**
     * Launches a coroutine with automatic error handling.
     *
     * Any exception thrown inside the block will be caught and passed to [onError].
     *
     * R13-001 修复：返回 Job 引用，支持调用方取消协程
     * - 之前返回 Unit，导致 `job = launchWithErrorHandler { }` 中 job 为 Unit
     * - `job?.cancel()` 无法真正取消协程，造成 Flow 收集泄漏
     *
     * @param block The suspend lambda to execute
     * @return Job 引用，可用于取消协程
     */
    protected fun launchWithErrorHandler(block: suspend () -> Unit): kotlinx.coroutines.Job {
        return viewModelScope.launch(exceptionHandler) {
            block()
        }
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }

    /**
     * Shows a message to the user.
     *
     * @param message The message to display
     */
    fun showMessage(message: String) {
        updateState { copy(message = message) }
    }

    /**
     * Clears the current message.
     */
    fun clearMessage() {
        updateState { copy(message = null) }
    }
}
