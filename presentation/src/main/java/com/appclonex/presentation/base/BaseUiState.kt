package com.appclonex.presentation.base

/**
 * Base UI state interface.
 *
 * Provides common properties for all UI states in the application:
 * - isLoading: Indicates if an async operation is in progress
 * - message: A user-facing message (info, success, warning)
 * - error: An error message if something went wrong
 *
 * All UI state classes should implement this interface.
 */
interface BaseUiState {
    val isLoading: Boolean
    val message: String?
    val error: String?
}

/**
 * Sealed class representing different types of messages.
 *
 * @property message The actual message content
 * @property type The type/category of the message
 */
data class MessageState(
    val message: String? = null,
    val type: MessageType = MessageType.INFO
) {
    enum class MessageType {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    companion object {
        fun info(message: String) = MessageState(message, MessageType.INFO)
        fun success(message: String) = MessageState(message, MessageType.SUCCESS)
        fun warning(message: String) = MessageState(message, MessageType.WARNING)
        fun error(message: String) = MessageState(message, MessageType.ERROR)
    }
}
