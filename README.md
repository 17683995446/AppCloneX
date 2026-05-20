# AppCloneX - 安卓应用分身工具

一个基于Android多用户机制的应用分身工具，支持**无上限分身**创建。

## 功能特点

- ✅ **无上限分身**：基于Android多用户机制，可创建无限数量的用户配置文件
- ✅ **无需Root**：通过Shizuku获取系统权限，无需Root设备
- ✅ **独立运行**：每个分身在独立的用户空间运行，数据完全隔离
- ✅ **批量操作**：支持批量克隆应用到多个配置文件
- ✅ **现代UI**：使用Jetpack Compose构建Material Design 3界面
- ✅ **崩溃报告与监控**：集成Firebase Crashlytics和Analytics
- ✅ **性能优化**：自适应缓存、数据库分页、内存优化
- ✅ **Clean Architecture**：多模块架构，便于维护和测试

## 技术架构

### 核心技术

1. **Shizuku API**：获取系统级权限而无需Root
2. **Android多用户机制**：创建独立的用户空间实现应用分身
3. **反射调用隐藏API**：通过反射调用UserManager和PackageManager的隐藏方法

## 项目架构

AppCloneX 采用 Clean Architecture 多模块架构，分为四个模块：

```
AppCloneX/
├── app/                    # 应用入口模块
│   ├── application/        # Application类和初始化
│   ├── widget/             # 桌面小组件
│   └── worker/             # WorkManager后台任务
│
├── domain/                 # 领域层（纯Kotlin，无Android依赖）
│   ├── model/              # 领域模型（数据类、枚举）
│   ├── repository/         # 仓库接口（抽象）
│   └── usecase/            # 用例（业务逻辑）
│       ├── clone/          # 克隆相关用例
│       ├── profile/        # 配置文件用例
│       └── monitoring/     # 监控用例
│
├── infrastructure/         # 基础设施层
│   ├── cache/              # 数据缓存
│   ├── caching/            # 图标缓存（Coil）
│   ├── di/                 # Hilt依赖注入模块
│   ├── external/           # 外部服务（Shizuku、包管理器）
│   ├── logging/            # 日志服务（Timber、Firebase）
│   ├── network/            # 网络配置（Retrofit）
│   ├── notification/       # 通知管理
│   ├── paging/             # 分页数据源
│   ├── persistence/        # 数据持久化
│   │   ├── database/       # Room数据库
│   │   ├── datastore/      # DataStore偏好
│   │   └── sandbox/        # 沙盒数据管理
│   ├── repository/         # 仓库实现
│   ├── system/             # 系统服务（冻结、快捷方式）
│   └── utility/            # 工具类
│
└── presentation/           # 表现层
    ├── base/               # BaseViewModel/BaseUiState
    ├── di/                 # Presentation层DI
    ├── navigation/         # Compose导航
    ├── screen/             # 屏幕ViewModel
    ├── screens/            # Compose UI组件
    └── theme/              # Material3主题
```

### 架构原则

1. **依赖规则**: 依赖只能向内指向，presentation → domain ← infrastructure
2. **接口隔离**: Domain层定义接口，Infrastructure层提供实现
3. **单一职责**: 每个模块只负责特定功能
4. **可测试性**: Domain层纯Kotlin，便于单元测试

### 技术栈

- **UI**: Jetpack Compose + Material3
- **DI**: Hilt
- **数据库**: Room
- **异步**: Kotlin Coroutines + Flow
- **网络**: Retrofit + OkHttp
- **图片**: Coil
- **监控**: Firebase Crashlytics + Analytics

## 使用方法

### 前置条件

1. **安装Shizuku**：从GitHub或Play商店下载安装Shizuku应用
2. **启动Shizuku服务**：打开Shizuku应用并启动服务
3. **授权AppCloneX**：首次打开AppCloneX时授权Shizuku权限

### 操作步骤

1. **创建配置文件**
   - 点击右下角"+"按钮
   - 输入配置文件名称（如"工作"、"游戏"等）
   - 点击"创建"

2. **克隆应用**
   - 切换到"应用"页面
   - 选择要克隆的应用
   - 点击"克隆到..."
   - 选择目标配置文件

3. **启动分身**
   - 在"配置文件"页面启动目标配置文件
   - 进入配置文件查看分身应用
   - 点击启动按钮运行分身

## 技术原理

### 多用户机制

Android系统原生支持多用户功能，每个用户拥有：
- 独立的应用安装空间
- 独立的应用数据存储
- 独立的账户系统

通过创建多个用户配置文件，可以在同一设备上运行同一应用的多个实例。

### Shizuku权限

Shizuku是一个无需Root即可获取系统权限的框架：
- 通过ADB授权获取系统级权限
- 提供Binder接口访问系统服务
- 支持调用隐藏API

### 隐藏API调用

通过反射调用以下隐藏API：

```kotlin
// UserManager
createUser(String name, String userType)  // 创建用户
removeUser(int userId)                     // 删除用户
getUsers()                                 // 获取用户列表

// PackageManager
installPackage(String path, observer, flags, installer, userId)  // 安装应用
deletePackage(String packageName, observer, userId)              // 卸载应用
```

## 限制说明

1. **设备限制**：部分厂商ROM可能限制多用户功能
2. **存储空间**：每个用户空间占用额外存储
3. **性能影响**：大量用户可能影响系统性能
4. **应用兼容**：部分应用可能检测多用户环境

## 编译说明

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.2

### 编译步骤

```bash
# 克隆项目
git clone https://github.com/your-repo/AppCloneX.git

# 进入项目目录
cd AppCloneX

# 编译Debug版本
./gradlew assembleDebug

# 编译Release版本
./gradlew assembleRelease
```

## 依赖库

- [Shizuku API](https://github.com/RikkaApps/Shizuku-API) - 系统权限框架
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代UI框架
- [Room](https://developer.android.com/training/data-storage/room) - 数据库
- [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) - 异步处理

## 免责声明

本应用仅供学习和研究目的使用。使用本应用创建的应用分身可能违反某些应用的服务条款。请确保您有权限在多个账户中使用相关应用。开发者不对因使用本应用而产生的任何问题负责。

## 许可证

MIT License

Copyright (c) 2025 AppCloneX

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
