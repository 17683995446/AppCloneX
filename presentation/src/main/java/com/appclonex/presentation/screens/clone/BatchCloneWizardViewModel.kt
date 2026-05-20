package com.appclonex.presentation.screens.clone

import androidx.lifecycle.viewModelScope
import com.appclonex.domain.model.*
import com.appclonex.domain.usecase.clone.BatchCloneUseCase
import com.appclonex.presentation.base.BaseUiState
import com.appclonex.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// 向导步骤常量
// ─────────────────────────────────────────────────────────────────────────────
object WizardStep {
    const val SELECT_PROFILE = 0
    const val SELECT_APPS = 1
    const val CONFIGURE_OPTIONS = 2
    const val REVIEW_CONFIRM = 3
    const val PROGRESS = 4
    const val SUMMARY = 5
    const val TOTAL_STEPS = 6
}

// ─────────────────────────────────────────────────────────────────────────────
// UI 状态
// ─────────────────────────────────────────────────────────────────────────────
data class BatchCloneWizardUiState(
    val currentStep: Int = WizardStep.SELECT_PROFILE,
    override val isLoading: Boolean = false,
    // 步骤 1 - 选择配置文件
    val profiles: List<ProfileItem> = emptyList(),
    val selectedProfile: ProfileItem? = null,
    // 步骤 2 - 选择应用
    val allApps: List<AppItem> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    // 步骤 3 - 配置选项
    val options: BatchCloneOptions = BatchCloneOptions(),
    // 步骤 4 - 确认
    // (组合以上字段即可)
    // 步骤 5 - 进度
    val progress: BatchCloneProgress = BatchCloneProgress(),
    // 步骤 6 - 摘要
    val summary: BatchCloneSummary? = null,
    // 通用
    override val message: String? = null,
    override val error: String? = null
) : BaseUiState

/**
 * 配置文件条目（UI 展示用）
 */
data class ProfileItem(
    val id: Int,
    val name: String,
    val appCount: Int = 0,
    val isAvailable: Boolean = true
)

/**
 * 应用条目（UI 展示用）
 */
