# GitHub Actions 自动构建指南

## 快速开始（3步完成）

### 第1步：在 GitHub 创建仓库

1. 打开 [github.com/new](https://github.com/new)
2. 仓库名称：`AppCloneX`
3. 选择 **Public**（公开仓库可使用免费Actions）
4. **不要**勾选 README / .gitignore / License（已有）
5. 点击 **Create repository**

### 第2步：推送代码到 GitHub

在本地终端执行以下命令（替换 `YOUR_USERNAME`）：

```bash
cd /workspace/AppCloneX

# 初始化 Git
git init
git add -A
git commit -m "feat: AppCloneX 应用分身工具"

# 关联远程仓库并推送
git remote add origin https://github.com/YOUR_USERNAME/AppCloneX.git
git branch -M main
git push -u origin main
```

### 第3步：下载 APK

推送完成后，GitHub Actions 会自动开始构建：

1. 打开仓库页面 → **Actions** 标签
2. 等待 "Build APK" 工作流完成（约 5-10 分钟）
3. 点击成功的构建 → 底部 **Artifacts** 区域
4. 下载 `AppCloneX-debug.zip`，解压即可得到 APK

---

## 手动触发构建

除了自动构建，你也可以随时手动触发：

1. 仓库页面 → **Actions** 标签
2. 左侧选择 **Build APK**
3. 点击 **Run workflow** → **Run workflow**

---

## Release 签名构建（可选）

如需构建已签名的 Release APK：

### 1. 生成签名密钥

```bash
keytool -genkey -v -keystore release.keystore \
    -alias appclonex \
    -keyalg RSA -keysize 2048 -validity 10000
```

### 2. 配置 GitHub Secrets

1. 仓库 → **Settings** → **Secrets and variables** → **Actions**
2. 点击 **New repository secret**
3. 添加以下密钥：

| Secret 名称 | 值 |
|-------------|-----|
| `KEYSTORE_BASE64` | `base64 -w 0 release.keystore` 的输出 |
| `KEYSTORE_PASSWORD` | 密钥库密码 |
| `KEY_ALIAS` | 密钥别名（如 `appclonex`） |
| `KEY_PASSWORD` | 密钥密码 |

### 3. 修改 app/build.gradle.kts 添加签名配置

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

---

## 工作流说明

```
推送代码 → GitHub Actions 自动触发
    │
    ├── build（Debug构建）
    │     ├── 配置 JDK 17
    │     ├── 缓存 Gradle 依赖
    │     ├── ./gradlew assembleDebug
    │     └── 上传 APK 到 Artifacts
    │
    └── build-release（Release构建，Debug成功后执行）
          ├── 配置 JDK 17
          ├── 缓存 Gradle 依赖
          ├── ./gradlew assembleRelease
          └── 上传 APK 到 Artifacts
```

---

## 常见问题

### Q: Actions 构建失败怎么办？
- 检查 Actions 页面的错误日志
- 确保所有 `.kt` 源文件无语法错误
- 确认 `build.gradle.kts` 中依赖版本兼容

### Q: 如何加速构建？
- 已配置 Gradle 缓存，第二次构建会快很多
- 仅修改代码时自动触发，修改 `.md` 文件不会触发

### Q: 私有仓库能用吗？
- GitHub 免费账户每月有 2000 分钟 Actions 时间
- 私有仓库也可用，但会消耗免费额度
