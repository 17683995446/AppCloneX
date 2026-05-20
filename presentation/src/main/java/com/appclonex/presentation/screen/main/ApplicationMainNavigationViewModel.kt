package com.appclonex.presentation.screen.main

import com.appclonex.domain.model.ShizukuConnectionState
import com.appclonex.domain.repository.PreferencesRepository
import com.appclonex.domain.repository.ShizukuStateRepository
import com.appclonex.domain.repository.UserProfileRepository
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * UI state for main navigation screen.
 */
data class MainUiState(
    override val isLoading: Boolean = false,
    override val message: String? = null,
    override val error: String? = null,
    val shizukuState: ShizukuConnectionState = ShizukuConnectionState.DISCONNECTED,
    val showOnboarding: Boolean = true,
    val currentTab: Int = 0
) : BaseUiState

/**
 * ViewModel for main navigation.
 *
 * Responsibilities:
 * - Check and monitor Shizuku status
 * - Handle onboarding flow
 * - Manage navigation state
 */
@HiltViewModel
class ApplicationMainNavigationViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val preferencesRepository: PreferencesRepository,
    private val shizukuStateRepository: ShizukuStateRepository
) : BaseViewModel<MainUiState>(MainUiState()) {

    init {
        shizukuStateRepository.init()
        checkShizukuState()
        checkOnboardingStatus()
    }

    fun checkShizukuState() {
        launchWithErrorHandler {
            shizukuStateRepository.shizukuState.collectLatest { newState ->
                updateState { copy(shizukuState = newState) }
            }
        }
    }

    private fun checkOnboardingStatus() {
        launchWithErrorHandler {
            preferencesRepository.isOnboardingCompleted().collectLatest { completed ->
                updateState { copy(showOnboarding = !completed) }
            }
        }
    }

    fun completeOnboarding() {
        launchWithErrorHandler {
            preferencesRepository.setOnboardingCompleted()

            updateState {
                copy(
                    showOnboarding = false,
                    message = "设置完成"
                )
            }
        }
    }

    fun setCurrentTab(tabIndex: Int) {
        updateState { copy(currentTab = tabIndex) }
    }

    fun navigateToApps() {
        setCurrentTab(1)
    }

    fun navigateToSettings() {
        setCurrentTab(2)
    }

    fun navigateToHome() {
        setCurrentTab(0)
    }

    override fun onCleared() {
        super.onCleared()
        shizukuStateRepository.destroy()
    }
}
