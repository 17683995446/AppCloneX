# AppCloneX APK 编译指南

## 环境要求

### 必需软件

1. **JDK 17** 或更高版本
2. **Android SDK** (API 34)
3. **Gradle 8.2+**
4. **Android Studio** (推荐) 或命令行工具

### 安装步骤

#### 1. 安装 JDK 17

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# macOS
brew install openjdk@17

# 验证安装
java -version
```

#### 2. 安装 Android SDK

**方式一：通过 Android Studio 安装（推荐）**

1. 下载并安装 [Android Studio](https://developer.android.com/studio)
2. 打开 Android Studio，安装 SDK Platform 34
3. 记录 SDK 安装路径（通常是 `~/Android/Sdk` 或 `C:\Users\<用户名>\AppData\Local\Android\Sdk`）

**方式二：命令行安装**

```bash
# 下载命令行工具
mkdir -p ~/Android/Sdk/cmdline-tools
cd ~/Android/Sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest

# 设置环境变量
export ANDROID_HOME=~/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

# 安装 SDK 组件
sdkmanager "platforms;android-34"
sdkmanager "build-tools;34.0.0"
sdkmanager "platform-tools"
```

#### 3. 配置环境变量

将以下内容添加到 `~/.bashrc` 或 `~/.zshrc`：

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64  # 根据实际路径修改
export ANDROID_HOME=~/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

然后执行：

```bash
source ~/.bashrc  # 或 source ~/.zshrc
```

## 编译 APK

### 方法一：使用 Android Studio（推荐）

1. 打开 Android Studio
2. 选择 `File` → `Open`，选择 `AppCloneX` 项目文件夹
3. 等待 Gradle 同步完成
4. 选择 `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
5. 编译完成后，APK 位于 `app/build/outputs/apk/debug/app-debug.apk`

### 方法二：使用命令行

```bash
# 进入项目目录
cd AppCloneX

# 赋予构建脚本执行权限
chmod +x build_apk.sh

# 运行构建脚本
./build_apk.sh
```

或直接使用 Gradle：

```bash
# 编译 Debug APK
./gradlew assembleDebug

# 编译 Release APK（需要签名配置）
./gradlew assembleRelease
```

编译完成后，APK 文件位于：
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

## 签名 APK（Release 版本）

Release APK 需要签名才能安装：

```bash
# 生成签名密钥
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias

# 签名 APK
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore my-release-key.jks app/build/outputs/apk/release/app-release-unsigned.apk my-alias

# 优化 APK
zipalign -v 4 app/build/outputs/apk/release/app-release-unsigned.apk AppCloneX-release.apk
```

## 常见问题

### 1. Gradle 下载失败

如果 Gradle wrapper 下载超时，可以手动下载并配置：

```bash
# 下载 Gradle 8.5
wget https://services.gradle.org/distributions/gradle-8.5-bin.zip
unzip gradle-8.5-bin.zip -d ~/gradle

# 使用本地 Gradle
export PATH=~/gradle/gradle-8.5/bin:$PATH

# 然后使用 gradle 命令代替 ./gradlew
gradle assembleDebug
```

### 2. SDK 组件缺失

```bash
# 列出可用的 SDK 组件
sdkmanager --list

# 安装缺失的组件
sdkmanager "platforms;android-34"
sdkmanager "build-tools;34.0.0"
```

### 3. 编译内存不足

在 `gradle.properties` 中增加内存配置：

```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

### 4. 依赖下载失败

检查网络连接，或配置镜像源：

```kotlin
// 在 settings.gradle.kts 中添加阿里云镜像
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
    }
}
```

## 安装 APK

### 通过 ADB 安装

```bash
# 连接设备
adb devices

# 安装 Debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 覆盖安装
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 直接安装

将 APK 文件传输到 Android 设备，点击安装即可。

**注意**：首次安装需要允许安装未知来源应用。

## 验证安装

1. 安装完成后，在应用列表中找到 "AppCloneX"
2. 打开应用，检查 Shizuku 状态
3. 按照应用内指引配置 Shizuku
4. 开始创建配置文件和克隆应用

## 项目文件清单

编译完成后，项目结构如下：

```
AppCloneX/
├── app/
│   ├── build/
│   │   └── outputs/
│   │       └── apk/
│   │           ├── debug/
│   │           │   └── app-debug.apk      ← Debug APK
│   │           └── release/
│   │               └── app-release-unsigned.apk  ← Release APK（未签名）
│   └── src/...
├── build_apk.sh          ← 构建脚本
├── BUILD_GUIDE.md        ← 本文件
└── README.md             ← 项目说明
```

## 技术支持

如有编译问题，请检查：
1. Java 版本是否为 17+
2. Android SDK 是否完整安装
3. 环境变量是否正确配置
4. 网络连接是否正常（需要下载依赖）
