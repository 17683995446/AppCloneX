package com.appclonex.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.appclonex.presentation.screens.appdetail.AppDetailScreen
import com.appclonex.presentation.screens.applock.AppLockSettingsScreen
import com.appclonex.presentation.screens.apps.AppsScreen
import com.appclonex.presentation.screens.automation.AutomationRulesScreen
import com.appclonex.presentation.screens.backup.BackupScreen
import com.appclonex.presentation.screens.clone.BatchCloneWizardScreen
import com.appclonex.presentation.screens.dashboard.DashboardScreen
import com.appclonex.presentation.screens.dashboard.PerformanceRegressionScreen
import com.appclonex.presentation.screens.freeze.FreezeScreen
import com.appclonex.presentation.screens.freeze.FreezeStrategyScreen
import com.appclonex.presentation.screens.home.HomeScreen
import com.appclonex.presentation.screens.notifications.NotificationCenterScreen
import com.appclonex.presentation.screens.onboarding.OnboardingScreen
import com.appclonex.presentation.screens.settings.plugins.PluginManagerScreen
import com.appclonex.presentation.screens.settings.accessibility.AccessibilitySettingsScreen
import com.appclonex.presentation.screens.settings.language.LanguageSelectionScreen
import com.appclonex.presentation.screens.profile.ProfileScreen
import com.appclonex.presentation.screens.sandbox.SandboxScreen
import com.appclonex.presentation.screens.settings.SettingsScreen
import com.appclonex.presentation.screens.statistics.UsageStatisticsScreen
import com.appclonex.presentation.screens.template.TemplateMarketplaceScreen
import com.appclonex.presentation.screen.main.ApplicationMainNavigationViewModel

/**
 * 导航目标
 */
sealed class NavTarget(val route: String) {
    data object Onboarding : NavTarget("onboarding")
    data object Home : NavTarget("home")
    data object Apps : NavTarget("apps")
    data object Profile : NavTarget("profile")
    data object Settings : NavTarget("settings")
    data object Freeze : NavTarget("freeze")
    data object Sandbox : NavTarget("sandbox")
    data object Dashboard : NavTarget("dashboard")
    data object AppLock : NavTarget("app_lock")
    data object Automation : NavTarget("automation")
    data object Backup : NavTarget("backup")
    data object BatchClone : NavTarget("batch_clone")
    data object FreezeStrategy : NavTarget("freeze_strategy")
    data object UsageStatistics : NavTarget("usage_statistics")
    data object TemplateMarketplace : NavTarget("template_marketplace")
    data object PerformanceRegression : NavTarget("performance_regression")
    data object PluginManager : NavTarget("plugin_manager")
    data object NotificationCenter : NavTarget("notification_center")
    data object AccessibilitySettings : NavTarget("accessibility_settings")
    data object LanguageSelection : NavTarget("language_selection")

    data object AppDetail : NavTarget("app_detail/{packageName}/{userId}") {
        fun createRoute(packageName: String, userId: Int = 0) =
            "app_detail/$packageName/$userId"
    }
}

/**
 * 应用导航主机
 */
