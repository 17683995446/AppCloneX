# AppCloneX 安全加固 Security Hardening

## V21 安全优化点

### 1. 数据加密
- EncryptedSharedPreferences存储敏感配置
- SQLCipher数据库加密（可选）
- 密钥存储在Android Keystore

### 2. 应用锁
- BiometricPrompt生物识别
- PIN码保护（6位数字）
- 自动锁定超时设置

### 3. 网络安全
- 禁用明文流量 (usesCleartextTraffic="false")
- 证书固定 (Certificate Pinning)
- 网络安全配置 (Network Security Config)

### 4. 代码安全
- ProGuard/R8代码混淆
- 反调试检测
- 根检测与模拟器检测

### 5. 权限安全
- 最小权限原则
- 运行时权限请求
- 权限使用说明

### 6. 数据安全
- 禁用备份 (allowBackup="false")
- 数据提取规则限制
- 安全删除数据

## 安全合规
- GDPR数据保护
- CCPA隐私合规
- PIPL中国个保法