data class AppItem(
    val packageName: String,
    val appName: String,
    val icon: Any? = null,           // Bitmap / ImageVector 占位
    val versionName: String = "",
    val isAlreadyCloned: Boolean = false,
    val sizeBytes: Long = 0L
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class BatchCloneWizardViewModel @Inject constructor(
    private val batchCloneUseCase: BatchCloneUseCase
) : BaseViewModel<BatchCloneWizardUiState>(BatchCloneWizardUiState()) {

    private var cloneJob: Job? = null

    // ──────────────── 初始化 ────────────────
    init {
        loadProfiles()
        loadInstalledApps()
    }

    // ──────────────── 数据加载 ────────────────
    private fun loadProfiles() {
        // TODO: 从 repository 获取真实配置文件列表
        // 此处提供模拟数据以展示完整逻辑
        val demoProfiles = listOf(
            ProfileItem(id = 1, name = "工作空间", appCount = 5),
            ProfileItem(id = 2, name = "个人空间", appCount = 3),
            ProfileItem(id = 3, name = "测试空间", appCount = 0)
        )
        updateState { copy(profiles = demoProfiles) }
    }

    private fun loadInstalledApps() {
        // TODO: 从 repository 获取真实已安装应用列表
        val demoApps = listOf(
            AppItem("com.example.app1", "微信", isAlreadyCloned = false, versionName = "8.0.1"),
            AppItem("com.example.app2", "QQ", isAlreadyCloned = false, versionName = "9.0.0"),
            AppItem("com.example.app3", "抖音", isAlreadyCloned = true, versionName = "12.0.0"),
            AppItem("com.example.app4", "支付宝", isAlreadyCloned = false, versionName = "10.3.0"),
            AppItem("com.example.app5", "淘宝", isAlreadyCloned = false, versionName = "11.0.0"),
            AppItem("com.example.app6", "微博", isAlreadyCloned = false, versionName = "13.0.0"),
            AppItem("com.example.app7", "网易云音乐", isAlreadyCloned = true, versionName = "8.5.0"),
            AppItem("com.example.app8", "哔哩哔哩", isAlreadyCloned = false, versionName = "7.0.0")
        )
        updateState { copy(allApps = demoApps) }
    }

    // ──────────────── 步骤导航 ────────────────
    fun nextStep() {
        updateState {
            val next = (currentStep + 1).coerceAtMost(WizardStep.SUMMARY)
            copy(currentStep = next, error = null)
        }
    }

    fun previousStep() {
        updateState {
            val prev = (currentStep - 1).coerceAtLeast(WizardStep.SELECT_PROFILE)
            copy(currentStep = prev, error = null)
        }
    }

    fun goToStep(step: Int) {
        updateState { copy(currentStep = step.coerceIn(0 until WizardStep.TOTAL_STEPS)) }
    }

    // ──────────────── 步骤 1：选择配置文件 ────────────────
    fun selectProfile(profile: ProfileItem) {
        updateState {
            copy(selectedProfile = profile)
        }
    }

    // ──────────────── 步骤 2：选择应用 ────────────────
    fun toggleApp(packageName: String) {
        updateState {
            val newSet = selectedPackages.toMutableSet()
            if (newSet.contains(packageName)) {
                newSet.remove(packageName)
            } else {
                newSet.add(packageName)
            }
            copy(selectedPackages = newSet)
        }
    }

    fun selectAllApps() {
        updateState {
            val all = allApps.map { it.packageName }.toSet()
            copy(selectedPackages = all)
        }
    }

    fun deselectAllApps() {
        updateState { copy(selectedPackages = emptySet()) }
    }

    fun updateSearchQuery(query: String) {
        updateState { copy(searchQuery = query) }
    }

    /** 根据搜索关键字过滤后的应用列表 */
    val filteredApps: StateFlow<List<AppItem>> = uiState
        .map { state ->
            if (state.searchQuery.isBlank()) {
                state.allApps
            } else {
                state.allApps.filter { app ->
                    app.appName.contains(state.searchQuery, ignoreCase = true) ||
                        app.packageName.contains(state.searchQuery, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ──────────────── 步骤 3：配置选项 ────────────────
    fun setSkipExisting(value: Boolean) {
        updateState { copy(options = options.copy(skipExisting = value)) }
    }

    fun setAutoFreeze(value: Boolean) {
        updateState { copy(options = options.copy(autoFreeze = value)) }
    }

    fun setCopySettings(value: Boolean) {
        updateState { copy(options = options.copy(copySettings = value)) }
    }

    fun setParallelClone(value: Boolean) {
        updateState { copy(options = options.copy(parallelClone = value)) }
    }

    fun setMaxParallel(value: Int) {
        updateState {
            copy(
                options = options.copy(
                    maxParallel = value.coerceIn(1, 5)
                )
            )
        }
    }

    fun setShowProgress(value: Boolean) {
        updateState { copy(options = options.copy(showProgress = value)) }
    }

    fun setNotifyOnComplete(value: Boolean) {
        updateState { copy(options = options.copy(notifyOnComplete = value)) }
    }

    // ──────────────── 步骤 4：验证 & 开始克隆 ────────────────
    fun canProceedToReview(): Boolean {
        val state = currentState
        return state.selectedProfile != null && state.selectedPackages.isNotEmpty()
    }

    fun canStartClone(): Boolean {
        return canProceedToReview()
    }

    fun startClone() {
        val state = currentState
        val profile = state.selectedProfile ?: return
        val packages = state.selectedPackages.toList()
        if (packages.isEmpty()) return

        val config = BatchCloneConfig(
            targetProfileId = profile.id,
            targetProfileName = profile.name,
            selectedPackages = packages,
            options = state.options
        )

        // 跳转到进度步骤
        updateState {
            copy(
                currentStep = WizardStep.PROGRESS,
                isLoading = true,
                progress = BatchCloneProgress(
                    totalApps = packages.size,
                    currentStatus = CloneStatus.PREPARING,
                    startTime = System.currentTimeMillis()
                ),
                error = null
            )
        }

        cloneJob = launchWithErrorHandler {
            batchCloneUseCase(config)
                .catch { error ->
                    updateState {
                        copy(
                            isLoading = false,
                            error = error.message ?: "克隆过程中发生未知错误"
                        )
                    }
                }
                .collect { progress ->
                    updateState { copy(progress = progress) }

                    // 如果克隆完成或取消，自动跳转到摘要步骤
                    if (progress.currentStatus == CloneStatus.COMPLETED ||
                        progress.currentStatus == CloneStatus.CANCELLED
                    ) {
                        val summary = buildSummary(progress)
                        updateState {
                            copy(
                                isLoading = false,
                                currentStep = WizardStep.SUMMARY,
                                summary = summary
                            )
                        }
                    }
                }
        }
    }

    fun cancelClone() {
        cloneJob?.cancel()
        cloneJob = null

        updateState {
            val elapsed = System.currentTimeMillis() - progress.startTime
            val summary = BatchCloneSummary(
                totalApps = progress.totalApps,
                successCount = progress.completedApps,
                failedCount = progress.failedApps,
                skippedCount = 0,
                totalDurationMs = elapsed,
                results = progress.results
            )
            copy(
                isLoading = false,
                currentStep = WizardStep.SUMMARY,
                summary = summary,
                progress = progress.copy(currentStatus = CloneStatus.CANCELLED)
            )
        }
    }

    fun retryFailed() {
        val state = currentState
        val summary = state.summary ?: return
        val failedPackages = summary.results
            .filter { !it.success }
            .map { it.packageName }

        if (failedPackages.isEmpty()) return

        val profile = state.selectedProfile ?: return

        val config = BatchCloneConfig(
            targetProfileId = profile.id,
            targetProfileName = profile.name,
            selectedPackages = failedPackages,
            options = state.options
        )

        updateState {
            copy(
                currentStep = WizardStep.PROGRESS,
                isLoading = true,
                selectedPackages = failedPackages.toSet(),
                progress = BatchCloneProgress(
                    totalApps = failedPackages.size,
                    currentStatus = CloneStatus.PREPARING,
                    startTime = System.currentTimeMillis()
                ),
                error = null
            )
        }

        cloneJob = launchWithErrorHandler {
            batchCloneUseCase(config)
                .catch { error ->
                    updateState {
                        copy(
                            isLoading = false,
                            error = error.message ?: "重试过程中发生未知错误"
                        )
                    }
                }
                .collect { progress ->
                    updateState { copy(progress = progress) }

                    if (progress.currentStatus == CloneStatus.COMPLETED ||
                        progress.currentStatus == CloneStatus.CANCELLED
                    ) {
                        val newSummary = buildSummary(progress)
                        updateState {
                            copy(
                                isLoading = false,
                                currentStep = WizardStep.SUMMARY,
                                summary = newSummary
                            )
                        }
                    }
                }
        }
    }

    // ──────────────── 重置向导 ────────────────
    fun resetWizard() {
        cloneJob?.cancel()
        cloneJob = null
        updateState {
            BatchCloneWizardUiState(
                profiles = profiles,
                allApps = allApps
            )
        }
    }

    // ──────────────── 私有方法 ────────────────
    private fun buildSummary(progress: BatchCloneProgress): BatchCloneSummary {
        val successResults = progress.results.filter { it.success }
        val failedResults = progress.results.filter { !it.success }
        val elapsed = System.currentTimeMillis() - progress.startTime

        return BatchCloneSummary(
            totalApps = progress.totalApps,
            successCount = successResults.size,
            failedCount = failedResults.size,
            skippedCount = 0,
            totalDurationMs = elapsed,
            results = progress.results
        )
    }

    override fun onCleared() {
        super.onCleared()
        cloneJob?.cancel()
    }
}
