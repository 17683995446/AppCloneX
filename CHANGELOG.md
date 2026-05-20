# 变更日志 Changelog

所有重要变更都将记录在此文件中。

All notable changes to this project will be documented in this file.

格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

## [2.5.0] - 2026-05-17

### Added
- WebDAV/Nextcloud 云备份真实实现（RFC 4918 协议 + OCS API）
- 插件系统架构（安装/卸载/启用/禁用/权限管理/配置）
- 智能通知中心（分类/优先级/免打扰/分组/搜索/滑动操作）
- 雷达图和散点图组件（纯 Canvas 实现）
- 模板市场（分享/导入/评分/搜索/分类筛选）
- 性能回归自动检测（基准测试/趋势分析/阈值告警）
- Google Drive 云备份集成（REST API v3 + 断点续传）
- 105+ 单元测试（V25 新增 105 个测试用例）

### Improved
- HomeScreen 三区分组布局（核心/高级/工具）
- ProGuard 规则完善（WebDAV/Plugin/Notification/Serialization）
- 国际化翻译完善（英文/简中/繁中 V25 新增功能）
- 无障碍描述生成器
- KtLint + Detekt 双静态分析 CI 配置

### Fixed
- Kotlin 版本统一为 1.9.20
- Compose Compiler 版本修复为 1.5.10
- KSP 版本匹配 Kotlin 版本
- Google Play Services 插件定义补充

---

## [2.4.0] - 2025-05-16

### 🎉 V24 发布 - 云备份·模板市场·性能检测

### 新增 Added
- ☁️ Google Drive 云备份集成（REST API v3，断点续传，冲突检测）
- 🏪 克隆模板市场（分享/导入/评分/搜索/分类筛选）
- 📊 性能回归自动检测（基准测试+历史趋势+阈值告警）
- 📈 雷达图组件（纯 Canvas，多组数据叠加）
- 🔵 散点图组件（纯 Canvas，趋势线+点击交互）
- 🎨 AdaptiveChart 自适应图表组件
- 📋 ChartTooltip 通用悬浮提示
- ⚡ ChartLoadingIndicator 脉冲加载动画
- 🏠 首页重构（核心/高级/工具三区分组，14个功能入口）
- 📊 首页统计摘要扩展（4项网格+图标）
- 🔔 首页告警提示区域
- 📝 KtLint 静态分析配置（.editorconfig + ktlint.yml）
- 🔄 CI 集成 KtLint 格式化验证

### 改进 Improved
- 🏠 HomeScreen 全面重构：三区分组布局，支持角标和高亮
- 🧭 Navigation.kt 新增 TemplateMarketplace + PerformanceRegression 路由
- 🌐 strings.xml 新增 24 个 V24 功能字符串资源
- 📦 版本号更新: versionCode=24, versionName="2.4.0"

### 测试 Testing
- 🧪 V24ModelTests: 30 个模型测试（Google Drive + 性能回归 + 模板市场）
- 🧪 ChartComponentTest: 20 个图表组件测试
- 🧪 总计 50+ 新增测试用例

---

## [2.3.0] - 2025-05-16

### 🎉 V23 发布 - 图表增强·基准测试·无障碍

### 新增 Added
- 📊 纯 Canvas 柱状图（BarChart）
- 🥧 纯 Canvas 饼图（PieChart）
- 📈 纯 Canvas 折线图（LineChart）
- 🎨 ChartComponents 通用组件（颜色/图例/空状态）
- ⚡ 性能基准测试框架（冷启动/内存/数据库，6级评分）
- ♿ 无障碍设置（字体缩放/高对比/粗体/减少动画/触摸目标）
- 📋 Detekt 静态分析配置（451行，6大规则类别）
- 🧪 V23ModelTests: 20 个模型测试

---

## [2.2.0] - 2025-05-16

### 🎉 V22 发布 - 备份恢复·批量克隆·智能冻结

### 新增 Added
- 💾 备份恢复系统（Domain + Infrastructure + Presentation）
- 📦 批量克隆向导（6步引导流程）
- ❄️ 智能冻结策略（8种触发类型+8种条件类型）
- 📊 使用统计分析（日/周/月/趋势）
- 🌙 DynamicTheme Material You 深色模式
- 🧪 BackupRepositoryTest: 13 个备份测试

---

## [2.1.0] - 2025-05-16

### 🎉 V21 发布 - Google Play 准备版本

### 新增 Added
- ✅ Google Play 发布准备（签名配置、App Bundle、隐私政策）
- ✅ 贡献指南 (CONTRIBUTING.md) - 中英双语
- ✅ 隐私政策 (PRIVACY_POLICY.md) - 符合 GDPR/CCPA/PIPL
- ✅ 数据提取规则（禁止云端备份敏感数据）
- ✅ 应用签名配置（支持环境变量注入）
- ✅ 变更日志 (CHANGELOG.md)

### 安全 Security
- 🔒 禁用 AndroidManifest.xml allowBackup
- 🔒 添加 usesCleartextTraffic="false"
- 🔒 数据提取规则排除所有敏感数据
- 🔒 Release 构建启用代码混淆

### 构建 Build
- 📦 版本号更新: versionCode=21, versionName="2.1.0"
- 📦 App Bundle 配置（语言/密度/ABI 分包）
- 📦 Debug 构建添加 applicationIdSuffix

---

## [2.0.0] - 2025-05-16

### 🎉 V20 发布 - 动画与性能优化版本

### 新增 Added
- 🎨 Coil 图片加载库集成（替代手写 AppIconCache）
- 🎨 Compose 导航过渡动画（淡入淡出+滑动）
- 🎨 列表项动画（animateItemPlacement）
- 🎨 PermissionNameMapper（24种权限中文映射）
- 🎨 StorageCleanupWorker 多配置文件支持
- 🎨 异常日志记录（静默 catch 块）

### 改进 Improved
- ⚡ 应用图标加载性能提升
- ⚡ 页面切换流畅度优化
- ⚡ 列表滚动性能优化

### 测试 Testing
- 🧪 新增 25 个单元测试
- 🧪 ViewModel 测试覆盖率提升
- 🧪 Repository 测试覆盖核心路径

---

## [1.0.0] - 2024-XX-XX

### 🎉 初始版本 Initial Release

### 功能 Features
- 📱 基于 Android 多用户机制的应用分身
- 📱 Shizuku 权限集成（无需 Root）
- 📱 Clean Architecture 多模块架构
- 📱 Jetpack Compose + Material 3 UI
- 📱 Room 数据库 + DataStore 偏好设置
- 📱 Hilt 依赖注入
- 📱 Firebase Crashlytics + Analytics
- 📱 应用锁（生物识别 + PIN）
- 📱 桌面小组件（Glance）
- 📱 WorkManager 后台任务

---

## 版本说明 Version Legend

- **Added**: 新功能 New features
- **Changed**: 变更 Changes
- **Deprecated**: 弃用 Deprecated
- **Removed**: 移除 Removed
- **Fixed**: 修复 Bug fixes
- **Security**: 安全 Security improvements
