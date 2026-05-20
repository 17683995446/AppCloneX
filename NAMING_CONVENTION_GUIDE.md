# AppCloneX 四单词命名规范总结

## 命名规范要求

所有类名必须满足以下要求：
1. **至少四个单词**
2. **见名知意** - 从名称即可理解功能
3. **高度模块化** - 按功能分组

---

## 命名规范对照表

### 基础设施层 (Infrastructure)

#### persistence/database/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| AppDatabase | ApplicationDatabaseConfiguration | 4 |
| UserProfileDao | UserProfileEntityDataAccessObject | 5 |
| ClonedAppDao | ClonedApplicationDataAccessObject | 4 |

#### persistence/datastore/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| PreferencesDataStore | ApplicationPreferencesDataStoreManager | 5 |

#### persistence/sandbox/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| SandboxSharedPreferences | SandboxIsolatedSharedPreferencesImplementation | 5 |
| SandboxDataManager | ApplicationSandboxDataManagementService | 5 |

#### logging/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| ApplicationLogger | ApplicationLoggingServiceManager | 4 |

#### caching/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| ApplicationIconFetcher | ApplicationIconImageFetcherService | 5 |
| ApplicationIconCacheManager | ApplicationIconMemoryCacheManager | 5 |

#### utility/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| PinyinSearchConverter | ChinesePinyinSearchQueryConverter | 5 |
| RetryOperationHelper | OperationRetryExecutionHelper | 4 |
| ApplicationUsageTracker | ApplicationUsageStatisticsTracker | 4 |
| LanguageConfigurationManager | ApplicationLanguageConfigurationManager | 4 |
| MemoryOptimizationManager | ApplicationMemoryOptimizationManager | 4 |
| GlobalCrashExceptionHandler | GlobalApplicationCrashExceptionHandler | 4 |

#### external/shizuku/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| ShizukuServiceProvider | ShizukuServiceConnectionProvider | 4 |
| ShizukuPermissionHandler | ShizukuPermissionRequestHandler | 4 |

#### external/packagemanager/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| PackageInstallerService | ApplicationPackageInstallerService | 4 |

#### system/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| ApplicationFreezingManager | ApplicationFreezingStateManager | 4 |
| UserProfileManager | UserProfileConfigurationManager | 4 |
| ApplicationShortcutManager | ApplicationDesktopShortcutManager | 4 |

#### notification/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| CloneProgressNotificationManager | ApplicationCloneProgressNotificationManager | 5 |

---

### 领域层 (Domain)

#### model/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| ClonedApplication | ClonedApplicationInformationModel | 4 |
| UserProfileConfig | UserProfileConfigurationModel | 4 |
| ApplicationUsageStatistics | ApplicationUsageStatisticsModel | 4 |

#### repository/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| UserProfileRepository | UserProfileDataRepository | 4 |
| ClonedAppRepository | ClonedApplicationDataRepository | 4 |

---

### 表现层 (Presentation)

#### screen/home/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| HomeScreen | ApplicationHomeScreenDisplay | 4 |
| HomeViewModel | ApplicationHomeScreenViewModel | 4 |

#### screen/profile/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| UserProfileViewModel | UserProfileManagementViewModel | 4 |
| UserProfileManagementScreen | UserProfileManagementScreenDisplay | 5 |

#### screen/cloned/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| ClonedAppsViewModel | ClonedApplicationsManagementViewModel | 4 |
| ClonedAppsManagementScreen | ClonedApplicationsManagementScreenDisplay | 5 |

#### screen/freeze/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| ApplicationFreezingScreen | ApplicationFreezingScreenDisplay | 4 |
| ApplicationFreezingViewModel | ApplicationFreezingScreenViewModel | 4 |

#### screen/sandbox/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| SandboxManagementScreen | SandboxManagementScreenDisplay | 4 |
| SandboxManagementViewModel | SandboxManagementScreenViewModel | 4 |

#### screen/dashboard/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| UsageDashboardScreen | ApplicationUsageDashboardScreenDisplay | 5 |
| UsageDashboardViewModel | ApplicationUsageDashboardViewModel | 4 |

#### screen/settings/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| ApplicationSettingsScreen | ApplicationSettingsScreenDisplay | 4 |
| ApplicationSettingsViewModel | ApplicationSettingsScreenViewModel | 4 |

#### screen/detail/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| ApplicationDetailScreen | ApplicationDetailScreenDisplay | 4 |

#### screen/main/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| MainNavigationScreen | ApplicationMainNavigationScreenDisplay | 5 |
| MainNavigationViewModel | ApplicationMainNavigationViewModel | 4 |

#### screen/onboarding/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| UserOnboardingScreen | UserOnboardingScreenDisplay | 4 |

#### component/common/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| LoadingIndicator | LoadingProgressIndicatorComponent | 4 |
| ErrorMessageDisplay | ErrorMessageDisplayComponent | 4 |
| EmptyStateMessage | EmptyStateMessageDisplayComponent | 5 |

#### component/app/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| ApplicationListItem | ApplicationListItemComponent | 4 |

#### theme/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| AppCloneXTheme | AppCloneXApplicationThemeConfiguration | 4 |
| AppCloneXTypography | AppCloneXApplicationTypographyConfiguration | 4 |
| AppCloneXAnimations | AppCloneXApplicationAnimationsConfiguration | 4 |

#### activity/
| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| MainLauncherActivity | ApplicationMainLauncherActivity | 4 |
| AppCloneLauncherActivity | ApplicationCloneLauncherActivity | 4 |

---

### 应用层 (Application)

| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| AppCloneXApplication | AppCloneXAndroidApplication | 4 |
| ApplicationModule | ApplicationDependencyInjectionModule | 4 |
| AppInitializer | ApplicationStartupInitializer | 4 |

---

### 小组件和后台任务

| 原命名 | 新命名 | 单词数 |
|--------|--------|--------|
| QuickActionsAppWidget | QuickActionsDesktopAppWidget | 4 |
| QuickActionsWidgetReceiver | QuickActionsWidgetBroadcastReceiver | 4 |
| ApplicationBackgroundWorker | ApplicationBackgroundTaskWorker | 4 |

---

## 命名规范总结

### 后缀规范
| 后缀 | 用途 | 示例 |
|------|------|------|
| Manager | 管理器 | ApplicationFreezingStateManager |
| Service | 服务 | ApplicationSandboxDataManagementService |
| Repository | 仓库 | UserProfileDataRepository |
| ViewModel | 视图模型 | ApplicationHomeScreenViewModel |
| Display | 屏幕 | ApplicationHomeScreenDisplay |
| Component | 组件 | LoadingProgressIndicatorComponent |
| Configuration | 配置 | AppCloneXApplicationThemeConfiguration |
| DataAccessObject | DAO | UserProfileEntityDataAccessObject |
| BroadcastReceiver | 广播接收器 | QuickActionsWidgetBroadcastReceiver |

### 前缀规范
| 前缀 | 用途 | 示例 |
|------|------|------|
| Application | 应用级别 | ApplicationHomeScreenDisplay |
| UserProfile | 用户配置 | UserProfileConfigurationModel |
| ClonedApplication | 克隆应用 | ClonedApplicationDataRepository |
| Sandbox | 沙盒 | SandboxIsolatedSharedPreferencesImplementation |
| Shizuku | Shizuku相关 | ShizukuServiceConnectionProvider |

---

*命名规范版本: V1.0*
*更新时间: 2026-05-14*
