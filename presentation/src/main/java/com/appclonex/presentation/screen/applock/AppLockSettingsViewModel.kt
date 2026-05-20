package com.appclonex.presentation.screen.applock

import com.appclonex.domain.model.AppLockConfiguration
import com.appclonex.domain.repository.AppLockRepository
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * UI state for app lock settings screen.
 */
data class AppLockSettingsUiState(
    override val isLoading: Boolean = false,
    override val message: String? = null,
    override val error: String? = null,
    val currentLockType: AppLockConfiguration.LockType = AppLockConfiguration.LockType.NONE,
    val lockedApps: List<String> = emptyList(),
    val isSettingPin: Boolean = false,
    val isEnrollingBiometric: Boolean = false
) : BaseUiState

/**
 * ViewModel for app lock settings.
 *
 * Responsibilities:
 * - Load current lock status
 * - Set/remove PIN lock
 * - Enroll biometric lock
 * - Lock/unlock individual apps
 */
@HiltViewModel
class AppLockSettingsViewModel @Inject constructor(
    private val appLockRepository: AppLockRepository
) : BaseViewModel<AppLockSettingsUiState>(AppLockSettingsUiState()) {

    init {
        loadLockStatus()
    }

    /**
     * Load current lock status and locked apps list.
     */
    fun loadLockStatus() {
        launchWithErrorHandler {
            updateState { copy(isLoading = true) }

            appLockRepository.getAllLockedApps().collectLatest { lockedConfigs ->
                val lockedPackageNames = lockedConfigs
                    .filter { it.isLocked }
                    .map { it.packageName }

                // Determine current lock type from the first locked config
                val lockType = lockedConfigs
                    .firstOrNull { it.isLocked }
                    ?.lockType
                    ?: AppLockConfiguration.LockType.NONE

                updateState {
                    copy(
                        currentLockType = lockType,
                        lockedApps = lockedPackageNames,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Set a PIN lock for the given package name.
     */
    fun setPin(pin: String) {
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            updateState { copy(error = "PIN must be exactly 4 digits") }
            return
        }

        launchWithErrorHandler {
            updateState { copy(isSettingPin = true) }

            val result = appLockRepository.setPin(
                packageName = "global",
                profileUserId = 0,
                pin = pin
            )

            result.onSuccess {
                updateState {
                    copy(
                        currentLockType = AppLockConfiguration.LockType.PIN,
                        isSettingPin = false,
                        message = "PIN lock set successfully"
                    )
                }
            }.onFailure { exception ->
                updateState {
                    copy(
                        error = exception.message ?: "Failed to set PIN",
                        isSettingPin = false
                    )
                }
            }
        }
    }

    /**
     * Remove the current PIN lock.
     */
    fun removePin() {
        launchWithErrorHandler {
            updateState { copy(isLoading = true) }

            val result = appLockRepository.removeAppLock(
                packageName = "global",
                profileUserId = 0
            )

            result.onSuccess {
                updateState {
                    copy(
                        currentLockType = AppLockConfiguration.LockType.NONE,
                        isLoading = false,
                        message = "PIN lock removed"
                    )
                }
            }.onFailure { exception ->
                updateState {
                    copy(
                        error = exception.message ?: "Failed to remove PIN",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Enroll biometric lock.
     */
    fun enrollBiometric() {
        launchWithErrorHandler {
            updateState { copy(isEnrollingBiometric = true) }

            val config = AppLockConfiguration(
                packageName = "global",
                profileUserId = 0,
                lockType = AppLockConfiguration.LockType.BIOMETRIC
            )

            val result = appLockRepository.setAppLock(config)

            result.onSuccess {
                updateState {
                    copy(
                        currentLockType = AppLockConfiguration.LockType.BIOMETRIC,
                        isEnrollingBiometric = false,
                        message = "Biometric lock enabled"
                    )
                }
            }.onFailure { exception ->
                updateState {
                    copy(
                        error = exception.message ?: "Failed to enable biometric lock",
                        isEnrollingBiometric = false
                    )
                }
            }
        }
    }

    /**
     * Lock a specific app by package name.
     */
    fun lockApp(packageName: String) {
        launchWithErrorHandler {
            val config = AppLockConfiguration(
                packageName = packageName,
                profileUserId = 0,
                lockType = currentState.currentLockType
            )

            val result = appLockRepository.setAppLock(config)

            result.onSuccess {
                updateState { copy(message = "App locked: $packageName") }
            }.onFailure { exception ->
                updateState { copy(error = exception.message ?: "Failed to lock app") }
            }
        }
    }

    /**
     * Unlock a specific app by package name.
     */
    fun unlockApp(packageName: String) {
        launchWithErrorHandler {
            val result = appLockRepository.removeAppLock(
                packageName = packageName,
                profileUserId = 0
            )

            result.onSuccess {
                updateState { copy(message = "App unlocked: $packageName") }
            }.onFailure { exception ->
                updateState { copy(error = exception.message ?: "Failed to unlock app") }
            }
        }
    }
}