@Composable
fun AppCloneXNavHost(
    navController: NavHostController = rememberNavController(),
    mainViewModel: ApplicationMainNavigationViewModel = hiltViewModel()
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

    // 根据引导页状态决定起始目的地
    val startDestination = if (uiState.showOnboarding) {
        NavTarget.Onboarding.route
    } else {
        NavTarget.Home.route
    }

    LaunchedEffect(uiState.showOnboarding) {
        if (!uiState.showOnboarding && navController.currentDestination?.route == NavTarget.Onboarding.route) {
            navController.navigate(NavTarget.Home.route) {
                popUpTo(NavTarget.Onboarding.route) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) +
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) +
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) +
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        // 引导页
        composable(NavTarget.Onboarding.route) {
            OnboardingScreen(
                shizukuConnected = uiState.shizukuState ==
                    com.appclonex.domain.model.ShizukuConnectionState.CONNECTED,
                onComplete = {
                    mainViewModel.completeOnboarding()
                },
                onSkip = {
                    mainViewModel.completeOnboarding()
                }
            )
        }

        // 首页
        composable(NavTarget.Home.route) {
            HomeScreen(
                onNavigateToApps = { navController.navigate(NavTarget.Apps.route) },
                onNavigateToSettings = { navController.navigate(NavTarget.Settings.route) },
                onNavigateToFreeze = { navController.navigate(NavTarget.Freeze.route) },
                onNavigateToSandbox = { navController.navigate(NavTarget.Sandbox.route) },
                onNavigateToDashboard = { navController.navigate(NavTarget.Dashboard.route) },
                onNavigateToProfile = { navController.navigate(NavTarget.Profile.route) },
                onNavigateToAppLock = { navController.navigate(NavTarget.AppLock.route) },
                onNavigateToAutomation = { navController.navigate(NavTarget.Automation.route) },
                onNavigateToBackup = { navController.navigate(NavTarget.Backup.route) },
                onNavigateToBatchClone = { navController.navigate(NavTarget.BatchClone.route) },
                onNavigateToFreezeStrategy = { navController.navigate(NavTarget.FreezeStrategy.route) },
                onNavigateToUsageStatistics = { navController.navigate(NavTarget.UsageStatistics.route) },
                onNavigateToTemplateMarketplace = { navController.navigate(NavTarget.TemplateMarketplace.route) },
                onNavigateToPerformanceRegression = { navController.navigate(NavTarget.PerformanceRegression.route) },
                onNavigateToPluginManager = { navController.navigate(NavTarget.PluginManager.route) },
                onNavigateToNotificationCenter = { navController.navigate(NavTarget.NotificationCenter.route) }
            )
        }

        // 克隆应用管理
        composable(NavTarget.Apps.route) {
            AppsScreen(
                onNavigateBack = { navController.popBackStack() },
                onAppClick = { packageName, userId ->
                    navController.navigate(NavTarget.AppDetail.createRoute(packageName, userId))
                }
            )
        }

        // 应用详情
        composable(
            route = NavTarget.AppDetail.route,
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
                navArgument("userId") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            val userId = backStackEntry.arguments?.getInt("userId") ?: 0
            AppDetailScreen(
                packageName = packageName,
                userId = userId,
                onNavigateBack = { navController.popBackStack() },
                onDeleteSuccess = { navController.popBackStack() }
            )
        }

        // 用户配置文件
        composable(NavTarget.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 设置
        composable(NavTarget.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 应用冻结
        composable(NavTarget.Freeze.route) {
            FreezeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 沙盒管理
        composable(NavTarget.Sandbox.route) {
            SandboxScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 使用统计仪表盘
        composable(NavTarget.Dashboard.route) {
            DashboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 应用锁设置
        composable(NavTarget.AppLock.route) {
            AppLockSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 自动化规则
        composable(NavTarget.Automation.route) {
            AutomationRulesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 备份与恢复
        composable(NavTarget.Backup.route) {
            BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 批量克隆向导
        composable(NavTarget.BatchClone.route) {
            BatchCloneWizardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 智能冻结策略
        composable(NavTarget.FreezeStrategy.route) {
            FreezeStrategyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 使用统计
        composable(NavTarget.UsageStatistics.route) {
            UsageStatisticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 模板市场
        composable(NavTarget.TemplateMarketplace.route) {
            TemplateMarketplaceScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 性能回归检测
        composable(NavTarget.PerformanceRegression.route) {
            PerformanceRegressionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 插件管理
        composable(NavTarget.PluginManager.route) {
            PluginManagerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 通知中心
        composable(NavTarget.NotificationCenter.route) {
            NotificationCenterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 无障碍设置
        composable(NavTarget.AccessibilitySettings.route) {
            AccessibilitySettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 语言选择
        composable(NavTarget.LanguageSelection.route) {
            LanguageSelectionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
