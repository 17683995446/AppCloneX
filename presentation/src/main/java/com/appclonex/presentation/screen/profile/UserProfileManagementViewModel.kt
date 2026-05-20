package com.appclonex.presentation.screen.profile

import com.appclonex.domain.model.UserProfileConfigurationModel
import com.appclonex.domain.repository.UserProfileRepository
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * UI state for user profile management screen.
 */
data class UserProfileUiState(
    override val isLoading: Boolean = false,
    override val message: String? = null,
    override val error: String? = null,
    val profiles: List<UserProfileConfigurationModel> = emptyList(),
    val selectedProfileId: Int? = null,
    val isCreating: Boolean = false,
    val maxUserCount: Int = 0
) : BaseUiState

/**
 * ViewModel for managing user profiles.
 *
 * Responsibilities:
 * - List all user profiles
 * - Create new profiles
 * - Delete existing profiles
 * - Switch between profiles
 */
@HiltViewModel
class UserProfileManagementViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : BaseViewModel<UserProfileUiState>(UserProfileUiState()) {

    init {
        loadProfiles()
        loadMaxUserCount()
    }

    fun loadProfiles() {
        launchWithErrorHandler {
            updateState { copy(isLoading = true) }

            userProfileRepository.getAllProfilesFlow().collectLatest { profiles ->
                updateState {
                    copy(
                        profiles = profiles,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadMaxUserCount() {
        launchWithErrorHandler {
            userProfileRepository.getMaxUserCount()
                .onSuccess { count ->
                    updateState { copy(maxUserCount = count) }
                }
        }
    }

    fun createProfile(name: String) {
        if (currentState.profiles.size >= currentState.maxUserCount) {
            updateState { copy(error = "已达到最大用户数量限制") }
            return
        }

        launchWithErrorHandler {
            updateState { copy(isCreating = true) }

            userProfileRepository.createProfile(name)
                .onSuccess { newUserId ->
                    updateState {
                        copy(
                            message = "配置文件已创建: $name",
                            isCreating = false
                        )
                    }
                }
                .onFailure { exception ->
                    updateState {
                        copy(
                            error = exception.message ?: "创建失败",
                            isCreating = false
                        )
                    }
                }
        }
    }

    fun deleteProfile(userId: Int) {
        if (userId == 0) {
            updateState { copy(error = "无法删除主用户") }
            return
        }

        launchWithErrorHandler {
            updateState { copy(isLoading = true) }

            userProfileRepository.deleteProfile(userId)
                .onSuccess {
                    updateState { copy(message = "配置文件已删除", isLoading = false) }
                }
                .onFailure { exception ->
                    updateState {
                        copy(
                            error = exception.message ?: "删除失败",
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun switchToProfile(userId: Int) {
        launchWithErrorHandler {
            updateState { copy(isLoading = true) }

            userProfileRepository.switchToProfile(userId)
                .onSuccess {
                    updateState {
                        copy(
                            selectedProfileId = userId,
                            message = "已切换到配置文件",
                            isLoading = false
                        )
                    }
                }
                .onFailure { exception ->
                    updateState {
                        copy(
                            error = exception.message ?: "切换失败",
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun updateProfileName(userId: Int, newName: String) {
        launchWithErrorHandler {
            updateState { copy(isLoading = true) }

            userProfileRepository.updateProfileName(userId, newName)
                .also {
                    updateState { copy(message = "名称已更新", isLoading = false) }
                }
        }
    }

    fun syncWithSystem() {
        launchWithErrorHandler {
            updateState { copy(isLoading = true) }

            userProfileRepository.syncWithSystem()
                .onSuccess {
                    updateState { copy(message = "同步成功", isLoading = false) }
                }
                .onFailure { exception ->
                    updateState {
                        copy(
                            error = exception.message ?: "同步失败",
                            isLoading = false
                        )
                    }
                }
        }
    }
}
