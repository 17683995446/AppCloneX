# AppCloneX 架构重构计划

## 当前问题

### 1. 命名不够见名知意
- `AppFreezer` -> 应该更明确是应用冻结管理
- `MultiUserManager` -> 应该明确是用户配置管理
- `SandboxManager` -> 应该明确是数据隔离管理

### 2. 层级不够清晰
- core/ 混合了不同职责的模块
- utils/ 过于臃肿
- data/ 模型和仓库混在一起

### 3. 缺少明确的分层
- 缺少 domain 层（业务逻辑）
- 缺少 infrastructure 层（基础设施）
- presentation 层职责不够清晰

## 新的架构设计

```
com.appclonex/
├── application/                    # 应用层
│   ├── AppCloneXApplication.kt     # 应用入口
│   └── di/                         # 依赖注入配置
│       ├── ApplicationModule.kt
│       └── DatabaseModule.kt
│
├── domain/                         # 领域层（业务逻辑）
│   ├── model/                      # 领域模型
│   │   ├── ClonedApplication.kt    # 克隆应用
│   │   ├── UserProfileConfig.kt    # 用户配置
│   │   ├── ApplicationSandbox.kt   # 应用沙盒
│   │   └── UsageStatistics.kt      # 使用统计
│   │
│   ├── repository/                 # 仓库接口
│   │   ├── ClonedAppRepository.kt
│   │   ├── UserProfileRepository.kt
│   │   └── SandboxDataRepository.kt
│   │
│   └── usecase/                    # 用例（业务逻辑）
│       ├── clone/
│       │   ├── CloneApplicationUseCase.kt
│       │   ├── UninstallClonedAppUseCase.kt
│       │   └── GetClonedAppsUseCase.kt
│       ├── freeze/
│       │   ├── FreezeApplicationUseCase.kt
│       │   ├── UnfreezeApplicationUseCase.kt
│       │   └── GetFrozenAppsUseCase.kt
│       ├── sandbox/
│       │   ├── CreateSandboxUseCase.kt
│       │   ├── ClearSandboxUseCase.kt
│       │   ├── ExportSandboxUseCase.kt
│       │   └── ImportSandboxUseCase.kt
│       └── profile/
│           ├── CreateUserProfileUseCase.kt
│           ├── UpdateUserProfileUseCase.kt
│           └── DeleteUserProfileUseCase.kt
│
├── infrastructure/                 # 基础设施层
│   ├── persistence/                # 数据持久化
│   │   ├── database/
│   │   │   ├── AppDatabase.kt
│   │   │   ├── UserProfileDao.kt
│   │   │   └── ClonedAppDao.kt
│   │   ├── datastore/
│   │   │   └── PreferencesDataStore.kt
│   │   └── sandbox/
│   │       ├── SandboxDataManager.kt          (原SandboxManager)
│   │       └── SandboxSharedPreferences.kt
│   │
│   ├── external/                   # 外部服务
│   │   ├── shizuku/
│   │   │   ├── ShizukuServiceProvider.kt      (原ShizukuServiceManager)
│   │   │   └── ShizukuPermissionHandler.kt
│   │   └── packagemanager/
│   │       └── PackageInstallerService.kt     (原AppInstallManager)
│   │
│   ├── system/                     # 系统服务
│   │   ├── ApplicationFreezingManager.kt      (原AppFreezer)
│   │   ├── UserProfileManager.kt              (原MultiUserManager)
│   │   └── ApplicationShortcutManager.kt      (原AppShortcutHelper)
│   │
│   ├── notification/               # 通知
│   │   └── CloneProgressNotificationManager.kt
│   │
│   ├── logging/                    # 日志
│   │   └── ApplicationLogger.kt               (原LogManager)
│   │
│   ├── caching/                    # 缓存
│   │   ├── ApplicationIconCacheManager.kt     (原AppIconCache)
│   │   └── ApplicationIconFetcher.kt
│   │
│   └── utility/                    # 工具
│       ├── PinyinSearchConverter.kt           (原PinyinUtils)
│       ├── RetryOperationHelper.kt            (原RetryHelper)
│       ├── ApplicationUsageTracker.kt         (原UsageTracker)
│       ├── LanguageConfigurationManager.kt    (原LanguageManager)
│       ├── MemoryOptimizationManager.kt       (原MemoryManager)
│       └── GlobalCrashExceptionHandler.kt     (原CrashHandler)
│
├── presentation/                   # 表现层
│   ├── activity/                   # Activity
│   │   ├── MainLauncherActivity.kt            (原MainActivity)
│   │   └── AppCloneLauncherActivity.kt        (原LauncherActivity)
│   │
│   ├── screen/                     # 屏幕/页面
│   │   ├── home/
│   │   │   ├── HomeScreen.kt                  (原AppsScreen)
│   │   │   └── HomeViewModel.kt               (原AppsViewModel)
│   │   ├── cloned/
│   │   │   ├── ClonedAppsManagementScreen.kt  (原ClonedAppsScreen)
│   │   │   └── ClonedAppsViewModel.kt
│   │   ├── profile/
│   │   │   ├── UserProfileManagementScreen.kt (原ProfilesScreen)
│   │   │   └── UserProfileViewModel.kt
│   │   ├── freeze/
│   │   │   ├── ApplicationFreezingScreen.kt   (原FreezerScreen)
│   │   │   └── ApplicationFreezingViewModel.kt
│   │   ├── sandbox/
│   │   │   ├── SandboxManagementScreen.kt     (原SandboxScreen)
│   │   │   └── SandboxManagementViewModel.kt
│   │   ├── dashboard/
│   │   │   ├── UsageDashboardScreen.kt        (原DashboardScreen)
│   │   │   └── UsageDashboardViewModel.kt
│   │   ├── settings/
│   │   │   ├── ApplicationSettingsScreen.kt   (原SettingsScreen)
│   │   │   └── ApplicationSettingsViewModel.kt
│   │   ├── detail/
│   │   │   ├── ApplicationDetailScreen.kt     (原AppDetailScreen)
│   │   │   └── ApplicationDetailViewModel.kt
│   │   ├── main/
│   │   │   └── MainNavigationScreen.kt        (原MainScreen)
│   │   └── onboarding/
│   │       └── UserOnboardingScreen.kt        (原OnboardingScreen)
│   │
│   ├── component/                  # 可复用组件
│   │   ├── common/
│   │   │   ├── LoadingIndicator.kt
│   │   │   ├── ErrorMessageDisplay.kt
│   │   │   └── EmptyStateMessage.kt
│   │   ├── app/
│   │   │   ├── ApplicationListItem.kt
│   │   │   ├── ApplicationIconImage.kt
│   │   │   └── ApplicationStatusBadge.kt
│   │   └── dialog/
│   │       ├── ConfirmActionDialog.kt
│   │       └── ProgressIndicatorDialog.kt
│   │
│   ├── theme/                      # 主题
│   │   ├── AppCloneXColorScheme.kt
│   │   ├── AppCloneXTypography.kt
│   │   ├── AppCloneXShapeTheme.kt
│   │   ├── AppCloneXTheme.kt
│   │   └── AppCloneXAnimations.kt
│   │
│   └── navigation/                 # 导航
│       ├── AppNavigationRoutes.kt
│       └── AppNavigationGraph.kt
│
├── widget/                         # 桌面小组件
│   ├── QuickActionsAppWidget.kt               (原QuickActionsWidget)
│   ├── QuickActionsWidgetReceiver.kt
│   └── WidgetActionCallback.kt
│
└── worker/                         # 后台任务
    └── ApplicationBackgroundWorker.kt           (原WidgetActionWorker)
```

## 命名规范

### 1. 类名规范
- **Manager**: 管理某类资源或功能的管理器
- **Service**: 提供某种服务的类
- **Provider**: 提供数据或依赖的类
- **Handler**: 处理特定事件或逻辑的类
- **Helper**: 辅助工具类
- **Converter**: 数据转换类
- **Tracker**: 追踪统计类

### 2. 包名规范
- 使用复数形式表示集合（screens, components, utils）
- 使用动词名词组合表示动作（cloning, freezing, tracking）
- 按功能分组，不按类型分组

## 实施步骤

1. 创建新的目录结构
2. 逐个模块迁移，保持功能不变
3. 更新所有导入语句
4. 验证编译通过
5. 运行测试确保功能正常
