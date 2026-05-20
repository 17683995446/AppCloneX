# 隐私政策 Privacy Policy

**生效日期 Effective Date**: 2025年5月16日  
**最后更新 Last Updated**: 2025年5月16日

---

## 1. 引言 Introduction

AppCloneX（"我们"、"我们的"或"本应用"）重视用户的隐私保护。本隐私政策旨在说明我们如何收集、使用、存储和保护您的个人信息。

AppCloneX ("we", "our", or "the App") values user privacy protection. This Privacy Policy explains how we collect, use, store, and protect your personal information.

## 2. 信息收集 Information Collection

### 2.1 我们收集的信息 Information We Collect

**应用数据 App Data:**
- 已安装应用列表（仅用于显示可克隆应用）
- Installed app list (only for displaying clonable apps)
- 用户配置文件名称（由用户自定义）
- User profile names (customized by users)
- 应用克隆偏好设置
- App cloning preferences

**设备信息 Device Information:**
- Android 版本和 SDK 级别
- Android version and SDK level
- 设备型号和制造商
- Device model and manufacturer
- 屏幕分辨率和密度
- Screen resolution and density

**诊断信息 Diagnostic Information:**
- 应用崩溃日志（通过 Firebase Crashlytics）
- App crash logs (via Firebase Crashlytics)
- 匿名使用统计（通过 Firebase Analytics）
- Anonymous usage statistics (via Firebase Analytics)
- 性能指标（启动时间、内存使用）
- Performance metrics (launch time, memory usage)

### 2.2 我们不收集的信息 Information We Do NOT Collect

- **个人身份信息**: 姓名、电子邮件、电话号码
  **Personal Identification**: Name, email, phone number
- **敏感数据**: 银行账户、密码、聊天记录
  **Sensitive Data**: Bank accounts, passwords, chat records
- **应用内容**: 分身应用内的数据
  **App Content**: Data within cloned apps
- **位置信息**: GPS 位置数据
  **Location Data**: GPS location data

## 3. 信息使用 Information Usage

### 3.1 使用目的 Usage Purposes

| 目的 Purpose | 说明 Description |
|-------------|-----------------|
| 应用功能 App Functionality | 实现应用克隆和分身管理功能 |
| | Implement app cloning and profile management |
| 性能优化 Performance Optimization | 分析性能瓶颈，优化用户体验 |
| | Analyze performance bottlenecks, optimize UX |
| 错误修复 Bug Fixes | 收集崩溃信息以修复问题 |
| | Collect crash info to fix issues |
| 统计分析 Statistical Analysis | 了解功能使用情况，改进产品 |
| | Understand feature usage, improve product |

### 3.2 数据处理方式 Data Processing

- 所有数据处理均在设备本地进行
  All data processing is done locally on the device
- 不将用户数据上传到我们的服务器
  No user data is uploaded to our servers
- 第三方服务仅用于匿名统计和崩溃报告
  Third-party services are only used for anonymous statistics and crash reporting

## 4. 数据存储与保护 Data Storage and Protection

### 4.1 本地存储 Local Storage

- 应用数据存储在设备本地数据库（Room）
  App data is stored in local device database (Room)
- 敏感设置使用 EncryptedSharedPreferences 加密
  Sensitive settings use EncryptedSharedPreferences encryption
- 数据库可选择启用 SQLCipher 加密（v21+）
  Database can optionally enable SQLCipher encryption (v21+)

### 4.2 安全措施 Security Measures

- **加密存储 Encrypted Storage**: AES-256 加密算法
- **应用锁 App Lock**: 支持生物识别和 PIN 码保护
- **安全通信 Secure Communication**: HTTPS 用于所有网络请求
- **代码混淆 Code Obfuscation**: ProGuard/R8 代码混淆保护

## 5. 第三方服务 Third-Party Services

### 5.1 使用的第三方服务 Services We Use

| 服务 Service | 用途 Purpose | 隐私政策 Privacy Policy |
|-------------|-------------|------------------------|
| Firebase Crashlytics | 崩溃报告 Crash reporting | [Google Privacy](https://policies.google.com/privacy) |
| Firebase Analytics | 匿名统计 Anonymous analytics | [Google Privacy](https://policies.google.com/privacy) |
| Shizuku | 系统权限 System permissions | [Shizuku Privacy](https://shizuku.rikka.app/privacy/) |

### 5.2 数据共享 Data Sharing

- 我们不会向第三方出售您的数据
  We do not sell your data to third parties
- 仅在法律要求时向当局披露信息
  Information is only disclosed to authorities when legally required

## 6. 用户权利 User Rights

### 6.1 您的权利 Your Rights

根据 GDPR、CCPA 等隐私法规，您享有以下权利：

Under GDPR, CCPA, and other privacy regulations, you have the following rights:

- **知情权 Right to Know**: 了解我们收集哪些数据
  Know what data we collect
- **访问权 Right to Access**: 获取您的数据副本
  Obtain a copy of your data
- **删除权 Right to Deletion**: 要求删除您的数据
  Request deletion of your data
- **更正权 Right to Correction**: 更正不准确的数据
  Correct inaccurate data
- **可携带权 Right to Portability**: 导出您的数据
  Export your data

### 6.2 行使权利 Exercising Rights

如需行使上述权利，请通过以下方式联系我们：

To exercise these rights, please contact us:

- **电子邮件 Email**: privacy@appclonex.dev
- **GitHub Issues**: [创建隐私相关 Issue](https://github.com/your-repo/AppCloneX/issues)

我们将在 30 天内响应您的请求。
We will respond to your request within 30 days.

## 7. 数据保留 Data Retention

- **应用数据 App Data**: 保留至用户删除应用或清除数据
  Retained until user uninstalls app or clears data
- **崩溃日志 Crash Logs**: 保留 90 天后自动删除
  Retained for 90 days then automatically deleted
- **分析数据 Analytics**: 保留 26 个月（Firebase 默认）
  Retained for 26 months (Firebase default)

## 8. 儿童隐私 Children's Privacy

本应用不面向 13 岁以下儿童。我们不会故意收集儿童的个人信息。

This app is not directed to children under 13. We do not knowingly collect personal information from children.

## 9. 政策更新 Policy Updates

我们可能会更新本隐私政策。重大变更将通过应用内通知告知用户。

We may update this Privacy Policy. Significant changes will be notified to users via in-app notifications.

**变更历史 Change History:**
- 2025-05-16: 初始版本 Initial version

## 10. 联系我们 Contact Us

如有任何隐私相关问题，请通过以下方式联系我们：

For any privacy-related questions, please contact us:

- **电子邮件 Email**: privacy@appclonex.dev
- **GitHub**: [AppCloneX Repository](https://github.com/your-repo/AppCloneX)
- **问题反馈 Issue Tracker**: [GitHub Issues](https://github.com/your-repo/AppCloneX/issues)

---

## 11. 法律合规 Legal Compliance

本隐私政策符合以下法规要求：

This Privacy Policy complies with:

- **GDPR** (欧盟通用数据保护条例 EU General Data Protection Regulation)
- **CCPA** (加州消费者隐私法 California Consumer Privacy Act)
- **PIPL** (中国个人信息保护法 China Personal Information Protection Law)

---

**继续使用 AppCloneX 即表示您同意本隐私政策。**

**By continuing to use AppCloneX, you agree to this Privacy Policy.**
